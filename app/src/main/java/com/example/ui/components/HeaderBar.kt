package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CircuitBreakerStatus
import com.example.data.model.AccountSnapshot
import com.example.data.model.BotState
import com.example.data.model.TradingMode
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

@Composable
fun HeaderBar(
    botState: BotState,
    tradingMode: TradingMode,
    accountSnapshot: AccountSnapshot,
    circuitBreakers: CircuitBreakerStatus,
    onStartRequested: () -> Unit,
    onPauseRequested: () -> Unit,
    onResumeRequested: () -> Unit,
    onEmergencyStopRequested: () -> Unit,
    onTradingModeSelected: (TradingMode) -> Unit,
    onResetCircuitBreakers: (operator: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLiveConfirmDialog by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<TradingMode?>(null) }
    var showEmergencyConfirmDialog by remember { mutableStateOf(false) }
    var showResetCbDialog by remember { mutableStateOf(false) }

    Surface(
        color = TerminalSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TerminalCardBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Logo/Title, Mode selector, Bot Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimary.copy(alpha = 0.2f))
                            .border(1.dp, BrandPrimary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E*",
                            color = BrandPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "E*TRADE AutoTrader",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mean Reversion V1 • ${tradingMode.label}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Bot State Indicator Pill
                BotStatePill(
                    botState = botState,
                    onClick = {
                        when (botState) {
                            BotState.DISABLED, BotState.ARMED, BotState.ERROR -> onStartRequested()
                            BotState.PAUSED -> onResumeRequested()
                            BotState.RUNNING -> onPauseRequested()
                            BotState.RISK_HALTED -> showResetCbDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Trading Mode Segmented Control & Account Financials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Selector Buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceVariant)
                        .padding(2.dp)
                ) {
                    TradingMode.values().forEach { mode ->
                        val isSelected = tradingMode == mode
                        val btnColor = when {
                            isSelected && mode == TradingMode.LIVE -> LossRed
                            isSelected && mode == TradingMode.SANDBOX -> WarningAmber
                            isSelected -> BrandPrimary
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(btnColor)
                                .clickable {
                                    if (mode == TradingMode.LIVE && tradingMode != TradingMode.LIVE) {
                                        pendingMode = mode
                                        showLiveConfirmDialog = true
                                    } else {
                                        onTradingModeSelected(mode)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("mode_selector_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                // Circuit Breaker quick status icon
                if (circuitBreakers.isHalted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LossRedContainer)
                            .border(1.dp, LossRed, RoundedCornerShape(6.dp))
                            .clickable { showResetCbDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Halted", tint = LossRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HALTED (Reset)", color = LossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Metric Row: Equity, Buying Power, Day's P&L, Emergency Kill Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACCOUNT EQUITY", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$${String.format("%,.2f", accountSnapshot.accountEquity)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("BUYING POWER", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$${String.format("%,.2f", accountSnapshot.buyingPower)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BrandPrimary
                    )
                }

                Column {
                    Text("REALIZED P&L", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    val pnl = accountSnapshot.realizedPnlToday
                    val isPositive = pnl >= 0
                    val pnlText = "${if (isPositive) "+$" else "-$"}${String.format("%,.2f", kotlin.math.abs(pnl))}"
                    Text(
                        text = pnlText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPositive) ProfitGreen else LossRed
                    )
                }

                // Emergency Kill Switch Button
                Button(
                    onClick = { showEmergencyConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("emergency_stop_button")
                ) {
                    Text("KILL SWITCH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }

    // Live Mode Confirmation Dialog (Hard safety barrier)
    if (showLiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLiveConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ARM LIVE TRADING?", fontWeight = FontWeight.Bold, color = LossRed)
                }
            },
            text = {
                Text(
                    "You are about to switch to LIVE TRADING. Real capital will be risked at E*TRADE broker. All orders submitted by the engine will be executed directly in the market.\n\nEnsure risk limits, position sizing, and account balances are verified.",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingMode?.let { onTradingModeSelected(it) }
                        showLiveConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("I UNDERSTAND - ARM LIVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveConfirmDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Emergency Stop Confirmation Dialog
    if (showEmergencyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirmDialog = false },
            title = { Text("TRIGGER EMERGENCY KILL SWITCH?", fontWeight = FontWeight.Bold, color = LossRed) },
            text = {
                Text(
                    "This will IMMEDIATELY:\n• Halt automated strategy execution\n• Cancel all active pending broker orders\n• Put bot in DISABLED status",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmergencyStopRequested()
                        showEmergencyConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("HALT ALL TRADING NOW")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyConfirmDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reset Circuit Breakers Dialog
    if (showResetCbDialog) {
        var operatorName by remember { mutableStateOf("Operator") }
        var resetReason by remember { mutableStateOf("Manual review confirmed risk clearance") }

        AlertDialog(
            onDismissRequest = { showResetCbDialog = false },
            title = { Text("Reset Circuit Breakers", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Current Status: ${if (circuitBreakers.isHalted) circuitBreakers.haltReason ?: "Halted" else "Normal"}",
                        fontSize = 12.sp,
                        color = if (circuitBreakers.isHalted) LossRed else ProfitGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Operator Name: $operatorName", fontSize = 12.sp, color = TextSecondary)
                    Text("Resetting will clear daily loss and consecutive loss triggers and return bot to PAUSED status for re-arming.", fontSize = 12.sp, color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetCircuitBreakers(operatorName, resetReason)
                        showResetCbDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("RESET BREAKERS", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCbDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun BotStatePill(
    botState: BotState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (bgColor, borderColor, textColor, icon) = when (botState) {
        BotState.RUNNING -> Quad(ProfitGreenContainer, ProfitGreen, ProfitGreen, "RUNNING")
        BotState.ARMED -> Quad(ProfitGreenContainer, ProfitGreen, ProfitGreen, "ARMED")
        BotState.PAUSED -> Quad(WarningAmberContainer, WarningAmber, WarningAmber, "PAUSED")
        BotState.DISABLED -> Quad(TerminalSurfaceVariant, TextMuted, TextSecondary, "DISABLED")
        BotState.RISK_HALTED, BotState.ERROR -> Quad(LossRedContainer, LossRed, LossRed, "RISK HALTED")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("bot_state_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(borderColor)
                    .alpha(if (botState == BotState.RUNNING || botState == BotState.RISK_HALTED) alphaAnim else 1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = textColor.let { icon },
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
