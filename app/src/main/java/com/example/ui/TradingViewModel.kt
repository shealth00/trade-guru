package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.OrderIntent
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradeRecord
import com.example.data.local.entity.TradingSignal
import com.example.data.model.AccountSnapshot
import com.example.data.model.BacktestResult
import com.example.data.model.BotState
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues
import com.example.data.model.PerformanceMetrics
import com.example.data.model.Quote
import com.example.data.model.SignalType
import com.example.data.model.TradingMode
import com.example.engine.analytics.PerformanceCalculator
import com.example.engine.broker.BacktestEngine
import com.example.engine.worker.TradingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val worker = TradingWorker(database)
    private val performanceCalculator = PerformanceCalculator()
    private val backtestEngine = BacktestEngine()

    val botState: StateFlow<BotState> = worker.botState
    val tradingMode: StateFlow<TradingMode> = worker.tradingMode
    val activeSymbol: StateFlow<String> = worker.activeSymbol
    val currentQuote: StateFlow<Quote?> = worker.currentQuote
    val candles: StateFlow<List<Candle>> = worker.candles
    val indicators: StateFlow<IndicatorValues?> = worker.indicators
    val recentSignals: StateFlow<List<TradingSignal>> = worker.recentSignals
    val accountSnapshot: StateFlow<AccountSnapshot> = worker.accountSnapshot
    val circuitBreakers: StateFlow<CircuitBreakerStatus> = worker.circuitBreakers

    val openPositions: StateFlow<List<PositionEntity>> = database.positionDao()
        .getOpenPositionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<OrderIntent>> = database.orderIntentDao()
        .getAllOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrades: StateFlow<List<TradeRecord>> = database.tradeRecordDao()
        .getAllTradesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = database.auditLogDao()
        .getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strategyConfig: StateFlow<StrategyConfiguration> = MutableStateFlow(worker.strategy.config).asStateFlow()

    private val _backtestResult = MutableStateFlow<BacktestResult?>(null)
    val backtestResult: StateFlow<BacktestResult?> = _backtestResult.asStateFlow()

    private val _isRunningBacktest = MutableStateFlow(false)
    val isRunningBacktest: StateFlow<Boolean> = _isRunningBacktest.asStateFlow()

    val performanceMetrics: StateFlow<PerformanceMetrics> = allTrades.combine(accountSnapshot) { trades, _ ->
        performanceCalculator.calculate(trades, accountStartingBalance = 100000.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceMetrics())

    init {
        worker.start()
        // Run initial default backtest
        runBacktest()
    }

    fun startBot() {
        worker.setBotState(BotState.RUNNING)
    }

    fun pauseBot() {
        worker.setBotState(BotState.PAUSED)
    }

    fun resumeBot() {
        worker.setBotState(BotState.RUNNING)
    }

    fun emergencyStop() {
        worker.setBotState(BotState.DISABLED)
        viewModelScope.launch(Dispatchers.IO) {
            val openOrders = database.orderIntentDao().getOpenOrders()
            openOrders.forEach { ord ->
                worker.executionGateway.cancelOrder(ord.clientOrderId)
            }
        }
    }

    fun setTradingMode(mode: TradingMode) {
        worker.setTradingMode(mode)
    }

    fun selectSymbol(symbol: String) {
        worker.selectSymbol(symbol)
    }

    fun updateStrategyConfig(newConfig: StrategyConfiguration, operator: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            worker.updateStrategyConfig(newConfig, operator, reason)
        }
    }

    fun resetCircuitBreakers(operator: String, reason: String) {
        worker.resetCircuitBreakers(operator, reason)
    }

    fun closePosition(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            worker.executionGateway.closePosition(symbol)
        }
    }

    fun closeAllPositions() {
        viewModelScope.launch(Dispatchers.IO) {
            val positions = database.positionDao().getOpenPositions()
            positions.forEach { pos ->
                worker.executionGateway.closePosition(pos.symbol)
            }
        }
    }

    fun cancelOrder(clientOrderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            worker.executionGateway.cancelOrder(clientOrderId)
        }
    }

    fun triggerManualTestSignal(type: SignalType) {
        viewModelScope.launch(Dispatchers.IO) {
            worker.triggerManualTestSignal(type)
        }
    }

    fun triggerAcceptanceTest() {
        viewModelScope.launch(Dispatchers.IO) {
            // End-to-end CUJ test:
            // 1. Arm bot
            worker.setBotState(BotState.RUNNING)
            // 2. Inject buy signal
            worker.triggerManualTestSignal(SignalType.BUY)
        }
    }

    fun runBacktest() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRunningBacktest.value = true
            val result = backtestEngine.runBacktest(worker.strategy.config)
            _backtestResult.value = result
            _isRunningBacktest.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        worker.stop()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TradingViewModel(application) as T
                }
            }
        }
    }
}
