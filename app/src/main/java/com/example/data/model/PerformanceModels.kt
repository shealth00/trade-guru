package com.example.data.model

import com.example.data.local.entity.TradeRecord

data class PerformanceMetrics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val winRate: Double = 0.0,
    val profitFactor: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossLoss: Double = 0.0,
    val netPnl: Double = 0.0,
    val sharpeRatio: Double = 0.0,
    val sortinoRatio: Double = 0.0,
    val maxDrawdownPercent: Double = 0.0,
    val expectancy: Double = 0.0
)

data class BacktestResult(
    val initialBalance: Double,
    val finalBalance: Double,
    val totalReturnPercent: Double,
    val metrics: PerformanceMetrics,
    val equityCurve: List<Double>,
    val trades: List<TradeRecord>
)

data class AccountSnapshot(
    val accountEquity: Double = 100000.0,
    val cashBalance: Double = 100000.0,
    val buyingPower: Double = 200000.0,
    val realizedPnlToday: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val openPositionsCount: Int = 0,
    val dayTradesCount: Int = 0
)

data class RiskEvaluationResult(
    val isApproved: Boolean,
    val rejectionReason: String? = null,
    val approvedQuantity: Int = 0,
    val stopLossPrice: Double = 0.0,
    val takeProfitPrice: Double = 0.0
)
