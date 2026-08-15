package com.example.engine.broker

import com.example.data.local.entity.BrokerOrderEntity
import com.example.data.local.entity.PositionEntity
import com.example.data.model.AccountSnapshot
import com.example.data.model.Candle
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.PositionDirection
import com.example.data.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class PaperBroker(
    initialCapital: Double = 100000.0
) : BrokerInterface {

    private var cash: Double = initialCapital
    private var realizedPnlToday: Double = 0.0

    private val positions = ConcurrentHashMap<String, PositionEntity>()
    private val orders = ConcurrentHashMap<String, BrokerOrderEntity>()
    private val quotes = ConcurrentHashMap<String, Quote>()

    private val _accountFlow = MutableStateFlow(
        AccountSnapshot(
            accountEquity = initialCapital,
            cashBalance = initialCapital,
            buyingPower = initialCapital * 2.0,
            realizedPnlToday = 0.0,
            unrealizedPnl = 0.0
        )
    )

    private val basePrices = mapOf(
        "SPY" to 585.50,
        "VOO" to 535.20,
        "QQQ" to 495.80,
        "IWM" to 222.40,
        "DIA" to 430.10
    )

    init {
        // Initialize quotes
        basePrices.forEach { (sym, price) ->
            quotes[sym] = Quote(
                symbol = sym,
                bid = price - 0.02,
                ask = price + 0.02,
                lastPrice = price,
                volume = 2500000L,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getAccountSnapshot(): AccountSnapshot {
        var unrealizedPnl = 0.0
        positions.values.forEach { pos ->
            val quote = quotes[pos.symbol]
            val currentPrice = quote?.lastPrice ?: pos.currentPrice
            val posPnl = if (pos.direction == PositionDirection.LONG) {
                (currentPrice - pos.avgEntryPrice) * pos.quantity
            } else {
                (pos.avgEntryPrice - currentPrice) * pos.quantity
            }
            unrealizedPnl += posPnl
        }

        val totalEquity = cash + positions.values.sumOf { it.quantity * (quotes[it.symbol]?.lastPrice ?: it.currentPrice) }
        val snapshot = AccountSnapshot(
            accountEquity = totalEquity,
            cashBalance = cash,
            buyingPower = cash * 2.0,
            realizedPnlToday = realizedPnlToday,
            unrealizedPnl = unrealizedPnl,
            openPositionsCount = positions.size
        )
        _accountFlow.value = snapshot
        return snapshot
    }

    override suspend fun getPositions(): List<PositionEntity> {
        return positions.values.toList()
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        // Simulate minor realistic price drift
        symbols.forEach { sym ->
            val base = quotes[sym]?.lastPrice ?: basePrices[sym] ?: 500.0
            val drift = (Random.nextDouble() - 0.495) * (base * 0.0006)
            val newPrice = (base + drift).coerceAtLeast(1.0)
            val spread = 0.02

            val updatedQuote = Quote(
                symbol = sym,
                bid = newPrice - (spread / 2),
                ask = newPrice + (spread / 2),
                lastPrice = newPrice,
                volume = (quotes[sym]?.volume ?: 1000000L) + Random.nextLong(100, 1000),
                timestamp = System.currentTimeMillis()
            )
            quotes[sym] = updatedQuote

            // Update open position current prices
            positions[sym]?.let { p ->
                val pnl = (newPrice - p.avgEntryPrice) * p.quantity
                val pnlPct = ((newPrice - p.avgEntryPrice) / p.avgEntryPrice) * 100
                positions[sym] = p.copy(
                    currentPrice = newPrice,
                    unrealizedPnl = pnl,
                    unrealizedPnlPercent = pnlPct,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
        getAccountSnapshot()
        return quotes.filterKeys { it in symbols }
    }

    override suspend fun getCandles(symbol: String, count: Int): List<Candle> {
        val basePrice = quotes[symbol]?.lastPrice ?: basePrices[symbol] ?: 500.0
        val candles = mutableListOf<Candle>()
        var currentPrice = basePrice * 0.985
        val intervalMillis = 15 * 60 * 1000L
        val now = System.currentTimeMillis()

        for (i in count downTo 1) {
            val t = now - (i * intervalMillis)
            val change = (Random.nextDouble() - 0.49) * (currentPrice * 0.003)
            val open = currentPrice
            val close = open + change
            val high = maxOf(open, close) + Random.nextDouble(0.1, 0.4)
            val low = minOf(open, close) - Random.nextDouble(0.1, 0.4)
            val vol = Random.nextLong(20000, 85000)

            candles.add(
                Candle(
                    symbol = symbol,
                    timestamp = t,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol
                )
            )
            currentPrice = close
        }

        // Align last candle close with current quote
        if (candles.isNotEmpty()) {
            val last = candles.last()
            val latestClose = quotes[symbol]?.lastPrice ?: last.close
            candles[candles.size - 1] = last.copy(
                close = latestClose,
                high = maxOf(last.high, latestClose),
                low = minOf(last.low, latestClose)
            )
        }

        return candles
    }

    override suspend fun submitOrder(order: BrokerOrderEntity): BrokerOrderEntity {
        val currentQuote = quotes[order.symbol]
        val fillPrice = when (order.action) {
            OrderAction.BUY, OrderAction.BUY_TO_COVER -> currentQuote?.ask ?: order.limitPrice ?: 500.0
            OrderAction.SELL, OrderAction.SELL_SHORT -> currentQuote?.bid ?: order.limitPrice ?: 500.0
        }

        val brokerId = "ETRADE-SIM-${UUID.randomUUID().toString().take(8)}"
        val filledOrder = order.copy(
            brokerOrderId = brokerId,
            filledQuantity = order.quantity,
            avgFillPrice = fillPrice,
            status = OrderStatus.FILLED,
            updatedAt = System.currentTimeMillis()
        )
        orders[order.clientOrderId] = filledOrder

        // Update positions and cash
        when (order.action) {
            OrderAction.BUY -> {
                val totalCost = fillPrice * order.quantity
                cash -= totalCost

                val existing = positions[order.symbol]
                if (existing != null) {
                    val totalQty = existing.quantity + order.quantity
                    val avgEntry = ((existing.avgEntryPrice * existing.quantity) + totalCost) / totalQty
                    positions[order.symbol] = existing.copy(
                        quantity = totalQty,
                        avgEntryPrice = avgEntry,
                        currentPrice = fillPrice,
                        stopLoss = order.stopPrice ?: existing.stopLoss,
                        takeProfit = order.limitPrice ?: existing.takeProfit,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    positions[order.symbol] = PositionEntity(
                        symbol = order.symbol,
                        direction = PositionDirection.LONG,
                        quantity = order.quantity,
                        avgEntryPrice = fillPrice,
                        currentPrice = fillPrice,
                        stopLoss = order.stopPrice ?: (fillPrice * 0.99),
                        takeProfit = order.limitPrice ?: (fillPrice * 1.015),
                        entryTimestamp = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            OrderAction.SELL -> {
                val existing = positions[order.symbol]
                if (existing != null) {
                    val proceeds = fillPrice * order.quantity
                    val costBasis = existing.avgEntryPrice * order.quantity
                    val pnl = proceeds - costBasis
                    cash += proceeds
                    realizedPnlToday += pnl

                    val remainingQty = existing.quantity - order.quantity
                    if (remainingQty <= 0) {
                        positions.remove(order.symbol)
                    } else {
                        positions[order.symbol] = existing.copy(
                            quantity = remainingQty,
                            currentPrice = fillPrice,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
            }
            else -> {}
        }

        getAccountSnapshot()
        return filledOrder
    }

    override suspend fun cancelOrder(clientOrderId: String): Boolean {
        val existing = orders[clientOrderId] ?: return false
        orders[clientOrderId] = existing.copy(status = OrderStatus.CANCELLED, updatedAt = System.currentTimeMillis())
        return true
    }

    override suspend fun closePosition(symbol: String): BrokerOrderEntity? {
        val pos = positions[symbol] ?: return null
        val closeOrder = BrokerOrderEntity(
            clientOrderId = UUID.randomUUID().toString(),
            symbol = symbol,
            action = if (pos.direction == PositionDirection.LONG) OrderAction.SELL else OrderAction.BUY_TO_COVER,
            orderType = com.example.data.model.OrderType.MARKET,
            quantity = pos.quantity,
            status = OrderStatus.SUBMITTED
        )
        return submitOrder(closeOrder)
    }

    override fun observeAccountSnapshot(): Flow<AccountSnapshot> = _accountFlow.asStateFlow()
}
