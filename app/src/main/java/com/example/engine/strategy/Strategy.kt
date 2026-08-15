package com.example.engine.strategy

import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradingSignal
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues

interface Strategy {
    val config: StrategyConfiguration
    fun calculateIndicators(candles: List<Candle>): IndicatorValues?
    fun evaluate(candles: List<Candle>, currentHoldingQuantity: Int = 0): TradingSignal?
    fun updateConfig(newConfig: StrategyConfiguration)
}
