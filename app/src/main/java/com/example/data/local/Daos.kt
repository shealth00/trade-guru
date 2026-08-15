package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyDao {
    @Query("SELECT * FROM strategy_configs")
    fun getAllStrategies(): Flow<List<StrategyConfigEntity>>

    @Query("SELECT * FROM strategy_configs WHERE strategyId = :id")
    suspend fun getStrategyById(id: String): StrategyConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: StrategyConfigEntity)

    @Query("UPDATE strategy_configs SET enabled = :enabled WHERE strategyId = :id")
    suspend fun setStrategyEnabled(id: String, enabled: Boolean)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)
}

@Dao
interface SignalDao {
    @Query("SELECT * FROM trading_signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<TradingSignalEntity>>

    @Query("SELECT * FROM trading_signals WHERE signalKey = :key LIMIT 1")
    suspend fun getSignalByKey(key: String): TradingSignalEntity?

    @Query("SELECT * FROM trading_signals WHERE signalId = :id LIMIT 1")
    suspend fun getSignalById(id: String): TradingSignalEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(signal: TradingSignalEntity): Long

    @Update
    suspend fun update(signal: TradingSignalEntity)

    @Query("SELECT COUNT(*) FROM trading_signals WHERE timestamp >= :sinceTimestamp")
    suspend fun getSignalCountSince(sinceTimestamp: Long): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM broker_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<BrokerOrderEntity>>

    @Query("SELECT * FROM broker_orders WHERE status IN ('SUBMITTED', 'OPEN', 'PARTIALLY_FILLED')")
    fun getOpenOrders(): Flow<List<BrokerOrderEntity>>

    @Query("SELECT * FROM broker_orders WHERE status IN ('SUBMITTED', 'OPEN', 'PARTIALLY_FILLED')")
    suspend fun getOpenOrdersList(): List<BrokerOrderEntity>

    @Query("SELECT * FROM broker_orders WHERE symbol = :symbol AND status IN ('SUBMITTED', 'OPEN', 'PARTIALLY_FILLED')")
    suspend fun getOpenOrdersForSymbol(symbol: String): List<BrokerOrderEntity>

    @Query("SELECT * FROM broker_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): BrokerOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: BrokerOrderEntity)

    @Update
    suspend fun update(order: BrokerOrderEntity)

    @Query("UPDATE broker_orders SET status = 'CANCELLED' WHERE status IN ('SUBMITTED', 'OPEN')")
    suspend fun cancelAllOpenOrders()
}

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions")
    fun getAllPositions(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions")
    suspend fun getAllPositionsList(): List<PositionEntity>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    suspend fun getPositionBySymbol(symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PositionEntity)

    @Update
    suspend fun update(position: PositionEntity)

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("DELETE FROM positions WHERE positionId = :positionId")
    suspend fun deleteById(positionId: String)

    @Query("DELETE FROM positions")
    suspend fun deleteAllPositions()
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM completed_trades ORDER BY exitTime DESC")
    fun getAllTrades(): Flow<List<CompletedTradeEntity>>

    @Query("SELECT * FROM completed_trades ORDER BY exitTime DESC")
    suspend fun getAllTradesList(): List<CompletedTradeEntity>

    @Query("SELECT * FROM completed_trades WHERE tradeId = :id LIMIT 1")
    suspend fun getTradeById(id: String): CompletedTradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: CompletedTradeEntity)

    @Query("SELECT COUNT(*) FROM completed_trades WHERE exitTime >= :sinceTimestamp")
    suspend fun getTradesCountSince(sinceTimestamp: Long): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 100")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface CandleDao {
    @Query("SELECT * FROM market_candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    suspend fun getCandles(symbol: String, timeframe: String): List<MarketCandleEntity>

    @Query("SELECT * FROM market_candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    fun getCandlesFlow(symbol: String, timeframe: String): Flow<List<MarketCandleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<MarketCandleEntity>)

    @Query("DELETE FROM market_candles WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun clearCandles(symbol: String, timeframe: String)
}
