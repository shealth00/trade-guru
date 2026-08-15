package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OrderIntent
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradeRecord
import com.example.data.local.entity.TradingSignal
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyConfigurationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: StrategyConfiguration)

    @Query("SELECT * FROM strategy_configurations WHERE strategyId = :id")
    suspend fun getConfigById(id: String): StrategyConfiguration?

    @Query("SELECT * FROM strategy_configurations WHERE isActive = 1 LIMIT 1")
    fun getActiveConfigFlow(): Flow<StrategyConfiguration?>

    @Query("SELECT * FROM strategy_configurations ORDER BY lastUpdated DESC")
    fun getAllConfigsFlow(): Flow<List<StrategyConfiguration>>

    @Query("DELETE FROM strategy_configurations WHERE strategyId = :id")
    suspend fun deleteConfig(id: String)
}

@Dao
interface TradingSignalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: TradingSignal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<TradingSignal>)

    @Query("SELECT * FROM trading_signals ORDER BY timestamp DESC LIMIT 50")
    fun getRecentSignalsFlow(): Flow<List<TradingSignal>>

    @Query("SELECT * FROM trading_signals WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 20")
    fun getSignalsForSymbolFlow(symbol: String): Flow<List<TradingSignal>>

    @Query("UPDATE trading_signals SET isExecuted = 1 WHERE id = :signalId")
    suspend fun markSignalExecuted(signalId: String)

    @Query("DELETE FROM trading_signals")
    suspend fun clearSignals()
}

@Dao
interface OrderIntentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderIntent)

    @Update
    suspend fun updateOrder(order: OrderIntent)

    @Query("SELECT * FROM order_intents ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderIntent>>

    @Query("SELECT * FROM order_intents WHERE clientOrderId = :clientOrderId")
    suspend fun getOrderById(clientOrderId: String): OrderIntent?

    @Query("SELECT * FROM order_intents WHERE status IN ('SUBMITTED', 'PENDING_OPEN', 'PARTIALLY_FILLED')")
    suspend fun getOpenOrders(): List<OrderIntent>

    @Query("DELETE FROM order_intents")
    suspend fun clearOrders()
}

@Dao
interface TradeRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeRecord>)

    @Query("SELECT * FROM trade_records ORDER BY exitTime DESC")
    fun getAllTradesFlow(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records ORDER BY exitTime DESC")
    suspend fun getAllTrades(): List<TradeRecord>

    @Query("SELECT * FROM trade_records WHERE symbol = :symbol ORDER BY exitTime DESC")
    fun getTradesBySymbolFlow(symbol: String): Flow<List<TradeRecord>>

    @Query("DELETE FROM trade_records")
    suspend fun clearTrades()
}

@Dao
interface CircuitBreakerStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatus(status: CircuitBreakerStatus)

    @Query("SELECT * FROM circuit_breaker_statuses WHERE id = :id LIMIT 1")
    fun getCircuitBreakerFlow(id: String = "PRIMARY_SENTINEL"): Flow<CircuitBreakerStatus?>

    @Query("SELECT * FROM circuit_breaker_statuses WHERE id = :id LIMIT 1")
    suspend fun getCircuitBreakerStatus(id: String = "PRIMARY_SENTINEL"): CircuitBreakerStatus?

    @Query("DELETE FROM circuit_breaker_statuses")
    suspend fun clearStatus()
}

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePosition(position: PositionEntity)

    @Query("SELECT * FROM positions WHERE quantity > 0")
    fun getOpenPositionsFlow(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE quantity > 0")
    suspend fun getOpenPositions(): List<PositionEntity>

    @Query("SELECT * FROM positions WHERE symbol = :symbol")
    suspend fun getPositionBySymbol(symbol: String): PositionEntity?

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)

    @Query("DELETE FROM positions")
    suspend fun clearAllPositions()
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()
}

// Aliases for DAO compatibility
typealias BrokerOrderDao = OrderIntentDao
typealias TradeDao = TradeRecordDao
typealias SignalDao = TradingSignalDao
