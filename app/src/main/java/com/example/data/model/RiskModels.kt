package com.example.data.model

enum class BotState(val label: String) {
    DISABLED("DISABLED"),
    ARMED("ARMED"),
    RUNNING("RUNNING"),
    PAUSED("PAUSED"),
    RISK_HALTED("RISK HALTED"),
    ERROR("ERROR")
}

enum class TradingMode(val label: String) {
    PAPER("PAPER (Simulated)"),
    SANDBOX("E*TRADE SANDBOX"),
    LIVE("E*TRADE LIVE PRODUCTION")
}

data class RiskLimits(
    val maxRiskPerTradePercent: Double = 0.005, // 0.5% default
    val maxPositionAllocationPercent: Double = 0.05, // 5% default
    val maxTotalExposurePercent: Double = 0.50, // 50% default
    val maxOpenPositions: Int = 10,
    val maxDailyLossDollars: Double = 2000.0,
    val maxDailyLossPercent: Double = 0.02, // 2%
    val maxConsecutiveLosses: Int = 3,
    val maxDrawdownPercent: Double = 0.10, // 10%
    val enableMarketRegimeFilter: Boolean = true,
    val enableLiquidityFilter: Boolean = true,
    val minAverageVolume: Long = 500_000,
    val maxSpreadPercent: Double = 0.05, // 0.05%
    val minSharePrice: Double = 5.0,
    val allowPyramiding: Boolean = false,
    val enableShorting: Boolean = false,
    val allowExtendedHours: Boolean = false
)

data class RiskCheckResult(
    val isApproved: Boolean,
    val rejectionReason: String? = null,
    val calculatedQuantity: Int = 0,
    val calculatedStopLoss: Double = 0.0,
    val calculatedTakeProfit: Double = 0.0,
    val riskBudget: Double = 0.0,
    val riskPerShare: Double = 0.0,
    val allocationQuantity: Int = 0,
    val buyingPowerQuantity: Int = 0,
    val riskQuantity: Int = 0,
    val currentExposure: Double = 0.0,
    val newExposure: Double = 0.0,
    val checksSummary: List<String> = emptyList()
)

data class StartSequenceStep(
    val stepNumber: Int,
    val name: String,
    val description: String,
    val isVerified: Boolean = false,
    val error: String? = null
)
