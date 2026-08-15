package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TradingSignal
import com.example.data.model.Candle
import com.example.data.model.IndicatorValues
import com.example.data.model.SignalType
import com.example.ui.theme.BollingerBandLine
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Sma200Line
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalGridLine
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    indicators: IndicatorValues?,
    signals: List<TradingSignal> = emptyList(),
    activeStopLoss: Double? = null,
    activeTakeProfit: Double? = null,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(TerminalSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Awaiting market candle feed...", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }
    val displayCandle = selectedCandleIndex?.let { idx ->
        if (idx in candles.indices) candles[idx] else candles.lastOrNull()
    } ?: candles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .padding(8.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Chart Header with Selected / Current Candle metrics
        displayCandle?.let { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(c.timestamp))
                Text(
                    text = "${c.symbol} • $timeStr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OhlcItem(label = "O", value = c.open)
                    OhlcItem(label = "H", value = c.high)
                    OhlcItem(label = "L", value = c.low)
                    OhlcItem(label = "C", value = c.close, isClose = true, isOpen = c.open)
                }
            }
        }

        // Indicators Mini Legend
        indicators?.let { ind ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("RSI(14): ${String.format("%.1f", ind.rsi)}", fontSize = 10.sp, color = BrandPrimary, fontFamily = FontFamily.Monospace)
                Text("BB U: ${String.format("%.2f", ind.bbUpper)}", fontSize = 10.sp, color = BollingerBandLine, fontFamily = FontFamily.Monospace)
                Text("BB L: ${String.format("%.2f", ind.bbLower)}", fontSize = 10.sp, color = BollingerBandLine, fontFamily = FontFamily.Monospace)
                ind.sma200?.let {
                    Text("SMA200: ${String.format("%.2f", it)}", fontSize = 10.sp, color = Sma200Line, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Candlestick + Volume + RSI Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val candleWidth = size.width / candles.size
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedCandleIndex = index
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectDragGestures { change, _ ->
                            val candleWidth = size.width / candles.size
                            val index = (change.position.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                            selectedCandleIndex = index
                        }
                    }
            ) {
                val totalWidth = size.width
                val totalHeight = size.height

                // Layout subdivisions:
                // Main Price Chart: 0% to 65% of height
                // Volume Chart: 65% to 80% of height
                // RSI Chart: 80% to 100% of height
                val priceHeight = totalHeight * 0.65f
                val volumeTop = totalHeight * 0.67f
                val volumeHeight = totalHeight * 0.13f
                val rsiTop = totalHeight * 0.82f
                val rsiHeight = totalHeight * 0.16f

                // Find Price Range (Min/Max) with safety margin
                val minPrice = candles.minOfOrNull { it.low }?.times(0.998) ?: 1.0
                val maxPrice = candles.maxOfOrNull { it.high }?.times(1.002) ?: 2.0
                val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)

                val candleCount = candles.size
                val stepX = totalWidth / candleCount.toFloat()
                val candleBodyWidth = (stepX * 0.65f).coerceAtLeast(2f)

                // 1. Draw Grid Lines
                drawGrid(
                    totalWidth = totalWidth,
                    priceHeight = priceHeight,
                    volumeTop = volumeTop,
                    volumeHeight = volumeHeight,
                    rsiTop = rsiTop,
                    rsiHeight = rsiHeight,
                    minPrice = minPrice,
                    maxPrice = maxPrice
                )

                // 2. Draw Stop Loss & Take Profit overlays if present
                activeStopLoss?.let { sl ->
                    if (sl in minPrice..maxPrice) {
                        val y = priceHeight - ((sl - minPrice) / priceRange * priceHeight).toFloat()
                        drawLine(
                            color = LossRed.copy(alpha = 0.8f),
                            start = Offset(0f, y),
                            end = Offset(totalWidth, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }
                }
                activeTakeProfit?.let { tp ->
                    if (tp in minPrice..maxPrice) {
                        val y = priceHeight - ((tp - minPrice) / priceRange * priceHeight).toFloat()
                        drawLine(
                            color = ProfitGreen.copy(alpha = 0.8f),
                            start = Offset(0f, y),
                            end = Offset(totalWidth, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }
                }

                // 3. Draw Candlesticks & Volume Bars
                val maxVol = candles.maxOfOrNull { it.volume }?.toFloat()?.coerceAtLeast(1f) ?: 1f

                candles.forEachIndexed { i, c ->
                    val centerX = i * stepX + (stepX / 2f)
                    val isBull = c.close >= c.open
                    val candleColor = if (isBull) ProfitGreen else LossRed

                    // Price Chart Y coords
                    val highY = priceHeight - ((c.high - minPrice) / priceRange * priceHeight).toFloat()
                    val lowY = priceHeight - ((c.low - minPrice) / priceRange * priceHeight).toFloat()
                    val openY = priceHeight - ((c.open - minPrice) / priceRange * priceHeight).toFloat()
                    val closeY = priceHeight - ((c.close - minPrice) / priceRange * priceHeight).toFloat()

                    val bodyTop = kotlin.math.min(openY, closeY)
                    val bodyHeight = kotlin.math.max(kotlin.math.abs(closeY - openY), 1.5f)

                    // Draw Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.2f
                    )

                    // Draw Body
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(centerX - candleBodyWidth / 2f, bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )

                    // Volume Bar
                    val volBarHeight = (c.volume.toFloat() / maxVol) * volumeHeight
                    drawRect(
                        color = candleColor.copy(alpha = 0.45f),
                        topLeft = Offset(centerX - candleBodyWidth / 2f, volumeTop + volumeHeight - volBarHeight),
                        size = Size(candleBodyWidth, volBarHeight)
                    )
                }

                // 4. Draw Bollinger Bands & SMA overlays
                indicators?.let { ind ->
                    if (ind.bbUpper in minPrice..maxPrice) {
                        val upperY = priceHeight - ((ind.bbUpper - minPrice) / priceRange * priceHeight).toFloat()
                        drawLine(BollingerBandLine.copy(alpha = 0.6f), Offset(0f, upperY), Offset(totalWidth, upperY), 1f)
                    }
                    if (ind.bbLower in minPrice..maxPrice) {
                        val lowerY = priceHeight - ((ind.bbLower - minPrice) / priceRange * priceHeight).toFloat()
                        drawLine(BollingerBandLine.copy(alpha = 0.6f), Offset(0f, lowerY), Offset(totalWidth, lowerY), 1f)
                    }
                    if (ind.bbMiddle in minPrice..maxPrice) {
                        val midY = priceHeight - ((ind.bbMiddle - minPrice) / priceRange * priceHeight).toFloat()
                        drawLine(BollingerBandLine.copy(alpha = 0.3f), Offset(0f, midY), Offset(totalWidth, midY), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                    }
                    ind.sma200?.let { sma ->
                        if (sma in minPrice..maxPrice) {
                            val smaY = priceHeight - ((sma - minPrice) / priceRange * priceHeight).toFloat()
                            drawLine(Sma200Line, Offset(0f, smaY), Offset(totalWidth, smaY), 1.5f)
                        }
                    }
                }

                // 5. Draw Signals (Buy / Sell Triangles)
                signals.takeLast(10).forEach { sig ->
                    // Find candle matching timestamp or latest
                    val targetCandleIdx = candles.indexOfFirst { it.timestamp >= sig.timestamp }.takeIf { it >= 0 } ?: (candles.size - 1)
                    val sigX = targetCandleIdx * stepX + (stepX / 2f)
                    val candle = candles[targetCandleIdx]

                    if (sig.signalType == SignalType.BUY) {
                        val lowY = priceHeight - ((candle.low - minPrice) / priceRange * priceHeight).toFloat()
                        drawTriangle(
                            center = Offset(sigX, lowY + 12f),
                            size = 8f,
                            isPointingUp = true,
                            color = ProfitGreen
                        )
                    } else if (sig.signalType == SignalType.SELL || sig.signalType == SignalType.SHORT) {
                        val highY = priceHeight - ((candle.high - minPrice) / priceRange * priceHeight).toFloat()
                        drawTriangle(
                            center = Offset(sigX, highY - 12f),
                            size = 8f,
                            isPointingUp = false,
                            color = LossRed
                        )
                    }
                }

                // 6. Draw RSI Sub-Chart
                drawRsiSection(
                    totalWidth = totalWidth,
                    rsiTop = rsiTop,
                    rsiHeight = rsiHeight,
                    rsiValue = indicators?.rsi ?: 50.0
                )

                // 7. Selected Cursor crosshair
                selectedCandleIndex?.let { selIdx ->
                    if (selIdx in candles.indices) {
                        val crossX = selIdx * stepX + (stepX / 2f)
                        drawLine(
                            color = TextMuted.copy(alpha = 0.5f),
                            start = Offset(crossX, 0f),
                            end = Offset(crossX, totalHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(
    totalWidth: Float,
    priceHeight: Float,
    volumeTop: Float,
    volumeHeight: Float,
    rsiTop: Float,
    rsiHeight: Float,
    minPrice: Double,
    maxPrice: Double
) {
    // Horizontal price grid lines (3 divisions)
    for (i in 1..3) {
        val y = priceHeight * (i / 4f)
        drawLine(TerminalGridLine, Offset(0f, y), Offset(totalWidth, y), 0.8f)
    }

    // Dividers between chart panes
    drawLine(TerminalCardBorder, Offset(0f, priceHeight + 2f), Offset(totalWidth, priceHeight + 2f), 1f)
    drawLine(TerminalCardBorder, Offset(0f, volumeTop + volumeHeight + 2f), Offset(totalWidth, volumeTop + volumeHeight + 2f), 1f)
}

private fun DrawScope.drawRsiSection(
    totalWidth: Float,
    rsiTop: Float,
    rsiHeight: Float,
    rsiValue: Double
) {
    // RSI 70 Line (Overbought)
    val y70 = rsiTop + rsiHeight * 0.3f
    drawLine(
        color = LossRed.copy(alpha = 0.4f),
        start = Offset(0f, y70),
        end = Offset(totalWidth, y70),
        strokeWidth = 0.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
    )

    // RSI 30 Line (Oversold)
    val y30 = rsiTop + rsiHeight * 0.7f
    drawLine(
        color = ProfitGreen.copy(alpha = 0.4f),
        start = Offset(0f, y30),
        end = Offset(totalWidth, y30),
        strokeWidth = 0.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
    )

    // RSI Level Marker Line
    val clampedRsi = rsiValue.coerceIn(0.0, 100.0).toFloat()
    val rsiY = rsiTop + rsiHeight * (1f - (clampedRsi / 100f))

    drawLine(
        color = BrandPrimary,
        start = Offset(0f, rsiY),
        end = Offset(totalWidth, rsiY),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawTriangle(
    center: Offset,
    size: Float,
    isPointingUp: Boolean,
    color: Color
) {
    val path = Path()
    if (isPointingUp) {
        path.moveTo(center.x, center.y - size)
        path.lineTo(center.x - size, center.y + size)
        path.lineTo(center.x + size, center.y + size)
    } else {
        path.moveTo(center.x, center.y + size)
        path.lineTo(center.x - size, center.y - size)
        path.lineTo(center.x + size, center.y - size)
    }
    path.close()
    drawPath(path, color = color)
}

@Composable
private fun OhlcItem(
    label: String,
    value: Double,
    isClose: Boolean = false,
    isOpen: Double = 0.0
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.width(2.dp))
        val color = if (isClose) {
            if (value >= isOpen) ProfitGreen else LossRed
        } else TextSecondary

        Text(
            text = String.format("%.2f", value),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}
