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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.BrokerOrderEntity
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.TradeEntity
import com.example.data.model.OrderAction
import com.example.data.model.OrderStatus
import com.example.data.model.PositionDirection
import com.example.ui.components.TradeExplainabilityModal
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
fun OrdersPositionsScreen(
    positions: List<PositionEntity>,
    orders: List<BrokerOrderEntity>,
    trades: List<TradeEntity>,
    onClosePosition: (String) -> Unit,
    onCancelOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Positions (${positions.size})", "Open Orders", "All Orders", "Trade History (${trades.size})")

    var selectedTradeForExplain by remember { mutableStateOf<TradeEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Tab Header
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = TerminalSurface,
            contentColor = BrandPrimary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BrandPrimary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) BrandPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Contents
        when (selectedTab) {
            0 -> {
                // Active Positions Tab
                if (positions.isEmpty()) {
                    EmptyStatePlaceholder("No active positions currently held in portfolio.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(positions) { pos ->
                            PositionDetailCard(pos = pos, onClose = { onClosePosition(pos.symbol) })
                        }
                    }
                }
            }
            1 -> {
                // Open Orders Tab
                val openOrders = orders.filter { it.status == OrderStatus.SUBMITTED || it.status == OrderStatus.PENDING_OPEN || it.status == OrderStatus.PARTIALLY_FILLED }
                if (openOrders.isEmpty()) {
                    EmptyStatePlaceholder("No open pending orders at broker.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(openOrders) { ord ->
                            OrderDetailCard(order = ord, onCancel = { onCancelOrder(ord.clientOrderId) })
                        }
                    }
                }
            }
            2 -> {
                // All Orders History
                if (orders.isEmpty()) {
                    EmptyStatePlaceholder("No order history records.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(orders) { ord ->
                            OrderDetailCard(order = ord, onCancel = { onCancelOrder(ord.clientOrderId) })
                        }
                    }
                }
            }
            3 -> {
                // Trade History (Closed positions)
                if (trades.isEmpty()) {
                    EmptyStatePlaceholder("No completed trade records.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(trades) { trade ->
                            TradeHistoryCard(trade = trade, onClick = { selectedTradeForExplain = trade })
                        }
                    }
                }
            }
        }
    }

    // Trade Explainability Modal
    selectedTradeForExplain?.let { tr ->
        TradeExplainabilityModal(
            trade = tr,
            onDismiss = { selectedTradeForExplain = null }
        )
    }
}

@Composable
private fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalCardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PositionDetailCard(pos: PositionEntity, onClose: () -> Unit) {
    val isLong = pos.direction == PositionDirection.LONG
    val isProfit = pos.unrealizedPnl >= 0

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    Text(pos.symbol, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${pos.quantity} shares",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${if (isProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(pos.unrealizedPnl))} (${String.format("%.2f", pos.unrealizedPnlPercent)}%)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProfit) ProfitGreen else LossRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Avg Entry", fontSize = 10.sp, color = TextMuted)
                    Text("$${String.format("%.2f", pos.avgEntryPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("Current Price", fontSize = 10.sp, color = TextMuted)
                    Text("$${String.format("%.2f", pos.currentPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("Stop Loss", fontSize = 10.sp, color = TextMuted)
                    Text("$${String.format("%.2f", pos.stopLoss ?: 0.0)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LossRed, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("Take Profit", fontSize = 10.sp, color = TextMuted)
                    Text("$${String.format("%.2f", pos.takeProfit ?: 0.0)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("CLOSE POSITION", color = LossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OrderDetailCard(order: BrokerOrderEntity, onCancel: () -> Unit) {
    val isBuy = order.action == OrderAction.BUY
    val isOpen = order.status == OrderStatus.SUBMITTED || order.status == OrderStatus.PENDING_OPEN

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBuy) ProfitGreenContainer else LossRedContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = order.action.name,
                            color = if (isBuy) ProfitGreen else LossRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(order.symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${order.filledQuantity}/${order.quantity} shs",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (order.status) {
                                OrderStatus.FILLED -> ProfitGreenContainer
                                OrderStatus.CANCELLED -> TerminalSurfaceVariant
                                OrderStatus.REJECTED -> LossRedContainer
                                else -> WarningAmberContainer
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = order.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            OrderStatus.FILLED -> ProfitGreen
                            OrderStatus.CANCELLED -> TextMuted
                            OrderStatus.REJECTED -> LossRed
                            else -> WarningAmber
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Limit: $${String.format("%.2f", order.limitPrice ?: 0.0)}", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text("Client ID: ${order.clientOrderId.take(12)}", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }

                if (isOpen) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("CANCEL", color = LossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeHistoryCard(trade: TradeEntity, onClick: () -> Unit) {
    val isProfit = (trade.pnl ?: 0.0) >= 0.0
    val pnl = trade.pnl ?: 0.0
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(trade.symbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${trade.quantity} shs @ $${String.format("%.2f", trade.entryPrice)} → $${String.format("%.2f", trade.exitPrice ?: 0.0)}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${dateFormat.format(Date(trade.entryTime))} • ${trade.exitReason ?: "Target reached"}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(pnl))}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProfit) ProfitGreen else LossRed
                )
                Text(
                    text = "Explain ▶",
                    fontSize = 10.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
