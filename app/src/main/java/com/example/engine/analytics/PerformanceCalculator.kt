package com.example.engine.analytics

import com.example.data.local.entity.TradeEntity
import com.example.data.model.PerformanceMetrics
import kotlin.math.max
import kotlin.math.sqrt

class PerformanceCalculator {

    fun calculate(trades: List<TradeEntity>, accountStartingBalance: Double = 100000.0): PerformanceMetrics {
        if (trades.isEmpty()) {
            return PerformanceMetrics()
        }

        val winningTrades = trades.filter { (it.pnl ?: 0.0) > 0 }
        val losingTrades = trades.filter { (it.pnl ?: 0.0) <= 0 }

        val grossProfit = winningTrades.sumOf { it.pnl ?: 0.0 }
        val grossLoss = losingTrades.sumOf { kotlin.math.abs(it.pnl ?: 0.0) }
        val netPnl = grossProfit - grossLoss

        val winRate = winningTrades.size.toDouble() / trades.size.toDouble()
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0
        val expectancy = if (trades.isNotEmpty()) netPnl / trades.size else 0.0

        // Calculate max drawdown
        var runningEquity = accountStartingBalance
        var peak = runningEquity
        var maxDrawdown = 0.0

        trades.reversed().forEach { t ->
            runningEquity += (t.pnl ?: 0.0)
            if (runningEquity > peak) peak = runningEquity
            val dd = (peak - runningEquity) / peak
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        // Returns for Sharpe / Sortino
        val tradeReturns = trades.map { (it.pnl ?: 0.0) / accountStartingBalance }
        val avgReturn = tradeReturns.average()
        val variance = if (tradeReturns.size > 1) {
            tradeReturns.map { (it - avgReturn) * (it - avgReturn) }.sum() / (tradeReturns.size - 1)
        } else 0.0001
        val stdDev = sqrt(variance)
        val sharpe = if (stdDev > 0) (avgReturn / stdDev) * sqrt(252.0) else 0.0

        val downReturns = tradeReturns.filter { it < 0 }
        val downVariance = if (downReturns.isNotEmpty()) {
            downReturns.map { it * it }.sum() / downReturns.size
        } else 0.0001
        val downStdDev = sqrt(downVariance)
        val sortino = if (downStdDev > 0) (avgReturn / downStdDev) * sqrt(252.0) else 0.0

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
            maxDrawdownPercent = maxDrawdown,
            expectancy = expectancy
        )
    }
}
