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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.entity.PositionEntity
import com.example.data.local.entity.TradingSignal
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues
import com.example.data.model.Quote
import com.example.data.model.SignalType
import com.example.ui.components.CandlestickChart
import com.example.ui.components.MetricRow
import com.example.ui.theme.BollingerBandLine
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Sma200Line
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveChartScreen(
    currentSymbol: String,
    candles: List<Candle>,
    indicators: IndicatorValues?,
    currentQuote: Quote?,
    signals: List<TradingSignal>,
    activePositions: List<PositionEntity>,
    onSymbolSelected: (String) -> Unit,
    onTestSignalTriggered: (SignalType) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportedSymbols = listOf("SPY", "VOO", "QQQ", "IWM", "DIA")
    val timeframes = listOf("1m", "5m", "15m", "1h", "1d")
    var selectedTimeframe by remember { mutableStateOf("15m") }

    val activePos = activePositions.find { it.symbol == currentSymbol }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Symbol & Timeframe Selection Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Symbol Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(supportedSymbols) { sym ->
                        val isSelected = sym == currentSymbol
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandPrimary else TerminalSurface)
                                .border(1.dp, if (isSelected) BrandPrimary else TerminalCardBorder, RoundedCornerShape(8.dp))
                                .clickable { onSymbolSelected(sym) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("symbol_pill_$sym")
                        ) {
                            Text(
                                text = sym,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                // Timeframe Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) TerminalSurface else Color.Transparent)
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrandPrimary else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. Real-time Quote Ticker Banner
        item {
            currentQuote?.let { q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$${String.format("%.2f", q.lastPrice)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Text(
                                text = "Bid: $${String.format("%.2f", q.bid)} • Ask: $${String.format("%.2f", q.ask)}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val spreadPercent = ((q.ask - q.bid) / q.lastPrice) * 100
                            Text(
                                text = "Spread: ${String.format("%.3f", spreadPercent)}%",
                                fontSize = 11.sp,
                                color = if (spreadPercent <= 0.05) ProfitGreen else LossRed,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Vol: ${String.format("%,d", q.volume)}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 3. Main Candlestick, BB, SMA200, Volume & RSI Canvas Chart
        item {
            CandlestickChart(
                candles = candles,
                indicators = indicators,
                signals = signals,
                activeStopLoss = activePos?.stopLoss,
                activeTakeProfit = activePos?.takeProfit
            )
        }

        // 4. Indicator Deep-Dive Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("STRATEGY INDICATOR INSPECTOR", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    indicators?.let { ind ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetricRow(
                                label = "RSI (14-period)",
                                value = String.format("%.2f", ind.rsi),
                                highlightColor = when {
                                    ind.rsi < 30 -> ProfitGreen
                                    ind.rsi > 70 -> LossRed
                                    else -> BrandPrimary
                                }
                            )
                            MetricRow(label = "Bollinger Upper (20, 2.0σ)", value = "$${String.format("%.2f", ind.bbUpper)}", highlightColor = BollingerBandLine)
                            MetricRow(label = "Bollinger Middle (20 SMA)", value = "$${String.format("%.2f", ind.bbMiddle)}")
                            MetricRow(label = "Bollinger Lower (20, 2.0σ)", value = "$${String.format("%.2f", ind.bbLower)}", highlightColor = BollingerBandLine)
                            ind.sma200?.let {
                                MetricRow(label = "SPY Macro Regime (200 SMA)", value = "$${String.format("%.2f", it)}", highlightColor = Sma200Line)
                            }
                            MetricRow(
                                label = "Volume Ratio (vs 20 MA)",
                                value = "${String.format("%.2f", ind.volumeRatio)}x",
                                highlightColor = if (ind.volumeRatio >= 1.2) ProfitGreen else TextMuted
                            )
                        }
                    } ?: Text("Calculating technical indicators...", color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        // 5. Manual Strategy Signal Injector (for test & verification)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("MANUAL STRATEGY TEST TRIGGER", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("Injects a simulated signal for $currentSymbol into Risk Engine to verify execution pipeline.", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTestSignalTriggered(SignalType.BUY) },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SIMULATE BUY (LONG)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onTestSignalTriggered(SignalType.SELL) },
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SIMULATE EXIT (SELL)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
