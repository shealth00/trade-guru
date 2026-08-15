package com.example.data.model

enum class ExitReason(val label: String) {
    RSI_EXIT("RSI Threshold Reached"),
    TAKE_PROFIT("Take Profit Target Hit"),
    STOP_LOSS("Stop Loss Threshold Triggered"),
    RISK_LIQUIDATION("Risk Engine Liquidation"),
    MANUAL_EXIT("Manual Operator Exit"),
    CIRCUIT_BREAKER("Circuit Breaker Liquidation")
}

data class CompletedTrade(
    val tradeId: String,
    val strategyId: String,
    val symbol: String,
    val direction: PositionDirection,
    val entryOrderId: String,
    val entryTime: Long,
    val entryPrice: Double,
    val entryQuantity: Int,
    val exitOrderId: String,
    val exitTime: Long,
    val exitPrice: Double,
    val grossPnl: Double,
    val fees: Double,
    val slippage: Double,
    val netPnl: Double,
    val holdingPeriodSeconds: Long,
    val exitReason: ExitReason,
    val tradingMode: TradingMode = TradingMode.PAPER,
    val explainabilityDetails: String = ""
) {
    val isWin: Boolean
        get() = netPnl > 0.0

    val returnPercent: Double
        get() = if (entryPrice > 0) ((exitPrice - entryPrice) / entryPrice) * (if (direction == PositionDirection.LONG) 100.0 else -100.0) else 0.0
}
