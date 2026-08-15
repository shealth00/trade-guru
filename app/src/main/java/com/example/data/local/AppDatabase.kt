package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CircuitBreakerStatusDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.OrderIntentDao
import com.example.data.local.dao.PositionDao
import com.example.data.local.dao.StrategyConfigurationDao
import com.example.data.local.dao.TradeRecordDao
import com.example.data.local.dao.TradingSignalDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OrderIntent
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradeRecord
import com.example.data.local.entity.TradingSignal
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PositionDirection
import com.example.data.model.SignalType

class Converters {
    @TypeConverter
    fun fromOrderAction(value: OrderAction): String = value.name

    @TypeConverter
    fun toOrderAction(value: String): OrderAction = OrderAction.valueOf(value)

    @TypeConverter
    fun fromOrderType(value: OrderType): String = value.name

    @TypeConverter
    fun toOrderType(value: String): OrderType = OrderType.valueOf(value)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)

    @TypeConverter
    fun fromPositionDirection(value: PositionDirection): String = value.name

    @TypeConverter
    fun toPositionDirection(value: String): PositionDirection = PositionDirection.valueOf(value)

    @TypeConverter
    fun fromSignalType(value: SignalType): String = value.name

    @TypeConverter
    fun toSignalType(value: String): SignalType = SignalType.valueOf(value)
}

@Database(
    entities = [
        StrategyConfiguration::class,
        TradingSignal::class,
        OrderIntent::class,
        TradeRecord::class,
        CircuitBreakerStatus::class,
        PositionEntity::class,
        AuditLogEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun strategyConfigurationDao(): StrategyConfigurationDao
    abstract fun tradingSignalDao(): TradingSignalDao
    abstract fun orderIntentDao(): OrderIntentDao
    abstract fun tradeRecordDao(): TradeRecordDao
    abstract fun circuitBreakerStatusDao(): CircuitBreakerStatusDao
    abstract fun positionDao(): PositionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao

    // Backward-compatible DAO getters
    fun brokerOrderDao(): OrderIntentDao = orderIntentDao()
    fun tradeDao(): TradeRecordDao = tradeRecordDao()
    fun signalDao(): TradingSignalDao = tradingSignalDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "etrade_autotrader.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
