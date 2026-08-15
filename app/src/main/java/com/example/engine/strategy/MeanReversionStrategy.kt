package com.example.engine.strategy

import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradingSignal
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues
import com.example.data.model.SignalType
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

class MeanReversionStrategy(
    initialConfig: StrategyConfiguration = StrategyConfiguration()
) : Strategy {
    override var config: StrategyConfiguration = initialConfig
        private set

    override fun updateConfig(newConfig: StrategyConfiguration) {
        this.config = newConfig
    }

    override fun calculateIndicators(candles: List<Candle>): IndicatorValues? {
        if (candles.size < kotlin.math.max(config.bollingerPeriod, config.rsiPeriod + 1)) {
            return null
        }

        val closes = candles.map { it.close }
        val rsi = calculateRsi(closes, config.rsiPeriod)
        val (bbUpper, bbMiddle, bbLower) = calculateBollingerBands(closes, config.bollingerPeriod, config.bollingerStdDev)
        val sma200 = if (candles.size >= 200) calculateSma(closes, 200) else null

        // Volume ratio vs 20 period SMA
        val volumes = candles.map { it.volume.toDouble() }
        val volSma20 = if (volumes.size >= 20) calculateSma(volumes, 20) else volumes.average()
        val currentVol = volumes.lastOrNull() ?: 1.0
        val volumeRatio = if (volSma20 > 0) currentVol / volSma20 else 1.0

        return IndicatorValues(
            rsi = rsi,
            bbUpper = bbUpper,
            bbMiddle = bbMiddle,
            bbLower = bbLower,
            sma200 = sma200,
            volumeRatio = volumeRatio,
            timestamp = candles.last().timestamp
        )
    }

    override fun evaluate(candles: List<Candle>, currentHoldingQuantity: Int): TradingSignal? {
        val indicators = calculateIndicators(candles) ?: return null
        val latestCandle = candles.last()
        val currentPrice = latestCandle.close

        // 1. Long Entry Evaluation
        // Conditions: Price touches/breaks below Lower Bollinger Band, RSI < oversold threshold, and Trend filter satisfied
        val isLowerBbCrossed = currentPrice <= indicators.bbLower * 1.002 // slight tolerance
        val isRsiOversold = indicators.rsi <= config.rsiOversoldThreshold
        val isTrendFavorable = if (config.requireTrendFilter && indicators.sma200 != null) {
            currentPrice >= indicators.sma200 * 0.98 // within or above long-term trend
        } else true
        val isVolumeAdequate = indicators.volumeRatio >= (config.minVolumeRatio * 0.8)

        if (currentHoldingQuantity == 0 && isLowerBbCrossed && isRsiOversold && isTrendFavorable && isVolumeAdequate) {
            val stopLoss = currentPrice * (1.0 - config.stopLossPercent)
            val takeProfit = currentPrice * (1.0 + config.takeProfitPercent)
            return TradingSignal(
                id = UUID.randomUUID().toString(),
                symbol = latestCandle.symbol,
                signalType = SignalType.BUY,
                price = currentPrice,
                timestamp = latestCandle.timestamp,
                rsiValue = indicators.rsi,
                bbUpper = indicators.bbUpper,
                bbMiddle = indicators.bbMiddle,
                bbLower = indicators.bbLower,
                sma200 = indicators.sma200,
                suggestedStopLoss = stopLoss,
                suggestedTakeProfit = takeProfit,
                rationale = "Mean Reversion LONG: Price ($${String.format("%.2f", currentPrice)}) tested Lower BB ($${String.format("%.2f", indicators.bbLower)}) with RSI oversold (${String.format("%.1f", indicators.rsi)})."
            )
        }

        // 2. Long Exit Evaluation (Take profit / Mean reversion to middle band or RSI recovery)
        if (currentHoldingQuantity > 0) {
            val isRsiRecovered = indicators.rsi >= config.rsiExitLongThreshold
            val isMiddleBbReached = currentPrice >= indicators.bbMiddle

            if (isRsiRecovered || isMiddleBbReached) {
                return TradingSignal(
                    id = UUID.randomUUID().toString(),
                    symbol = latestCandle.symbol,
                    signalType = SignalType.SELL,
                    price = currentPrice,
                    timestamp = latestCandle.timestamp,
                    rsiValue = indicators.rsi,
                    bbUpper = indicators.bbUpper,
                    bbMiddle = indicators.bbMiddle,
                    bbLower = indicators.bbLower,
                    sma200 = indicators.sma200,
                    suggestedStopLoss = null,
                    suggestedTakeProfit = null,
                    rationale = "Mean Reversion EXIT: Price ($${String.format("%.2f", currentPrice)}) reached Mean SMA ($${String.format("%.2f", indicators.bbMiddle)}) / RSI recovered (${String.format("%.1f", indicators.rsi)})."
                )
            }
        }

        return null
    }

    private fun calculateRsi(prices: List<Double>, period: Int): Double {
        if (prices.size <= period) return 50.0

        var gainSum = 0.0
        var lossSum = 0.0

        // Initial average
        for (i in 1..period) {
            val diff = prices[i] - prices[i - 1]
            if (diff >= 0) gainSum += diff else lossSum += -diff
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        // Smoothed RSI for subsequent bars
        for (i in (period + 1) until prices.size) {
            val diff = prices[i] - prices[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun calculateBollingerBands(prices: List<Double>, period: Int, stdDevMultiplier: Double): Triple<Double, Double, Double> {
        val window = prices.takeLast(period)
        val mean = window.average()
        val variance = window.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        val upper = mean + (stdDev * stdDevMultiplier)
        val lower = mean - (stdDev * stdDevMultiplier)
        return Triple(upper, mean, lower)
    }

    private fun calculateSma(values: List<Double>, period: Int): Double {
        return values.takeLast(period).average()
    }
}
