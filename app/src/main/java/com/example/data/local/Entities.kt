package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ExitReason
import com.example.data.model.NotificationType
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PositionDirection
import com.example.data.model.SignalAction
import com.example.data.model.SignalStatus
import com.example.data.model.TimeInForce
import com.example.data.model.TradingMode

@Entity(tableName = "strategy_configs")
data class StrategyConfigEntity(
    @PrimaryKey val strategyId: String,
    val name: String,
    val enabled: Boolean,
    val timeframe: String,
    val symbolsCsv: String,
    val rsiPeriod: Int,
    val rsiEntry: Double,
    val rsiExit: Double,
    val rsiShortEntry: Double,
    val bollingerPeriod: Int,
    val bollingerStdDev: Double,
    val takeProfitPercent: Double,
    val stopLossPercent: Double,
    val minVolumeRatio: Double,
    val filterRequireAboveSma200: Boolean,
    val filterMaxSpreadPercent: Double,
    val lastUpdated: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val logId: String,
    val timestamp: Long,
    val strategyId: String,
    val operator: String,
    val parameterChanged: String,
    val previousValue: String,
    val newValue: String,
    val reason: String
)

@Entity(tableName = "trading_signals")
data class TradingSignalEntity(
    @PrimaryKey val signalId: String,
    val signalKey: String,
    val symbol: String,
    val action: String,
    val timestamp: Long,
    val strategyId: String,
    val timeframe: String,
    val price: Double,
    val confidence: Double,
    val indicatorsJson: String,
    val reason: String,
    val status: String,
    val rejectionReason: String?,
    val calculatedStopLoss: Double?,
    val calculatedTakeProfit: Double?,
    val targetQuantity: Int,
    val orderId: String?
)

@Entity(tableName = "broker_orders")
data class BrokerOrderEntity(
    @PrimaryKey val orderId: String,
    val brokerOrderId: String?,
    val clientOrderId: String,
    val symbol: String,
    val action: String,
    val quantity: Int,
    val filledQuantity: Int,
    val avgFillPrice: Double,
    val orderType: String,
    val limitPrice: Double?,
    val stopPrice: Double?,
    val status: String,
    val strategyId: String,
    val signalId: String,
    val tradingMode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rejectionReason: String?,
    val commissionPaid: Double
)

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey val positionId: String,
    val symbol: String,
    val direction: String,
    val quantity: Int,
    val avgEntryPrice: Double,
    val currentPrice: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val strategyId: String,
    val entryOrderId: String,
    val openedAt: Long,
    val updatedAt: Long,
    val tradingMode: String
)

@Entity(tableName = "completed_trades")
data class CompletedTradeEntity(
    @PrimaryKey val tradeId: String,
    val strategyId: String,
    val symbol: String,
    val direction: String,
    val entryOrderId: String,
    val entryTime: Long,
    val entryPrice: Double,
    val entryQuantity: Int,
    val exitOrderId: String,
    val exitTime: Long,
    val exitPrice: Double,
    val grossPnl: Double,
    val fees: Double,
    val slippage: Double,
    val netPnl: Double,
    val holdingPeriodSeconds: Long,
    val exitReason: String,
    val tradingMode: String,
    val explainabilityDetails: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean
)

@Entity(tableName = "market_candles", primaryKeys = ["symbol", "timeframe", "timestamp"])
data class MarketCandleEntity(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val bid: Double,
    val ask: Double,
    val isClosed: Boolean
)
