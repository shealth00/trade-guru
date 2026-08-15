package com.example.engine.execution

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BrokerOrderEntity
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.TradeEntity
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PositionDirection
import com.example.engine.broker.BrokerInterface
import java.util.UUID

class ExecutionGateway(
    private val database: AppDatabase,
    private var broker: BrokerInterface
) {
    fun setBroker(broker: BrokerInterface) {
        this.broker = broker
    }

    suspend fun submitOrder(
        symbol: String,
        action: OrderAction,
        quantity: Int,
        limitPrice: Double? = null,
        stopPrice: Double? = null,
        orderType: OrderType = if (limitPrice != null) OrderType.LIMIT else OrderType.MARKET
    ): BrokerOrderEntity {
        val clientOrderId = UUID.randomUUID().toString()
        val initialOrder = BrokerOrderEntity(
            clientOrderId = clientOrderId,
            symbol = symbol,
            action = action,
            orderType = orderType,
            quantity = quantity,
            limitPrice = limitPrice,
            stopPrice = stopPrice,
            status = OrderStatus.CREATED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Persist initial order record in Room DB
        database.brokerOrderDao().insertOrder(initialOrder)

        // 2. Submit to broker
        val brokerResult = try {
            broker.submitOrder(initialOrder.copy(status = OrderStatus.SUBMITTED))
        } catch (e: Exception) {
            val rejected = initialOrder.copy(
                status = OrderStatus.REJECTED,
                rejectReason = e.message ?: "Broker rejection"
            )
            database.brokerOrderDao().updateOrder(rejected)
            return rejected
        }

        // 3. Update DB with broker response
        database.brokerOrderDao().updateOrder(brokerResult)

        // 4. If filled, update Position & Trade records in Room DB
        if (brokerResult.status == OrderStatus.FILLED) {
            handleOrderFill(brokerResult)
        }

        return brokerResult
    }

    suspend fun cancelOrder(clientOrderId: String): Boolean {
        val success = broker.cancelOrder(clientOrderId)
        if (success) {
            val order = database.brokerOrderDao().getOrderById(clientOrderId)
            order?.let {
                database.brokerOrderDao().updateOrder(it.copy(status = OrderStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
            }
        }
        return success
    }

    suspend fun closePosition(symbol: String, reason: String = "Manual Close"): BrokerOrderEntity? {
        val pos = database.positionDao().getPositionBySymbol(symbol) ?: return null
        val action = if (pos.direction == PositionDirection.LONG) OrderAction.SELL else OrderAction.BUY_TO_COVER
        val order = submitOrder(
            symbol = symbol,
            action = action,
            quantity = pos.quantity,
            orderType = OrderType.MARKET
        )
        return order
    }

    private suspend fun handleOrderFill(order: BrokerOrderEntity) {
        val fillPrice = order.avgFillPrice ?: order.limitPrice ?: 0.0

        when (order.action) {
            OrderAction.BUY -> {
                val existing = database.positionDao().getPositionBySymbol(order.symbol)
                if (existing != null) {
                    val totalQty = existing.quantity + order.quantity
                    val avgPrice = ((existing.avgEntryPrice * existing.quantity) + (fillPrice * order.quantity)) / totalQty
                    database.positionDao().insertOrUpdatePosition(
                        existing.copy(
                            quantity = totalQty,
                            avgEntryPrice = avgPrice,
                            currentPrice = fillPrice,
                            stopLoss = order.stopPrice ?: existing.stopLoss,
                            takeProfit = order.limitPrice ?: existing.takeProfit,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    database.positionDao().insertOrUpdatePosition(
                        PositionEntity(
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
                    )
                }
            }
            OrderAction.SELL -> {
                val existing = database.positionDao().getPositionBySymbol(order.symbol)
                if (existing != null) {
                    val grossPnl = (fillPrice - existing.avgEntryPrice) * order.quantity
                    val pnlPct = ((fillPrice - existing.avgEntryPrice) / existing.avgEntryPrice) * 100
                    val commission = 1.00 // flat commission

                    // Record completed Trade
                    database.tradeDao().insertTrade(
                        TradeEntity(
                            id = UUID.randomUUID().toString(),
                            symbol = order.symbol,
                            side = "LONG",
                            quantity = order.quantity,
                            entryPrice = existing.avgEntryPrice,
                            exitPrice = fillPrice,
                            entryTime = existing.entryTimestamp,
                            exitTime = System.currentTimeMillis(),
                            pnl = grossPnl - commission,
                            pnlPercent = pnlPct,
                            commission = commission,
                            exitReason = if (grossPnl >= 0) "Take Profit Target Hit" else "Stop Loss Hit"
                        )
                    )

                    val remainingQty = existing.quantity - order.quantity
                    if (remainingQty <= 0) {
                        database.positionDao().deletePosition(order.symbol)
                    } else {
                        database.positionDao().insertOrUpdatePosition(
                            existing.copy(
                                quantity = remainingQty,
                                currentPrice = fillPrice,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            else -> {}
        }
    }
}
