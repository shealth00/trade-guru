package com.example.engine.indicators

import com.example.data.model.IndicatorResult
import com.example.data.model.MarketCandle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object IndicatorEngine {

    /**
     * Calculates all required technical indicators up to the given index (no future data lookahead).
     */
    fun calculateIndicators(
        candles: List<MarketCandle>,
        rsiPeriod: Int = 14,
        bollingerPeriod: Int = 20,
        bollingerStdDev: Double = 2.0,
        smaLongPeriod: Int = 200,
        atrPeriod: Int = 14,
        volumePeriod: Int = 20
    ): IndicatorResult {
        if (candles.isEmpty()) return IndicatorResult()

        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }
        val volumes = candles.map { it.volume.toDouble() }

        val rsiSeries = calculateRsiSeries(closes, rsiPeriod)
        val currentRsi = rsiSeries.lastOrNull()

        val sma20Series = calculateSmaSeries(closes, bollingerPeriod)
        val currentSma20 = sma20Series.lastOrNull()

        val sma200Series = calculateSmaSeries(closes, smaLongPeriod)
        // If not enough candles for 200, fallback to longest available SMA or null
        val currentSma200 = if (closes.size >= smaLongPeriod) {
            sma200Series.lastOrNull()
        } else if (closes.size >= 50) {
            calculateSmaSeries(closes, 50).lastOrNull()
        } else {
            calculateSmaSeries(closes, min(closes.size, 20)).lastOrNull()
        }

        val bbSeries = calculateBollingerBandsSeries(closes, bollingerPeriod, bollingerStdDev)
        val currentBb = bbSeries.lastOrNull()

        val atrSeries = calculateAtrSeries(highs, lows, closes, atrPeriod)
        val currentAtr = atrSeries.lastOrNull()

        val volSmaSeries = calculateSmaSeries(volumes, volumePeriod)
        val currentVolSma = volSmaSeries.lastOrNull()
        val currentVol = volumes.lastOrNull() ?: 0.0
        val currentVolRatio = if (currentVolSma != null && currentVolSma > 0) {
            currentVol / currentVolSma
        } else {
            1.0
        }

        return IndicatorResult(
            rsi = currentRsi,
            sma20 = currentSma20,
            sma200 = currentSma200,
            upperBollinger = currentBb?.upper,
            middleBollinger = currentBb?.middle,
            lowerBollinger = currentBb?.lower,
            atr = currentAtr,
            volumeAverage = currentVolSma,
            volumeRatio = currentVolRatio
        )
    }

    /**
     * Calculates RSI series using Wilder's smoothed moving average.
     */
    fun calculateRsiSeries(closes: List<Double>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(closes.size) { null }
        if (closes.size <= period) return result

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change >= 0) {
                gainSum += change
            } else {
                lossSum += abs(change)
            }
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        result[period] = calculateRsiValue(avgGain, avgLoss)

        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val currentGain = if (change >= 0) change else 0.0
            val currentLoss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period

            result[i] = calculateRsiValue(avgGain, avgLoss)
        }

        return result
    }

    private fun calculateRsiValue(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return 100.0
        if (avgGain == 0.0) return 0.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /**
     * Simple Moving Average series.
     */
    fun calculateSmaSeries(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period || period <= 0) return result

        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        result[period - 1] = sum / period

        for (i in period until values.size) {
            sum += values[i] - values[i - period]
            result[i] = sum / period
        }

        return result
    }

    /**
     * Exponential Moving Average series.
     */
    fun calculateEmaSeries(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period || period <= 0) return result

        val multiplier = 2.0 / (period + 1.0)
        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        var prevEma = sum / period
        result[period - 1] = prevEma

        for (i in period until values.size) {
            val currentEma = (values[i] - prevEma) * multiplier + prevEma
            result[i] = currentEma
            prevEma = currentEma
        }

        return result
    }

    data class BollingerBandPoint(
        val upper: Double,
        val middle: Double,
        val lower: Double
    )

    /**
     * Bollinger Bands calculation.
     */
    fun calculateBollingerBandsSeries(
        closes: List<Double>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): List<BollingerBandPoint?> {
        val result = MutableList<BollingerBandPoint?>(closes.size) { null }
        if (closes.size < period) return result

        val smas = calculateSmaSeries(closes, period)

        for (i in (period - 1) until closes.size) {
            val sma = smas[i] ?: continue
            var varianceSum = 0.0
            for (j in (i - period + 1)..i) {
                val diff = closes[j] - sma
                varianceSum += diff * diff
            }
            val stdDev = sqrt(varianceSum / period)
            val upper = sma + (stdDevMultiplier * stdDev)
            val lower = sma - (stdDevMultiplier * stdDev)
            result[i] = BollingerBandPoint(upper = upper, middle = sma, lower = lower)
        }

        return result
    }

    /**
     * Average True Range (ATR) calculation.
     */
    fun calculateAtrSeries(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): List<Double?> {
        val size = closes.size
        val result = MutableList<Double?>(size) { null }
        if (size <= period) return result

        val trList = mutableListOf<Double>()
        trList.add(highs[0] - lows[0])

        for (i in 1 until size) {
            val tr1 = highs[i] - lows[i]
            val tr2 = abs(highs[i] - closes[i - 1])
            val tr3 = abs(lows[i] - closes[i - 1])
            trList.add(max(tr1, max(tr2, tr3)))
        }

        var atrSum = 0.0
        for (i in 0 until period) {
            atrSum += trList[i]
        }
        var currentAtr = atrSum / period
        result[period - 1] = currentAtr

        for (i in period until size) {
            currentAtr = (currentAtr * (period - 1) + trList[i]) / period
            result[i] = currentAtr
        }

        return result
    }
}
