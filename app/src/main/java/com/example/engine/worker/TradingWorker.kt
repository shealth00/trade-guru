package com.example.engine.worker

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradingSignal
import com.example.data.model.AccountSnapshot
import com.example.data.model.BotState
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues
import com.example.data.model.OrderAction
import com.example.data.model.Quote
import com.example.data.model.SignalType
import com.example.data.model.TradingMode
import com.example.engine.broker.BrokerInterface
import com.example.engine.broker.ETradeBroker
import com.example.engine.broker.PaperBroker
import com.example.engine.execution.ExecutionGateway
import com.example.engine.execution.OrderMonitor
import com.example.engine.execution.PositionReconciler
import com.example.engine.risk.RiskEngine
import com.example.engine.strategy.MeanReversionStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class TradingWorker(
    private val database: AppDatabase,
    private val riskEngine: RiskEngine = RiskEngine()
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var loopJob: Job? = null

    private val paperBroker = PaperBroker()
    private val etradeSandboxBroker = ETradeBroker(isLive = false, paperFallback = paperBroker)
    private val etradeLiveBroker = ETradeBroker(isLive = true, paperFallback = paperBroker)

    private var activeBroker: BrokerInterface = paperBroker
    val executionGateway = ExecutionGateway(database, activeBroker)
    private val positionReconciler = PositionReconciler(database, activeBroker)
    private val orderMonitor = OrderMonitor(database, activeBroker)

    val strategy = MeanReversionStrategy()

    private val _tradingMode = MutableStateFlow(TradingMode.PAPER)
    val tradingMode: StateFlow<TradingMode> = _tradingMode.asStateFlow()

    private val _botState = MutableStateFlow(BotState.DISABLED)
    val botState: StateFlow<BotState> = _botState.asStateFlow()

    private val _activeSymbol = MutableStateFlow("SPY")
    val activeSymbol: StateFlow<String> = _activeSymbol.asStateFlow()

    private val _currentQuote = MutableStateFlow<Quote?>(null)
    val currentQuote: StateFlow<Quote?> = _currentQuote.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _indicators = MutableStateFlow<IndicatorValues?>(null)
    val indicators: StateFlow<IndicatorValues?> = _indicators.asStateFlow()

    private val _recentSignals = MutableStateFlow<List<TradingSignal>>(emptyList())
    val recentSignals: StateFlow<List<TradingSignal>> = _recentSignals.asStateFlow()

    private val _accountSnapshot = MutableStateFlow(AccountSnapshot())
    val accountSnapshot: StateFlow<AccountSnapshot> = _accountSnapshot.asStateFlow()

    val circuitBreakers: StateFlow<CircuitBreakerStatus> = riskEngine.circuitBreakers

    val watchlist = listOf("SPY", "VOO", "QQQ", "IWM", "DIA")

    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                try {
                    tickTradingCycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(2000)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    fun setTradingMode(mode: TradingMode) {
        _tradingMode.value = mode
        activeBroker = when (mode) {
            TradingMode.PAPER -> paperBroker
            TradingMode.SANDBOX -> etradeSandboxBroker
            TradingMode.LIVE -> etradeLiveBroker
        }
        executionGateway.setBroker(activeBroker)
        positionReconciler.setBroker(activeBroker)
        orderMonitor.setBroker(activeBroker)
    }

    fun setBotState(state: BotState) {
        _botState.value = state
    }

    fun selectSymbol(symbol: String) {
        _activeSymbol.value = symbol
    }

    suspend fun updateStrategyConfig(newConfig: StrategyConfiguration, operator: String, reason: String) {
        val prevConfig = strategy.config
        strategy.updateConfig(newConfig)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                strategyId = newConfig.strategyId,
                operator = operator,
                parameterChanged = "RSI (${prevConfig.rsiOversoldThreshold}→${newConfig.rsiOversoldThreshold}) / TP (${prevConfig.takeProfitPercent * 100}%→${newConfig.takeProfitPercent * 100}%)",
                previousValue = "RSI_OS=${prevConfig.rsiOversoldThreshold}, TP=${prevConfig.takeProfitPercent}, SL=${prevConfig.stopLossPercent}",
                newValue = "RSI_OS=${newConfig.rsiOversoldThreshold}, TP=${newConfig.takeProfitPercent}, SL=${newConfig.stopLossPercent}",
                reason = reason
            )
        )
    }

    fun resetCircuitBreakers(operator: String, reason: String) {
        riskEngine.resetCircuitBreakers(operator, reason)
        if (_botState.value == BotState.RISK_HALTED) {
            _botState.value = BotState.PAUSED
        }
        scope.launch {
            database.auditLogDao().insertAuditLog(
                AuditLogEntity(
                    strategyId = strategy.config.strategyId,
                    operator = operator,
                    parameterChanged = "CIRCUIT_BREAKER_RESET",
                    previousValue = "HALTED",
                    newValue = "ARMED_NORMAL",
                    reason = reason
                )
            )
        }
    }

    suspend fun triggerManualTestSignal(type: SignalType) {
        val sym = _activeSymbol.value
        val quote = activeBroker.getQuotes(listOf(sym))[sym] ?: return
        val currentInd = _indicators.value

        val testSignal = TradingSignal(
            id = UUID.randomUUID().toString(),
            symbol = sym,
            signalType = type,
            price = quote.lastPrice,
            timestamp = System.currentTimeMillis(),
            rsiValue = if (type == SignalType.BUY) 28.4 else 62.1,
            bbUpper = (currentInd?.bbUpper ?: (quote.lastPrice * 1.01)),
            bbMiddle = (currentInd?.bbMiddle ?: quote.lastPrice),
            bbLower = (currentInd?.bbLower ?: (quote.lastPrice * 0.99)),
            sma200 = currentInd?.sma200,
            suggestedStopLoss = quote.lastPrice * 0.99,
            suggestedTakeProfit = quote.lastPrice * 1.015,
            rationale = "MANUAL CUJ ACCEPTANCE TEST: ${type.name} trigger for $sym at $${String.format("%.2f", quote.lastPrice)}"
        )

        processSignal(testSignal)
    }

    private suspend fun tickTradingCycle() {
        // 1. Fetch live quotes for watchlist
        val quotes = activeBroker.getQuotes(watchlist)
        val activeSym = _activeSymbol.value
        val symQuote = quotes[activeSym]
        _currentQuote.value = symQuote

        // 2. Fetch candles and calculate technical indicators for active symbol
        val symCandles = activeBroker.getCandles(activeSym, 120)
        _candles.value = symCandles
        val calculatedInd = strategy.calculateIndicators(symCandles)
        _indicators.value = calculatedInd

        // 3. Update Account Snapshot & Reconcile Positions
        val account = activeBroker.getAccountSnapshot()
        _accountSnapshot.value = account
        positionReconciler.reconcile()
        orderMonitor.checkOpenOrders()

        // 4. If Bot is RUNNING, evaluate strategy across watchlist
        if (_botState.value == BotState.RUNNING) {
            for (sym in watchlist) {
                val cList = if (sym == activeSym) symCandles else activeBroker.getCandles(sym, 80)
                val pos = database.positionDao().getPositionBySymbol(sym)
                val holdingQty = pos?.quantity ?: 0

                val signal = strategy.evaluate(cList, holdingQty)
                if (signal != null) {
                    processSignal(signal)
                }
            }
        }
    }

    private suspend fun processSignal(signal: TradingSignal) {
        // Record signal in state
        _recentSignals.value = (listOf(signal) + _recentSignals.value).take(20)

        // Persist signal in DB
        database.tradingSignalDao().insertSignal(
            TradingSignal(
                id = signal.id,
                symbol = signal.symbol,
                signalType = signal.signalType,
                price = signal.price,
                timestamp = signal.timestamp,
                rsiValue = signal.rsiValue,
                bbUpper = signal.bbUpper,
                bbMiddle = signal.bbMiddle,
                bbLower = signal.bbLower,
                sma200 = signal.sma200,
                suggestedStopLoss = signal.suggestedStopLoss,
                suggestedTakeProfit = signal.suggestedTakeProfit,
                rationale = signal.rationale,
                rawScore = signal.rawScore,
                isExecuted = false
            )
        )

        // If not running, don't execute
        if (_botState.value != BotState.RUNNING) return

        // 1. Evaluate signal through Risk Engine
        val openPositions = database.positionDao().getOpenPositions()
        val riskEval = riskEngine.evaluateSignal(
            signal = signal,
            account = _accountSnapshot.value,
            config = strategy.config,
            currentOpenPositionsCount = openPositions.size
        )

        if (!riskEval.isApproved) {
            if (riskEngine.circuitBreakers.value.isHalted) {
                _botState.value = BotState.RISK_HALTED
            }
            return
        }

        // 2. Submit order through Execution Gateway
        when (signal.signalType) {
            SignalType.BUY -> {
                executionGateway.submitOrder(
                    symbol = signal.symbol,
                    action = OrderAction.BUY,
                    quantity = riskEval.approvedQuantity,
                    limitPrice = signal.price,
                    stopPrice = riskEval.stopLossPrice
                )
            }
            SignalType.SELL -> {
                val pos = database.positionDao().getPositionBySymbol(signal.symbol)
                if (pos != null) {
                    executionGateway.submitOrder(
                        symbol = signal.symbol,
                        action = OrderAction.SELL,
                        quantity = pos.quantity,
                        limitPrice = signal.price
                    )
                }
            }
            else -> {}
        }
    }
}
