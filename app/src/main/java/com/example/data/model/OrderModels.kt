package com.example.data.model

enum class OrderType {
    MARKET,
    LIMIT,
    STOP,
    STOP_LIMIT
}

enum class OrderAction {
    BUY,
    SELL,
    BUY_TO_COVER,
    SELL_SHORT
}

enum class TimeInForce {
    DAY,
    GTC,
    EXT
}

enum class OrderStatus {
    CREATED,
    PENDING_PREVIEW,
    PREVIEWED,
    SUBMITTED,
    PENDING_OPEN,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED
}

data class OrderPreviewResult(
    val previewId: String,
    val clientOrderId: String,
    val symbol: String,
    val action: OrderAction,
    val quantity: Int,
    val estimatedPrice: Double,
    val estimatedTotal: Double,
    val estimatedCommission: Double,
    val estimatedMarginRequirement: Double,
    val isApproved: Boolean,
    val warningMessages: List<String> = emptyList(),
    val errorMessage: String? = null
)
