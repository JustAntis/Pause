package com.justantis.pause.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.presentation.limits.ActiveDays
import com.justantis.pause.presentation.limits.LimitItem
import com.justantis.pause.presentation.limits.LimitStatus
import com.justantis.pause.presentation.limits.LimitsIntent
import com.justantis.pause.presentation.limits.LimitsScreen
import com.justantis.pause.presentation.limits.LimitsState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LimitsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<LimitsIntent>()

    private val items = listOf(
        LimitItem("app.chat", "Chat", 30, 10, 0.33f, ActiveDays.EveryDay, LimitStatus.Remaining(20)),
        LimitItem("app.video", "Video", 60, 60, 1f, ActiveDays.EveryDay, LimitStatus.UsedUp(3)),
        LimitItem("app.game", "Game", 45, 0, 0f, ActiveDays.Weekends, LimitStatus.NotActiveToday),
        LimitItem("app.news", "News", 20, 5, 0.25f, ActiveDays.Weekdays, LimitStatus.Remaining(15))
    )

    private fun show(state: LimitsState) {
        composeRule.setScreen { LimitsScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun noLimitsShowsTheEmptyCard() {
        show(LimitsState(isLoading = false))

        composeRule.onNodeWithText("No limits yet").assertIsDisplayed()
    }

    @Test
    fun cardsShowTheStatusLineForEachKindOfStatus() {
        show(LimitsState(isLoading = false, items = items))

        composeRule.onNodeWithText("No limits yet").assertDoesNotExist()
        composeRule.onNodeWithText("20 min left").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Used up · 3 attempts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Weekends only · Not active today").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Weekdays only · 15 min left").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingACardOpensThatLimit() {
        show(LimitsState(isLoading = false, items = items))

        composeRule.onNodeWithText("Video").performScrollTo().performClick()

        assertEquals(LimitsIntent.LimitClicked("app.video"), recorder.last())
    }

    @Test
    fun addLimitSendsAddLimitClicked() {
        show(LimitsState(isLoading = false, items = items))

        composeRule.onNodeWithText("Add limit").performScrollTo().performClick()

        assertEquals(LimitsIntent.AddLimitClicked, recorder.last())
    }
}
