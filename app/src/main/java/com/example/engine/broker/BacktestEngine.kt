package com.example.engine.broker

import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradeRecord
import com.example.data.model.BacktestResult
import com.example.data.model.Candle
import com.example.data.model.PerformanceMetrics
import com.example.data.model.SignalType
import com.example.engine.strategy.MeanReversionStrategy
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class BacktestEngine {

    fun runBacktest(
        config: StrategyConfiguration,
        initialBalance: Double = 100000.0,
        candleCount: Int = 500
    ): BacktestResult {
        val strategy = MeanReversionStrategy(config)
        val candles = generateHistoricalCandles("SPY", candleCount)

        var balance = initialBalance
        var peakBalance = initialBalance
        val equityCurve = mutableListOf<Double>()
        equityCurve.add(balance)

        val trades = mutableListOf<TradeRecord>()

        var currentPositionQty = 0
        var entryPrice = 0.0
        var entryTime = 0L
        var stopLoss = 0.0
        var takeProfit = 0.0

        val windowCandles = mutableListOf<Candle>()

        for (i in candles.indices) {
            val candle = candles[i]
            windowCandles.add(candle)
            if (windowCandles.size > 250) {
                windowCandles.removeAt(0)
            }

            if (windowCandles.size < 30) continue

            // 1. Check open position exit triggers (Stop Loss or Take Profit or Strategy exit)
            if (currentPositionQty > 0) {
                var exitPrice: Double? = null
                var exitReason: String? = null

                if (candle.low <= stopLoss) {
                    exitPrice = stopLoss * 0.9995 // slippage
                    exitReason = "Stop Loss Hit (-${String.format("%.1f", config.stopLossPercent * 100)}%)"
                } else if (candle.high >= takeProfit) {
                    exitPrice = takeProfit * 0.9995
                    exitReason = "Take Profit Target Hit (+${String.format("%.1f", config.takeProfitPercent * 100)}%)"
                } else {
                    val signal = strategy.evaluate(windowCandles, currentPositionQty)
                    if (signal != null && signal.signalType == SignalType.SELL) {
                        exitPrice = candle.close * 0.9998
                        exitReason = "Mean Reversion Target (BB Mid / RSI)"
                    }
                }

                if (exitPrice != null) {
                    val grossPnl = (exitPrice - entryPrice) * currentPositionQty
                    val commission = 1.00 // $1 flat execution
                    val netPnl = grossPnl - commission
                    balance += (exitPrice * currentPositionQty) - commission
                    val pnlPct = ((exitPrice - entryPrice) / entryPrice) * 100

                    trades.add(
                        TradeRecord(
                            id = UUID.randomUUID().toString(),
                            symbol = candle.symbol,
                            side = "LONG",
                            quantity = currentPositionQty,
                            entryPrice = entryPrice,
                            exitPrice = exitPrice,
                            entryTime = entryTime,
                            exitTime = candle.timestamp,
                            pnl = netPnl,
                            pnlPercent = pnlPct,
                            commission = commission,
                            exitReason = exitReason
                        )
                    )

                    currentPositionQty = 0
                    if (balance > peakBalance) peakBalance = balance
                }
            }

            // 2. Check entry signals
            if (currentPositionQty == 0) {
                val signal = strategy.evaluate(windowCandles, 0)
                if (signal != null && signal.signalType == SignalType.BUY) {
                    entryPrice = candle.close * 1.0002 // buy slippage
                    stopLoss = signal.suggestedStopLoss ?: (entryPrice * (1.0 - config.stopLossPercent))
                    takeProfit = signal.suggestedTakeProfit ?: (entryPrice * (1.0 + config.takeProfitPercent))

                    val riskPerShare = entryPrice - stopLoss
                    val maxDollarRisk = balance * config.maxRiskPerTradePercent
                    val shares = (maxDollarRisk / riskPerShare).toInt().coerceIn(10, 200)

                    val cost = shares * entryPrice
                    if (balance >= cost) {
                        currentPositionQty = shares
                        entryTime = candle.timestamp
                        balance -= cost
                    }
                }
            }

            val currentEquity = balance + (currentPositionQty * candle.close)
            equityCurve.add(currentEquity)
        }

        // Close any remaining position at end of backtest
        if (currentPositionQty > 0) {
            val lastCandle = candles.last()
            val exitPrice = lastCandle.close
            val grossPnl = (exitPrice - entryPrice) * currentPositionQty
            balance += (exitPrice * currentPositionQty)
            trades.add(
                TradeRecord(
                    id = UUID.randomUUID().toString(),
                    symbol = lastCandle.symbol,
                    side = "LONG",
                    quantity = currentPositionQty,
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    entryTime = entryTime,
                    exitTime = lastCandle.timestamp,
                    pnl = grossPnl,
                    pnlPercent = ((exitPrice - entryPrice) / entryPrice) * 100,
                    commission = 1.00,
                    exitReason = "Backtest Window Close"
                )
            )
        }

        val metrics = calculateMetrics(trades, initialBalance, equityCurve)
        val finalBalance = equityCurve.lastOrNull() ?: balance

        return BacktestResult(
            initialBalance = initialBalance,
            finalBalance = finalBalance,
            totalReturnPercent = ((finalBalance - initialBalance) / initialBalance) * 100,
            metrics = metrics,
            equityCurve = equityCurve,
            trades = trades
        )
    }

    private fun calculateMetrics(
        trades: List<TradeRecord>,
        initialBalance: Double,
        equityCurve: List<Double>
    ): PerformanceMetrics {
        if (trades.isEmpty()) return PerformanceMetrics()

        val winningTrades = trades.filter { (it.pnl ?: 0.0) > 0 }
        val losingTrades = trades.filter { (it.pnl ?: 0.0) <= 0 }

        val grossProfit = winningTrades.sumOf { it.pnl ?: 0.0 }
        val grossLoss = losingTrades.sumOf { kotlin.math.abs(it.pnl ?: 0.0) }
        val netPnl = grossProfit - grossLoss

        val winRate = winningTrades.size.toDouble() / trades.size.toDouble()
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0
        val expectancy = if (trades.isNotEmpty()) netPnl / trades.size else 0.0

        // Calculate Max Drawdown from equity curve
        var peak = initialBalance
        var maxDd = 0.0
        equityCurve.forEach { eq ->
            if (eq > peak) peak = eq
            val dd = (peak - eq) / peak
            if (dd > maxDd) maxDd = dd
        }

        // Returns for Sharpe / Sortino
        val tradeReturns = trades.map { (it.pnl ?: 0.0) / initialBalance }
        val avgReturn = if (tradeReturns.isNotEmpty()) tradeReturns.average() else 0.0
        val variance = if (tradeReturns.size > 1) {
            tradeReturns.map { (it - avgReturn) * (it - avgReturn) }.sum() / (tradeReturns.size - 1)
        } else 0.0001
        val stdDev = sqrt(variance)
        val sharpe = if (stdDev > 0) (avgReturn / stdDev) * sqrt(252.0) else 0.0

        val downsideVariance = tradeReturns.filter { it < 0 }.let { down ->
            if (down.isNotEmpty()) down.map { it * it }.sum() / down.size else 0.0001
        }
        val downsideStdDev = sqrt(downsideVariance)
        val sortino = if (downsideStdDev > 0) (avgReturn / downsideStdDev) * sqrt(252.0) else 0.0

        return PerformanceMetrics(
            totalTrades = trades.size,
            winningTrades = winningTrades.size,
            losingTrades = losingTrades.size,
            winRate = winRate,
            profitFactor = profitFactor,
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            netPnl = netPnl,
            sharpeRatio = max(0.0, sharpe),
            sortinoRatio = max(0.0, sortino),
            maxDrawdownPercent = maxDd,
            expectancy = expectancy
        )
    }

    private fun generateHistoricalCandles(symbol: String, count: Int): List<Candle> {
        val candles = mutableListOf<Candle>()
        var price = 520.0
        val startTime = System.currentTimeMillis() - (count * 15 * 60 * 1000L)

        for (i in 0 until count) {
            val t = startTime + (i * 15 * 60 * 1000L)
            val cycle = kotlin.math.sin(i * 0.15) * 4.0
            val noise = (Random.nextDouble() - 0.49) * 2.5
            val open = price
            val close = (open + (cycle * 0.1) + noise).coerceAtLeast(10.0)
            val high = max(open, close) + Random.nextDouble(0.2, 1.2)
            val low = min(open, close) - Random.nextDouble(0.2, 1.2)
            val vol = Random.nextLong(25000, 110000)

            candles.add(
                Candle(
                    symbol = symbol,
                    timestamp = t,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol
                )
            )
            price = close
        }
        return candles
    }
}
