package com.example.data.model

data class MeanReversionParams(
    val rsiPeriod: Int = 14,
    val rsiEntry: Double = 30.0,
    val rsiExit: Double = 50.0,
    val rsiShortEntry: Double = 70.0,
    val bollingerPeriod: Int = 20,
    val bollingerStdDev: Double = 2.0,
    val takeProfitPercent: Double = 0.02, // 2%
    val stopLossPercent: Double = 0.015,  // 1.5%
    val minVolumeRatio: Double = 1.0,
    val filterRequireAboveSma200: Boolean = true,
    val filterMaxSpreadPercent: Double = 0.05
)

data class AuditLog(
    val logId: String,
    val timestamp: Long,
    val strategyId: String,
    val operator: String,
    val parameterChanged: String,
    val previousValue: String,
    val newValue: String,
    val reason: String
)

data class NotificationItem(
    val id: String,
    val timestamp: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    ORDER_SUBMITTED,
    ORDER_FILLED,
    ORDER_REJECTED,
    STOP_LOSS_HIT,
    TAKE_PROFIT_HIT,
    CIRCUIT_BREAKER_TRIGGERED,
    AUTH_ERROR,
    BOT_STATE_CHANGED,
    RISK_WARNING
}
