package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PositionDirection
import com.example.data.model.SignalType

/**
 * Room Entity: StrategyConfiguration
 * Persists user and algorithmic trading strategy parameters locally.
 */
@Entity(tableName = "strategy_configurations")
data class StrategyConfiguration(
    @PrimaryKey
    val strategyId: String = "MEAN_REVERSION_V1",
    val strategyName: String = "SPY Mean Reversion RSI + BB",
    val rsiPeriod: Int = 14,
    val rsiOversoldThreshold: Double = 30.0,
    val rsiOverboughtThreshold: Double = 70.0,
    val rsiExitLongThreshold: Double = 50.0,
    val bollingerPeriod: Int = 20,
    val bollingerStdDev: Double = 2.0,
    val takeProfitPercent: Double = 0.015, // 1.5%
    val stopLossPercent: Double = 0.010,   // 1.0%
    val maxRiskPerTradePercent: Double = 0.02, // 2% account equity
    val requireTrendFilter: Boolean = true, // SPY > 200 SMA
    val minVolumeRatio: Double = 1.0,
    val maxAllowedSpreadPercent: Double = 0.0005, // 0.05%
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Room Entity: TradingSignal
 * Persists technical analysis signals produced by strategies.
 */
@Entity(tableName = "trading_signals")
data class TradingSignal(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val signalType: SignalType,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val rsiValue: Double,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val sma200: Double? = null,
    val suggestedStopLoss: Double? = null,
    val suggestedTakeProfit: Double? = null,
    val rationale: String,
    val rawScore: Double = 1.0,
    val isExecuted: Boolean = false
)

/**
 * Room Entity: OrderIntent
 * Persists planned, submitted, and tracked broker order execution intents.
 */
@Entity(tableName = "order_intents")
data class OrderIntent(
    @PrimaryKey
    val clientOrderId: String,
    val brokerOrderId: String? = null,
    val symbol: String,
    val action: OrderAction,
    val orderType: OrderType,
    val quantity: Int,
    val filledQuantity: Int = 0,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val avgFillPrice: Double? = null,
    val status: OrderStatus = OrderStatus.CREATED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rejectReason: String? = null
)

/**
 * Room Entity: TradeRecord
 * Persists completed round-trip trades with PnL and execution metrics.
 */
@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val side: String, // "LONG" or "SHORT"
    val quantity: Int,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val entryTime: Long = System.currentTimeMillis(),
    val exitTime: Long? = null,
    val pnl: Double? = null,
    val pnlPercent: Double? = null,
    val commission: Double = 0.0,
    val exitReason: String? = null
)

/**
 * Room Entity: CircuitBreakerStatus
 * Persists risk management sentinel state, drawdown limits, and halt triggers.
 */
@Entity(tableName = "circuit_breaker_statuses")
data class CircuitBreakerStatus(
    @PrimaryKey
    val id: String = "PRIMARY_SENTINEL",
    val isHalted: Boolean = false,
    val haltReason: String? = null,
    val consecutiveLosses: Int = 0,
    val maxConsecutiveLosses: Int = 3,
    val dailyLossRealized: Double = 0.0,
    val maxDailyLossAllowed: Double = 6000.0,
    val peakDrawdownPercent: Double = 0.0,
    val maxDrawdownAllowedPercent: Double = 0.03, // 3% max drawdown
    val lastResetTimestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity: PositionEntity
 * Persists real-time on-device active portfolio holdings.
 */
@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey
    val symbol: String,
    val direction: PositionDirection,
    val quantity: Int,
    val avgEntryPrice: Double,
    val currentPrice: Double,
    val unrealizedPnl: Double = 0.0,
    val unrealizedPnlPercent: Double = 0.0,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val entryTimestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity: AuditLogEntity
 * Persists immutable operator changes, strategy adjustments, and breaker resets.
 */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val strategyId: String,
    val operator: String,
    val parameterChanged: String,
    val previousValue: String,
    val newValue: String,
    val reason: String
)

/**
 * Room Entity: NotificationEntity
 * Persists trading and risk system alerts.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val severity: String, // "INFO", "WARNING", "ALERT"
    val isRead: Boolean = false
)

// Aliases for seamless type compatibility
typealias BrokerOrderEntity = OrderIntent
typealias TradeEntity = TradeRecord
typealias SignalEntity = TradingSignal
typealias StrategyConfig = StrategyConfiguration
