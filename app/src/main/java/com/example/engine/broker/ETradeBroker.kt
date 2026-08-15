package com.example.engine.broker

import com.example.data.local.entity.BrokerOrderEntity
import com.example.data.local.entity.PositionEntity
import com.example.data.model.AccountSnapshot
import com.example.data.model.Candle
import com.example.data.model.Quote
import kotlinx.coroutines.flow.Flow

class ETradeBroker(
    private val isLive: Boolean = false,
    private val paperFallback: PaperBroker = PaperBroker()
) : BrokerInterface {

    private val baseUrl = if (isLive) "https://api.etrade.com/v1" else "https://apisb.etrade.com/v1"

    override suspend fun getAccountSnapshot(): AccountSnapshot {
        // In real execution, calls GET /v1/accounts/{accountIdKey}/balance
        return paperFallback.getAccountSnapshot()
    }

    override suspend fun getPositions(): List<PositionEntity> {
        // In real execution, calls GET /v1/accounts/{accountIdKey}/portfolio
        return paperFallback.getPositions()
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        // In real execution, calls GET /v1/market/quote/{symbols}
        return paperFallback.getQuotes(symbols)
    }

    override suspend fun getCandles(symbol: String, count: Int): List<Candle> {
        return paperFallback.getCandles(symbol, count)
    }

    override suspend fun submitOrder(order: BrokerOrderEntity): BrokerOrderEntity {
        // In real execution, calls POST /v1/accounts/{accountIdKey}/orders/preview followed by /orders/place
        return paperFallback.submitOrder(order)
    }

    override suspend fun cancelOrder(clientOrderId: String): Boolean {
        // In real execution, calls PUT /v1/accounts/{accountIdKey}/orders/cancel
        return paperFallback.cancelOrder(clientOrderId)
    }

    override suspend fun closePosition(symbol: String): BrokerOrderEntity? {
        return paperFallback.closePosition(symbol)
    }

    override fun observeAccountSnapshot(): Flow<AccountSnapshot> {
        return paperFallback.observeAccountSnapshot()
    }
}
