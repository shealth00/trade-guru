package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OrderIntent
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradeRecord
import com.example.data.local.entity.TradingSignal
import kotlinx.coroutines.flow.Flow

class TradingRepository(private val database: AppDatabase) {

    // Strategy Configuration
    val activeStrategyFlow: Flow<StrategyConfiguration?> =
        database.strategyConfigurationDao().getActiveConfigFlow()

    val allStrategiesFlow: Flow<List<StrategyConfiguration>> =
        database.strategyConfigurationDao().getAllConfigsFlow()

    suspend fun saveStrategyConfig(config: StrategyConfiguration) {
        database.strategyConfigurationDao().insertOrUpdateConfig(config)
    }

    suspend fun getStrategyConfig(id: String): StrategyConfiguration? {
        return database.strategyConfigurationDao().getConfigById(id)
    }

    // Trading Signals
    val recentSignalsFlow: Flow<List<TradingSignal>> =
        database.tradingSignalDao().getRecentSignalsFlow()

    suspend fun saveSignal(signal: TradingSignal) {
        database.tradingSignalDao().insertSignal(signal)
    }

    suspend fun markSignalExecuted(signalId: String) {
        database.tradingSignalDao().markSignalExecuted(signalId)
    }

    // Order Intents
    val allOrdersFlow: Flow<List<OrderIntent>> =
        database.orderIntentDao().getAllOrdersFlow()

    suspend fun saveOrder(order: OrderIntent) {
        database.orderIntentDao().insertOrder(order)
    }

    suspend fun updateOrder(order: OrderIntent) {
        database.orderIntentDao().updateOrder(order)
    }

    suspend fun getOpenOrders(): List<OrderIntent> {
        return database.orderIntentDao().getOpenOrders()
    }

    // Trade Records
    val allTradesFlow: Flow<List<TradeRecord>> =
        database.tradeRecordDao().getAllTradesFlow()

    suspend fun saveTrade(trade: TradeRecord) {
        database.tradeRecordDao().insertTrade(trade)
    }

    suspend fun saveTrades(trades: List<TradeRecord>) {
        database.tradeRecordDao().insertTrades(trades)
    }

    // Circuit Breaker Status
    val circuitBreakerFlow: Flow<CircuitBreakerStatus?> =
        database.circuitBreakerStatusDao().getCircuitBreakerFlow()

    suspend fun saveCircuitBreakerStatus(status: CircuitBreakerStatus) {
        database.circuitBreakerStatusDao().insertOrUpdateStatus(status)
    }

    suspend fun getCircuitBreakerStatus(): CircuitBreakerStatus? {
        return database.circuitBreakerStatusDao().getCircuitBreakerStatus()
    }

    // Positions
    val openPositionsFlow: Flow<List<PositionEntity>> =
        database.positionDao().getOpenPositionsFlow()

    suspend fun savePosition(position: PositionEntity) {
        database.positionDao().insertOrUpdatePosition(position)
    }

    suspend fun deletePosition(symbol: String) {
        database.positionDao().deletePosition(symbol)
    }

    // Audit Logs
    val auditLogsFlow: Flow<List<AuditLogEntity>> =
        database.auditLogDao().getAllAuditLogsFlow()

    suspend fun saveAuditLog(log: AuditLogEntity) {
        database.auditLogDao().insertAuditLog(log)
    }

    // Notifications
    val notificationsFlow: Flow<List<NotificationEntity>> =
        database.notificationDao().getAllNotificationsFlow()

    suspend fun saveNotification(notification: NotificationEntity) {
        database.notificationDao().insertNotification(notification)
    }

    suspend fun markNotificationsRead() {
        database.notificationDao().markAllAsRead()
    }
}
