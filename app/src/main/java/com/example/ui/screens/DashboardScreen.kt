package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.TradingSignal
import com.example.data.model.AccountSnapshot
import com.example.data.model.BotState
import com.example.data.model.PerformanceMetrics
import com.example.data.model.PositionDirection
import com.example.data.model.SignalType
import com.example.ui.components.SignalExplainabilityModal
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedContainer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    botState: BotState,
    accountSnapshot: AccountSnapshot,
    metrics: PerformanceMetrics,
    circuitBreakers: CircuitBreakerStatus,
    openPositions: List<PositionEntity>,
    recentSignals: List<TradingSignal>,
    onStartBotRequested: () -> Unit,
    onPauseBotRequested: () -> Unit,
    onResumeBotRequested: () -> Unit,
    onClosePosition: (String) -> Unit,
    onCloseAllPositions: () -> Unit,
    onTriggerAcceptanceTest: () -> Unit,
    onNavigateToChart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSignalForExplain by remember { mutableStateOf<TradingSignal?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bot Operational Bar & CUJ Test Trigger
        item {
            OperationalActionBanner(
                botState = botState,
                onStart = onStartBotRequested,
                onPause = onPauseBotRequested,
                onResume = onResumeBotRequested,
                onCloseAll = onCloseAllPositions,
                onTriggerAcceptanceTest = onTriggerAcceptanceTest
            )
        }

        // 2. Key Performance Metrics Grid (2x3)
        item {
            PerformanceMetricsGrid(metrics = metrics, accountSnapshot = accountSnapshot)
        }

        // 3. Circuit Breaker Sentinel Panel
        item {
            CircuitBreakerPanel(circuitBreakers = circuitBreakers)
        }

        // 4. Active Positions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Positions (${openPositions.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (openPositions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToOrders) {
                        Text("View All", fontSize = 12.sp, color = BrandPrimary)
                    }
                }
            }
        }

        if (openPositions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerminalSurface)
                        .border(1.dp, TerminalCardBorder, RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active positions • Engine monitoring market signals", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(openPositions) { pos ->
                PositionCard(
                    position = pos,
                    onClose = { onClosePosition(pos.symbol) }
                )
            }
        }

        // 5. Recent Signals with Explainability triggers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Strategy Signals",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToChart) {
                    Text("Live Chart", fontSize = 12.sp, color = BrandPrimary)
                }
            }
        }

        if (recentSignals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerminalSurface)
                        .border(1.dp, TerminalCardBorder, RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Scanning candles for Mean Reversion triggers...", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(recentSignals.take(5)) { sig ->
                SignalRowCard(
                    signal = sig,
                    onClick = { selectedSignalForExplain = sig }
                )
            }
        }
    }

    // Explainability Modal
    selectedSignalForExplain?.let { sig ->
        SignalExplainabilityModal(
            signal = sig,
            onDismiss = { selectedSignalForExplain = null }
        )
    }
}

@Composable
private fun OperationalActionBanner(
    botState: BotState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCloseAll: () -> Unit,
    onTriggerAcceptanceTest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ENGINE CONTROL & ACCEPTANCE", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)

                Button(
                    onClick = onTriggerAcceptanceTest,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("run_acceptance_test_button")
                ) {
                    Text("TEST FULL CUJ", color = BrandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (botState) {
                    BotState.DISABLED, BotState.ARMED, BotState.ERROR -> {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("START BOT (12-STEP)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    BotState.RUNNING -> {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PAUSE ENGINE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    BotState.PAUSED -> {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESUME ENGINE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    BotState.RISK_HALTED -> {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("HALTED BY RISK SENTINEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onCloseAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("close_all_positions_button")
                ) {
                    Text("FLATTEN ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PerformanceMetricsGrid(
    metrics: PerformanceMetrics,
    accountSnapshot: AccountSnapshot
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "WIN RATE",
                value = "${String.format("%.1f", metrics.winRate * 100)}%",
                subValue = "${metrics.winningTrades}W / ${metrics.losingTrades}L",
                color = if (metrics.winRate >= 0.5) ProfitGreen else WarningAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "PROFIT FACTOR",
                value = String.format("%.2f", metrics.profitFactor),
                subValue = "Gross: $${String.format("%,.0f", metrics.grossProfit)}",
                color = if (metrics.profitFactor >= 1.5) ProfitGreen else BrandPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "SHARPE RATIO",
                value = String.format("%.2f", metrics.sharpeRatio),
                subValue = "Sortino: ${String.format("%.2f", metrics.sortinoRatio)}",
                color = if (metrics.sharpeRatio >= 1.0) ProfitGreen else TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "MAX DRAWDOWN",
                value = "-${String.format("%.1f", metrics.maxDrawdownPercent * 100)}%",
                subValue = "Limit: 3.00%",
                color = if (metrics.maxDrawdownPercent < 0.03) TextPrimary else LossRed,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "TOTAL TRADES",
                value = "${metrics.totalTrades}",
                subValue = "Expectancy: $${String.format("%.2f", metrics.expectancy)}",
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "OPEN P&L",
                value = "${if (accountSnapshot.unrealizedPnl >= 0) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(accountSnapshot.unrealizedPnl))}",
                subValue = "Cash: $${String.format("%,.0f", accountSnapshot.cashBalance)}",
                color = if (accountSnapshot.unrealizedPnl >= 0) ProfitGreen else LossRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subValue, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CircuitBreakerPanel(circuitBreakers: CircuitBreakerStatus) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (circuitBreakers.isHalted) LossRed else TerminalCardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (circuitBreakers.isHalted) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (circuitBreakers.isHalted) LossRed else ProfitGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RISK CIRCUIT BREAKERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = if (circuitBreakers.isHalted) "TRADING HALTED" else "ALL LIMITS NORMAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (circuitBreakers.isHalted) LossRed else ProfitGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircuitMetric(
                    label = "Consecutive Losses",
                    value = "${circuitBreakers.consecutiveLosses} / 3 max",
                    isWarning = circuitBreakers.consecutiveLosses >= 2
                )
                CircuitMetric(
                    label = "Daily Realized Loss",
                    value = "$${String.format("%.2f", circuitBreakers.dailyLossRealized)} / $6,000",
                    isWarning = circuitBreakers.dailyLossRealized > 4000.0
                )
                CircuitMetric(
                    label = "Peak Drawdown",
                    value = "${String.format("%.2f", circuitBreakers.peakDrawdownPercent * 100)}% / 3.0%",
                    isWarning = circuitBreakers.peakDrawdownPercent > 0.02
                )
            }
        }
    }
}

@Composable
private fun CircuitMetric(label: String, value: String, isWarning: Boolean) {
    Column {
        Text(label, fontSize = 9.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) LossRed else TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PositionCard(
    position: PositionEntity,
    onClose: () -> Unit
) {
    val isLong = position.direction == PositionDirection.LONG
    val isProfit = position.unrealizedPnl >= 0

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLong) ProfitGreenContainer else LossRedContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLong) "LONG" else "SHORT",
                            color = if (isLong) ProfitGreen else LossRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = position.symbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${position.quantity} shs @ $${String.format("%.2f", position.avgEntryPrice)}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${if (isProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(position.unrealizedPnl))} (${String.format("%.2f", position.unrealizedPnlPercent)}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProfit) ProfitGreen else LossRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    position.stopLoss?.let {
                        Text("SL: $${String.format("%.2f", it)}", fontSize = 10.sp, color = LossRed, fontFamily = FontFamily.Monospace)
                    }
                    position.takeProfit?.let {
                        Text("TP: $${String.format("%.2f", it)}", fontSize = 10.sp, color = ProfitGreen, fontFamily = FontFamily.Monospace)
                    }
                    Text("Current: $${String.format("%.2f", position.currentPrice)}", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("CLOSE", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SignalRowCard(
    signal: TradingSignal,
    onClick: () -> Unit
) {
    val isBuy = signal.signalType == SignalType.BUY
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBuy) ProfitGreenContainer else LossRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isBuy) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isBuy) ProfitGreen else LossRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(signal.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${signal.signalType.name} @ $${String.format("%.2f", signal.price)}",
                            fontSize = 12.sp,
                            color = if (isBuy) ProfitGreen else LossRed,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "RSI: ${String.format("%.1f", signal.rsiValue)} • BB Low: $${String.format("%.2f", signal.bbLower)}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeFormat.format(Date(signal.timestamp)), fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("Explain ▶", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
