package com.example.engine.execution

import com.example.data.local.AppDatabase
import com.example.data.local.entity.PositionEntity
import com.example.engine.broker.BrokerInterface

class PositionReconciler(
    private val database: AppDatabase,
    private var broker: BrokerInterface
) {
    fun setBroker(broker: BrokerInterface) {
        this.broker = broker
    }

    suspend fun reconcile(): Boolean {
        return try {
            val brokerPositions = broker.getPositions()
            val localPositions = database.positionDao().getOpenPositions()

            val brokerMap = brokerPositions.associateBy { it.symbol }
            val localMap = localPositions.associateBy { it.symbol }

            // Sync broker positions to DB
            brokerPositions.forEach { bp ->
                val local = localMap[bp.symbol]
                if (local == null || local.quantity != bp.quantity || local.avgEntryPrice != bp.avgEntryPrice) {
                    database.positionDao().insertOrUpdatePosition(bp)
                }
            }

            // Remove any positions that were closed at broker
            localPositions.forEach { lp ->
                if (!brokerMap.containsKey(lp.symbol)) {
                    database.positionDao().deletePosition(lp.symbol)
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
