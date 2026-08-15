package com.example.engine.risk

import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.StrategyConfiguration
import com.example.data.local.entity.TradingSignal
import com.example.data.model.AccountSnapshot
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.SignalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RiskEngine(
    private val positionSizer: PositionSizer = PositionSizer(),
    private val marketCalendarService: MarketCalendarService = MarketCalendarService()
) {
    private val _circuitBreakers = MutableStateFlow(CircuitBreakerStatus())
    val circuitBreakers: StateFlow<CircuitBreakerStatus> = _circuitBreakers.asStateFlow()

    private var peakAccountEquity: Double = 100000.0

    fun evaluateSignal(
        signal: TradingSignal,
        account: AccountSnapshot,
        config: StrategyConfiguration,
        currentOpenPositionsCount: Int
    ): RiskEvaluationResult {
        val currentStatus = _circuitBreakers.value

        // 1. Circuit Breaker Check
        if (currentStatus.isHalted) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Trading halted by Risk Sentinel: ${currentStatus.haltReason}"
            )
        }

        // 2. Max Open Positions Limit (Cap at 4 concurrent positions)
        if (signal.signalType == SignalType.BUY && currentOpenPositionsCount >= 4) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Maximum portfolio concurrent positions reached (4/4)"
            )
        }

        // 3. Daily Loss Limit Check
        if (currentStatus.dailyLossRealized >= currentStatus.maxDailyLossAllowed) {
            triggerCircuitBreaker("Daily loss limit exceeded ($${String.format("%.2f", currentStatus.dailyLossRealized)})")
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Daily loss limit breached"
            )
        }

        // 4. Consecutive Loss Check
        if (currentStatus.consecutiveLosses >= currentStatus.maxConsecutiveLosses) {
            triggerCircuitBreaker("Maximum consecutive loss limit reached (${currentStatus.consecutiveLosses} consecutive losses)")
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Consecutive loss circuit breaker active"
            )
        }

        // 5. Account Peak Drawdown Check
        if (account.accountEquity > peakAccountEquity) {
            peakAccountEquity = account.accountEquity
        }
        val currentDrawdown = (peakAccountEquity - account.accountEquity) / peakAccountEquity
        if (currentDrawdown >= currentStatus.maxDrawdownAllowedPercent) {
            triggerCircuitBreaker("Account drawdown limit breached (${String.format("%.2f", currentDrawdown * 100)}% >= 3.0%)")
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Maximum drawdown threshold breached"
            )
        }

        // 6. Sizing & Target Calculations
        val stopLossPrice = signal.suggestedStopLoss ?: (signal.price * (1.0 - config.stopLossPercent))
        val takeProfitPrice = signal.suggestedTakeProfit ?: (signal.price * (1.0 + config.takeProfitPercent))

        val quantity = if (signal.signalType == SignalType.BUY) {
            positionSizer.calculateQuantity(account, config, signal.price, stopLossPrice)
        } else {
            1 // Default for exit signals
        }

        if (signal.signalType == SignalType.BUY && quantity <= 0) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "Calculated position size is 0 shares (insufficient buying power or invalid stop loss)"
            )
        }

        return RiskEvaluationResult(
            isApproved = true,
            approvedQuantity = quantity,
            stopLossPrice = stopLossPrice,
            takeProfitPrice = takeProfitPrice
        )
    }

    fun recordTradeResult(realizedPnl: Double, accountEquity: Double) {
        val current = _circuitBreakers.value
        val isLoss = realizedPnl < 0

        val newConsecutiveLosses = if (isLoss) current.consecutiveLosses + 1 else 0
        val newDailyLoss = if (isLoss) current.dailyLossRealized + kotlin.math.abs(realizedPnl) else current.dailyLossRealized

        if (accountEquity > peakAccountEquity) {
            peakAccountEquity = accountEquity
        }
        val currentDrawdown = (peakAccountEquity - accountEquity) / peakAccountEquity

        var isHalted = current.isHalted
        var haltReason = current.haltReason

        if (newConsecutiveLosses >= current.maxConsecutiveLosses) {
            isHalted = true
            haltReason = "Halted: $newConsecutiveLosses consecutive trade losses"
        } else if (newDailyLoss >= current.maxDailyLossAllowed) {
            isHalted = true
            haltReason = "Halted: Daily loss limit exceeded ($${String.format("%.2f", newDailyLoss)})"
        } else if (currentDrawdown >= current.maxDrawdownAllowedPercent) {
            isHalted = true
            haltReason = "Halted: Peak drawdown exceeded (${String.format("%.2f", currentDrawdown * 100)}%)"
        }

        _circuitBreakers.value = current.copy(
            isHalted = isHalted,
            haltReason = haltReason,
            consecutiveLosses = newConsecutiveLosses,
            dailyLossRealized = newDailyLoss,
            peakDrawdownPercent = currentDrawdown
        )
    }

    fun resetCircuitBreakers(operator: String, reason: String) {
        _circuitBreakers.value = CircuitBreakerStatus(
            isHalted = false,
            haltReason = null,
            consecutiveLosses = 0,
            dailyLossRealized = 0.0,
            peakDrawdownPercent = 0.0,
            lastResetTimestamp = System.currentTimeMillis()
        )
    }

    private fun triggerCircuitBreaker(reason: String) {
        _circuitBreakers.value = _circuitBreakers.value.copy(
            isHalted = true,
            haltReason = reason
        )
    }
}
