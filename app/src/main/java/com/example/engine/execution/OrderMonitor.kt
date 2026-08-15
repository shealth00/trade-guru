package com.example.engine.execution

import com.example.data.local.AppDatabase
import com.example.data.model.OrderStatus
import com.example.engine.broker.BrokerInterface

class OrderMonitor(
    private val database: AppDatabase,
    private var broker: BrokerInterface
) {
    fun setBroker(broker: BrokerInterface) {
        this.broker = broker
    }

    suspend fun checkOpenOrders() {
        val openOrders = database.brokerOrderDao().getOpenOrders()
        val now = System.currentTimeMillis()

        openOrders.forEach { order ->
            // Auto cancel orders that have been open for over 60 minutes without fill
            if (now - order.createdAt > 60 * 60 * 1000L) {
                broker.cancelOrder(order.clientOrderId)
                database.brokerOrderDao().updateOrder(
                    order.copy(
                        status = OrderStatus.EXPIRED,
                        updatedAt = now,
                        rejectReason = "Order timed out after 60 minutes"
                    )
                )
            }
        }
    }
}
