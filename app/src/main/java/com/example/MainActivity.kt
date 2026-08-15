package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TradingViewModel
import com.example.ui.components.HeaderBar
import com.example.ui.components.StartSequenceDialog
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LiveChartScreen
import com.example.ui.screens.OrdersPositionsScreen
import com.example.ui.screens.StrategyConfigScreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.ETradeTraderTheme
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class TradingTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Assessment),
    CHART("Live Chart", Icons.Default.ShowChart),
    STRATEGY("Strategy", Icons.Default.Tune),
    ORDERS("Orders", Icons.Default.List),
    BACKTEST("Backtest", Icons.Default.AutoGraph),
    AUDIT("Audit", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ETradeTraderTheme {
                val viewModel: TradingViewModel = viewModel(
                    factory = TradingViewModel.provideFactory(application)
                )
                TradingApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingApp(viewModel: TradingViewModel) {
    var currentTab by remember { mutableStateOf(TradingTab.DASHBOARD) }
    var showStartSequenceDialog by remember { mutableStateOf(false) }

    val botState by viewModel.botState.collectAsStateWithLifecycle()
    val tradingMode by viewModel.tradingMode.collectAsStateWithLifecycle()
    val activeSymbol by viewModel.activeSymbol.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val candles by viewModel.candles.collectAsStateWithLifecycle()
    val indicators by viewModel.indicators.collectAsStateWithLifecycle()
    val recentSignals by viewModel.recentSignals.collectAsStateWithLifecycle()
    val accountSnapshot by viewModel.accountSnapshot.collectAsStateWithLifecycle()
    val circuitBreakers by viewModel.circuitBreakers.collectAsStateWithLifecycle()
    val openPositions by viewModel.openPositions.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allTrades by viewModel.allTrades.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val strategyConfig by viewModel.strategyConfig.collectAsStateWithLifecycle()
    val backtestResult by viewModel.backtestResult.collectAsStateWithLifecycle()
    val isRunningBacktest by viewModel.isRunningBacktest.collectAsStateWithLifecycle()
    val performanceMetrics by viewModel.performanceMetrics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            HeaderBar(
                botState = botState,
                tradingMode = tradingMode,
                accountSnapshot = accountSnapshot,
                circuitBreakers = circuitBreakers,
                onStartRequested = { showStartSequenceDialog = true },
                onPauseRequested = { viewModel.pauseBot() },
                onResumeRequested = { viewModel.resumeBot() },
                onEmergencyStopRequested = { viewModel.emergencyStop() },
                onTradingModeSelected = { viewModel.setTradingMode(it) },
                onResetCircuitBreakers = { op, reason -> viewModel.resetCircuitBreakers(op, reason) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = TerminalSurface,
                tonalElevation = 4.dp
            ) {
                TradingTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) BrandPrimary else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                color = if (isSelected) BrandPrimary else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPrimary,
                            indicatorColor = TerminalSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = com.example.ui.theme.TerminalBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                TradingTab.DASHBOARD -> {
                    DashboardScreen(
                        botState = botState,
                        accountSnapshot = accountSnapshot,
                        metrics = performanceMetrics,
                        circuitBreakers = circuitBreakers,
                        openPositions = openPositions,
                        recentSignals = recentSignals,
                        onStartBotRequested = { showStartSequenceDialog = true },
                        onPauseBotRequested = { viewModel.pauseBot() },
                        onResumeBotRequested = { viewModel.resumeBot() },
                        onClosePosition = { viewModel.closePosition(it) },
                        onCloseAllPositions = { viewModel.closeAllPositions() },
                        onTriggerAcceptanceTest = { viewModel.triggerAcceptanceTest() },
                        onNavigateToChart = { currentTab = TradingTab.CHART },
                        onNavigateToOrders = { currentTab = TradingTab.ORDERS }
                    )
                }
                TradingTab.CHART -> {
                    LiveChartScreen(
                        currentSymbol = activeSymbol,
                        candles = candles,
                        indicators = indicators,
                        currentQuote = currentQuote,
                        signals = recentSignals,
                        activePositions = openPositions,
                        onSymbolSelected = { viewModel.selectSymbol(it) },
                        onTestSignalTriggered = { viewModel.triggerManualTestSignal(it) }
                    )
                }
                TradingTab.STRATEGY -> {
                    StrategyConfigScreen(
                        currentConfig = strategyConfig,
                        onSaveConfig = { updated, op, reason ->
                            viewModel.updateStrategyConfig(updated, op, reason)
                        }
                    )
                }
                TradingTab.ORDERS -> {
                    OrdersPositionsScreen(
                        positions = openPositions,
                        orders = allOrders,
                        trades = allTrades,
                        onClosePosition = { viewModel.closePosition(it) },
                        onCancelOrder = { viewModel.cancelOrder(it) }
                    )
                }
                TradingTab.BACKTEST -> {
                    BacktestScreen(
                        currentConfig = strategyConfig,
                        backtestResult = backtestResult,
                        isRunningBacktest = isRunningBacktest,
                        onRunBacktest = { viewModel.runBacktest() }
                    )
                }
                TradingTab.AUDIT -> {
                    AuditLogScreen(
                        auditLogs = auditLogs
                    )
                }
            }
        }
    }

    // 12-Step Automated Trading Pre-Flight Verification Modal
    if (showStartSequenceDialog) {
        StartSequenceDialog(
            tradingMode = tradingMode,
            onConfirmedStart = {
                viewModel.startBot()
                showStartSequenceDialog = false
            },
            onDismiss = { showStartSequenceDialog = false }
        )
    }
}
