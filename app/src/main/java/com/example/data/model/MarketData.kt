package com.example.data.model

data class Candle(
    val symbol: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class Quote(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val lastPrice: Double,
    val volume: Long,
    val timestamp: Long
)

data class IndicatorValues(
    val rsi: Double,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val sma200: Double?,
    val volumeRatio: Double,
    val timestamp: Long
)

enum class Timeframe(val label: String, val minutes: Int) {
    M5("5m", 5),
    M15("15m", 15),
    M30("30m", 30),
    H1("1h", 60),
    D1("1d", 1440);

    companion object {
        fun fromLabel(label: String): Timeframe =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: M15
    }
}

data class MarketCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val symbol: String,
    val timeframe: String,
    val bid: Double = close - 0.02,
    val ask: Double = close + 0.02,
    val isClosed: Boolean = true
) {
    val spreadPercent: Double
        get() = if (bid > 0) ((ask - bid) / bid) * 100.0 else 0.0
}

data class IndicatorResult(
    val rsi: Double? = null,
    val sma20: Double? = null,
    val sma200: Double? = null,
    val upperBollinger: Double? = null,
    val middleBollinger: Double? = null,
    val lowerBollinger: Double? = null,
    val atr: Double? = null,
    val volumeAverage: Double? = null,
    val volumeRatio: Double? = null
)
