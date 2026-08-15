package com.example.engine.broker

import com.example.data.local.entity.BrokerOrderEntity
import com.example.data.local.entity.PositionEntity
import com.example.data.model.AccountSnapshot
import com.example.data.model.Candle
import com.example.data.model.Quote
import kotlinx.coroutines.flow.Flow

interface BrokerInterface {
    suspend fun getAccountSnapshot(): AccountSnapshot
    suspend fun getPositions(): List<PositionEntity>
    suspend fun getQuotes(symbols: List<String>): Map<String, Quote>
    suspend fun getCandles(symbol: String, count: Int = 100): List<Candle>
    suspend fun submitOrder(order: BrokerOrderEntity): BrokerOrderEntity
    suspend fun cancelOrder(clientOrderId: String): Boolean
    suspend fun closePosition(symbol: String): BrokerOrderEntity?
    fun observeAccountSnapshot(): Flow<AccountSnapshot>
}
