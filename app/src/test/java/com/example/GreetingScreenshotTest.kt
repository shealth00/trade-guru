package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AccountSnapshot
import com.example.data.model.BotState
import com.example.data.model.CircuitBreakerStatus
import com.example.data.model.TradingMode
import com.example.ui.components.HeaderBar
import com.example.ui.theme.ETradeTraderTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      ETradeTraderTheme {
        HeaderBar(
          botState = BotState.RUNNING,
          tradingMode = TradingMode.PAPER,
          accountSnapshot = AccountSnapshot(
            accountEquity = 100000.0,
            cashBalance = 100000.0,
            buyingPower = 200000.0,
            realizedPnlToday = 450.0,
            unrealizedPnl = 120.0
          ),
          circuitBreakers = CircuitBreakerStatus(),
          onStartRequested = {},
          onPauseRequested = {},
          onResumeRequested = {},
          onEmergencyStopRequested = {},
          onTradingModeSelected = {},
          onResetCircuitBreakers = { _, _ -> }
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/header_bar.png")
  }
}
