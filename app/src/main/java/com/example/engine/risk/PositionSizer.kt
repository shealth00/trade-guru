package com.example.engine.risk

import com.example.data.local.entity.StrategyConfiguration
import com.example.data.model.AccountSnapshot
import kotlin.math.floor
import kotlin.math.max

class PositionSizer {

    /**
     * Calculates share quantity based on fixed account risk percentage and stop loss distance.
     * Formula: Position Size = (Account Equity * Max Risk Per Trade %) / (Entry Price - Stop Loss Price)
     */
    fun calculateQuantity(
        account: AccountSnapshot,
        config: StrategyConfiguration,
        entryPrice: Double,
        stopLossPrice: Double
    ): Int {
        val riskPerShare = entryPrice - stopLossPrice
        if (riskPerShare <= 0.01 || entryPrice <= 0.0) return 0

        val maxDollarRisk = account.accountEquity * config.maxRiskPerTradePercent
        val rawShares = floor(maxDollarRisk / riskPerShare).toInt()

        // Max allowable position capital (cap at 20% of account equity)
        val maxCapitalPerPosition = account.accountEquity * 0.20
        val maxSharesByCapital = floor(maxCapitalPerPosition / entryPrice).toInt()

        // Buying power constraint
        val maxSharesByBuyingPower = floor((account.buyingPower * 0.85) / entryPrice).toInt()

        val finalShares = minOf(rawShares, maxSharesByCapital, maxSharesByBuyingPower)
        return max(0, finalShares)
    }
}
