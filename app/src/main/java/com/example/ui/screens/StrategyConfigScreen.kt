package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.entity.StrategyConfiguration
import com.example.ui.components.MetricRow
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun StrategyConfigScreen(
    currentConfig: StrategyConfiguration,
    onSaveConfig: (updatedConfig: StrategyConfiguration, operator: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rsiPeriod by remember(currentConfig) { mutableIntStateOf(currentConfig.rsiPeriod) }
    var rsiOversold by remember(currentConfig) { mutableDoubleStateOf(currentConfig.rsiOversoldThreshold) }
    var rsiOverbought by remember(currentConfig) { mutableDoubleStateOf(currentConfig.rsiOverboughtThreshold) }
    var rsiExitLong by remember(currentConfig) { mutableDoubleStateOf(currentConfig.rsiExitLongThreshold) }

    var bbPeriod by remember(currentConfig) { mutableIntStateOf(currentConfig.bollingerPeriod) }
    var bbStdDev by remember(currentConfig) { mutableDoubleStateOf(currentConfig.bollingerStdDev) }

    var takeProfitPercent by remember(currentConfig) { mutableDoubleStateOf(currentConfig.takeProfitPercent) }
    var stopLossPercent by remember(currentConfig) { mutableDoubleStateOf(currentConfig.stopLossPercent) }
    var maxRiskPercent by remember(currentConfig) { mutableDoubleStateOf(currentConfig.maxRiskPerTradePercent) }

    var requireTrendFilter by remember(currentConfig) { mutableStateOf(currentConfig.requireTrendFilter) }
    var minVolumeRatio by remember(currentConfig) { mutableDoubleStateOf(currentConfig.minVolumeRatio) }

    var showAuditReasonModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Strategy Title & Overview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mean Reversion Strategy V1",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Statistical mean-reversion algorithm operating on 15-minute timeframe candles. Generates Long entries when price tests Lower Bollinger Band with oversold RSI and positive SPY 200 SMA trend support.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 1. RSI Indicator Thresholds
        item {
            ConfigSectionCard(title = "RSI INDICATOR PARAMETERS") {
                ConfigSliderItem(
                    label = "RSI Period",
                    value = rsiPeriod.toFloat(),
                    valueText = "$rsiPeriod bars",
                    range = 5f..30f,
                    onValueChange = { rsiPeriod = it.toInt() }
                )
                ConfigSliderItem(
                    label = "Long Oversold Entry Threshold",
                    value = rsiOversold.toFloat(),
                    valueText = "< ${String.format("%.0f", rsiOversold)}",
                    range = 15f..40f,
                    onValueChange = { rsiOversold = it.toDouble() }
                )
                ConfigSliderItem(
                    label = "Long Mean Reversion Exit Threshold",
                    value = rsiExitLong.toFloat(),
                    valueText = "> ${String.format("%.0f", rsiExitLong)}",
                    range = 45f..65f,
                    onValueChange = { rsiExitLong = it.toDouble() }
                )
                ConfigSliderItem(
                    label = "Short Overbought Entry Threshold",
                    value = rsiOverbought.toFloat(),
                    valueText = "> ${String.format("%.0f", rsiOverbought)}",
                    range = 60f..85f,
                    onValueChange = { rsiOverbought = it.toDouble() }
                )
            }
        }

        // 2. Bollinger Bands
        item {
            ConfigSectionCard(title = "BOLLINGER BANDS SETTINGS") {
                ConfigSliderItem(
                    label = "Bollinger Period (SMA)",
                    value = bbPeriod.toFloat(),
                    valueText = "$bbPeriod bars",
                    range = 10f..50f,
                    onValueChange = { bbPeriod = it.toInt() }
                )
                ConfigSliderItem(
                    label = "Standard Deviation Multiplier (σ)",
                    value = bbStdDev.toFloat(),
                    valueText = "${String.format("%.1f", bbStdDev)} σ",
                    range = 1.5f..3.0f,
                    onValueChange = { bbStdDev = it.toDouble() }
                )
            }
        }

        // 3. Risk & Position Sizing Targets
        item {
            ConfigSectionCard(title = "RISK ENGINE & TARGET PARAMETERS") {
                ConfigSliderItem(
                    label = "Target Profit Percentage",
                    value = (takeProfitPercent * 100).toFloat(),
                    valueText = "+${String.format("%.2f", takeProfitPercent * 100)}%",
                    range = 0.5f..5.0f,
                    onValueChange = { takeProfitPercent = (it / 100f).toDouble() }
                )
                ConfigSliderItem(
                    label = "Hard Stop Loss Percentage",
                    value = (stopLossPercent * 100).toFloat(),
                    valueText = "-${String.format("%.2f", stopLossPercent * 100)}%",
                    range = 0.5f..3.0f,
                    onValueChange = { stopLossPercent = (it / 100f).toDouble() }
                )
                ConfigSliderItem(
                    label = "Max Capital Risk Per Trade",
                    value = (maxRiskPercent * 100).toFloat(),
                    valueText = "${String.format("%.1f", maxRiskPercent * 100)}% equity",
                    range = 0.5f..3.0f,
                    onValueChange = { maxRiskPercent = (it / 100f).toDouble() }
                )
            }
        }

        // 4. Regime & Liquidity Filters
        item {
            ConfigSectionCard(title = "REGIME & LIQUIDITY FILTERS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require SPY > 200 SMA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Suppresses Long signals during broader bear market regimes", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = requireTrendFilter,
                        onCheckedChange = { requireTrendFilter = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandPrimary, checkedTrackColor = BrandPrimary.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ConfigSliderItem(
                    label = "Minimum Volume Ratio (vs 20 MA)",
                    value = minVolumeRatio.toFloat(),
                    valueText = "${String.format("%.1f", minVolumeRatio)}x",
                    range = 0.5f..2.5f,
                    onValueChange = { minVolumeRatio = it.toDouble() }
                )
            }
        }

        // 5. Impact Simulation & Apply Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("PARAMETER IMPACT PREVIEW", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estimated Trade Freq", fontSize = 10.sp, color = TextMuted)
                            Text("3 - 6 trades/wk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("Risk-Reward Ratio", fontSize = 10.sp, color = TextMuted)
                            val rrr = takeProfitPercent / stopLossPercent
                            Text("1 : ${String.format("%.2f", rrr)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        }
                        Column {
                            Text("Max Position Exposure", fontSize = 10.sp, color = TextMuted)
                            Text("15% portfolio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showAuditReasonModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_strategy_config_button")
                    ) {
                        Text("APPLY CONFIGURATION (AUDIT LOGGED)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Operator Justification Modal (Requirement: Parameter changes must be audited with operator name and reason)
    if (showAuditReasonModal) {
        var operatorName by remember { mutableStateOf("Lead Trader") }
        var justificationReason by remember { mutableStateOf("Optimized RSI entry threshold based on WFA backtest") }

        AlertDialog(
            onDismissRequest = { showAuditReasonModal = false },
            title = { Text("Audit Log Justification", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "All strategy parameter modifications are cryptographically timestamped and committed to the immutable audit database.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = operatorName,
                        onValueChange = { operatorName = it },
                        label = { Text("Operator Name / ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = justificationReason,
                        onValueChange = { justificationReason = it },
                        label = { Text("Justification Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = currentConfig.copy(
                            rsiPeriod = rsiPeriod,
                            rsiOversoldThreshold = rsiOversold,
                            rsiOverboughtThreshold = rsiOverbought,
                            rsiExitLongThreshold = rsiExitLong,
                            bollingerPeriod = bbPeriod,
                            bollingerStdDev = bbStdDev,
                            takeProfitPercent = takeProfitPercent,
                            stopLossPercent = stopLossPercent,
                            maxRiskPerTradePercent = maxRiskPercent,
                            requireTrendFilter = requireTrendFilter,
                            minVolumeRatio = minVolumeRatio
                        )
                        onSaveConfig(updated, operatorName, justificationReason)
                        showAuditReasonModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("SIGN & COMMIT AUDIT", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuditReasonModal = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ConfigSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ConfigSliderItem(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, color = TextPrimary)
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = BrandPrimary,
                activeTrackColor = BrandPrimary,
                inactiveTrackColor = TerminalSurfaceVariant
            )
        )
    }
}
