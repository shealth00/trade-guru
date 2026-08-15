package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AccountSnapshot
import com.example.data.model.Candle
import com.example.data.model.SignalType
import com.example.data.model.StrategyConfig
import com.example.engine.broker.BacktestEngine
import com.example.engine.broker.PaperBroker
import com.example.engine.risk.PositionSizer
import com.example.engine.risk.RiskEngine
import com.example.engine.strategy.MeanReversionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("E*TRADE AutoTrader", appName)
    }

    @Test
    fun `test position sizer risk calculations`() {
        val sizer = PositionSizer()
        val account = AccountSnapshot(
            accountEquity = 100000.0,
            cashBalance = 100000.0,
            buyingPower = 200000.0
        )
        val config = StrategyConfig(maxRiskPerTradePercent = 0.02)
        // Entry: $500, Stop Loss: $495 (Risk per share: $5). Max dollar risk: $2000 => 400 shares
        val qty = sizer.calculateQuantity(account, config, entryPrice = 500.0, stopLossPrice = 495.0)
        assertTrue(qty > 0)
    }

    @Test
    fun `test risk engine circuit breaker tripping`() {
        val riskEngine = RiskEngine()
        val account = AccountSnapshot(accountEquity = 90000.0) // 10% drawdown

        riskEngine.recordTradeResult(-2500.0, 97500.0)
        riskEngine.recordTradeResult(-2500.0, 95000.0)
        riskEngine.recordTradeResult(-2500.0, 92500.0) // 3 consecutive losses => should trip

        assertTrue(riskEngine.circuitBreakers.value.isHalted)
        assertEquals(3, riskEngine.circuitBreakers.value.consecutiveLosses)

        // Reset
        riskEngine.resetCircuitBreakers("Lead Operator", "Acceptance test reset")
        assertTrue(!riskEngine.circuitBreakers.value.isHalted)
    }

    @Test
    fun `test backtest engine runs successfully`() {
        val engine = BacktestEngine()
        val result = engine.runBacktest(StrategyConfig(), initialBalance = 100000.0, candleCount = 100)
        assertNotNull(result)
        assertTrue(result.equityCurve.isNotEmpty())
    }
}
