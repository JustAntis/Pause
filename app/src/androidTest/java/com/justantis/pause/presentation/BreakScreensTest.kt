package com.justantis.pause.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.domain.friction.FrictionConfig
import com.justantis.pause.presentation.block.BlockIntent
import com.justantis.pause.presentation.block.BlockState
import com.justantis.pause.presentation.block.PauseScreen
import com.justantis.pause.presentation.block.PurposeScreen
import com.justantis.pause.presentation.theme.PauseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreakScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nextIsDisabledDuringTheCountdownAndEnabledAfterIt() {
        var state by mutableStateOf(BlockState(isLoading = false, pauseSeconds = 30, pauseSecondsLeft = 12))
        composeRule.setContent { PauseTheme { PauseScreen(state = state, onIntent = {}) } }

        composeRule.onNodeWithText("Next – available in 12 s").assertIsNotEnabled()

        state = state.copy(pauseSecondsLeft = 0, isPauseFinished = true)

        composeRule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun openIsEnabledOnlyAfterAPurposeAndATimeAreChosen() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    BlockState(
                        isLoading = false,
                        purposes = FrictionConfig.DEFAULT.purposes,
                        accessMinutesOptions = FrictionConfig.DEFAULT.accessMinutes
                    )
                )
            }
            PauseTheme {
                PurposeScreen(
                    state = state,
                    onIntent = { intent ->
                        state = when (intent) {
                            is BlockIntent.PurposeSelected -> state.copy(selectedPurpose = intent.purpose)
                            is BlockIntent.MinutesSelected -> state.copy(selectedMinutes = intent.minutes)
                            else -> state
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithText("Open").assertIsNotEnabled()
        composeRule.onNodeWithText("Work or study").performClick()
        composeRule.onNodeWithText("Open").assertIsNotEnabled()
        composeRule.onNodeWithText("5 min").performClick()
        composeRule.onNodeWithText("Open for 5 min").assertIsEnabled()
    }
}
