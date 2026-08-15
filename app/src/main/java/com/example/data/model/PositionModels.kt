package com.example.data.model

enum class PositionDirection {
    LONG,
    SHORT
}

data class PositionRecord(
    val positionId: String,
    val symbol: String,
    val direction: PositionDirection,
    val quantity: Int,
    val avgEntryPrice: Double,
    val currentPrice: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val strategyId: String,
    val entryOrderId: String,
    val openedAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val tradingMode: TradingMode = TradingMode.PAPER
) {
    val marketValue: Double
        get() = quantity * currentPrice

    val totalCost: Double
        get() = quantity * avgEntryPrice

    val unrealizedPnl: Double
        get() = when (direction) {
            PositionDirection.LONG -> (currentPrice - avgEntryPrice) * quantity
            PositionDirection.SHORT -> (avgEntryPrice - currentPrice) * quantity
        }

    val unrealizedPnlPercent: Double
        get() = if (totalCost > 0) (unrealizedPnl / totalCost) * 100.0 else 0.0
}
