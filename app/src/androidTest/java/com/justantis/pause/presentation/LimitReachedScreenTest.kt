package com.justantis.pause.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.presentation.block.BlockIntent
import com.justantis.pause.presentation.block.BlockState
import com.justantis.pause.presentation.block.LimitReachedScreen
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LimitReachedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<BlockIntent>()

    private val state = BlockState(
        isLoading = false,
        packageName = "app.chat",
        appLabel = "Chat",
        reason = BlockReason.LIMIT,
        limitMinutes = 30,
        usedMinutes = 34,
        attemptsToday = 3,
        streakDays = 4,
        freezes = 1,
        maxFreezes = 2,
        pauseSeconds = 30
    )

    @Test
    fun showsLimitUsageAttemptsStreakAndFreezesOrTheFocusText() {
        var current by mutableStateOf(state)
        composeRule.setScreen { LimitReachedScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("30 of 30 min").assertIsDisplayed()
        composeRule.onNodeWithText("3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("attempts today").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4 days").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("The next access will be preceded by a 30 s pause").performScrollTo().assertIsDisplayed()

        current = state.copy(reason = BlockReason.FOCUS, focusMinutesLeft = 20)
        composeRule.onNodeWithText("Focus session in progress · 20 min left").assertIsDisplayed()
        composeRule.onNodeWithText("30 of 30 min").assertDoesNotExist()
    }

    @Test
    fun closeAndNeedAccessSendTheirIntentsAndAccessWaitsForLoading() {
        var current by mutableStateOf(state.copy(isLoading = true))
        composeRule.setScreen { LimitReachedScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("I need access").assertIsNotEnabled()
        composeRule.onNodeWithText("Close").performClick()
        assertEquals(BlockIntent.CloseClicked, recorder.last())

        current = state
        composeRule.onNodeWithText("I need access").assertIsEnabled().performClick()
        assertEquals(BlockIntent.NeedAccessClicked, recorder.last())
    }
}
