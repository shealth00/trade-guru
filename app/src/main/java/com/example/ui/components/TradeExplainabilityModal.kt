package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.TradeEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TradeExplainabilityModal(
    trade: TradeEntity,
    onDismiss: () -> Unit
) {
    val isProfit = (trade.pnl ?: 0.0) >= 0.0
    val pnl = trade.pnl ?: 0.0
    val pnlPercent = trade.pnlPercent ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("trade_explainability_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${trade.symbol} • ${trade.side}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Trade #${trade.id.take(8)}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isProfit) ProfitGreenContainer else LossRedContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (isProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(pnl))} (${String.format("%.2f", pnlPercent)}%)",
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) ProfitGreen else LossRed,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exit Reason Badge & Narrative
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerminalSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Column {
                        Text("EXIT CLASSIFICATION & REASON", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trade.exitReason ?: "Mean Reversion target reached",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Execution Timeline & Metrics
                Text("EXECUTION DETAILS", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, TerminalCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    MetricRow(label = "Entry Time", value = dateFormat.format(Date(trade.entryTime)))
                    trade.exitTime?.let {
                        MetricRow(label = "Exit Time", value = dateFormat.format(Date(it)))
                        val durationMinutes = ((it - trade.entryTime) / 60000).coerceAtLeast(1)
                        MetricRow(label = "Holding Duration", value = "$durationMinutes minutes")
                    }
                    MetricRow(label = "Position Size", value = "${trade.quantity} shares")
                    MetricRow(label = "Entry Price", value = "$${String.format("%.2f", trade.entryPrice)}")
                    trade.exitPrice?.let {
                        MetricRow(label = "Exit Price", value = "$${String.format("%.2f", it)}")
                    }
                    MetricRow(label = "Gross P&L", value = "$${String.format("%.2f", pnl)}")
                    MetricRow(label = "Fees / Commission", value = "$${String.format("%.2f", trade.commission)}")
                    MetricRow(
                        label = "Net P&L",
                        value = "${if (isProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(pnl - trade.commission))}",
                        highlightColor = if (isProfit) ProfitGreen else LossRed
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
