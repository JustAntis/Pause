package com.justantis.pause.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.presentation.stats.StatsAppItem
import com.justantis.pause.presentation.stats.StatsIntent
import com.justantis.pause.presentation.stats.StatsPeriod
import com.justantis.pause.presentation.stats.StatsScreen
import com.justantis.pause.presentation.stats.StatsState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<StatsIntent>()

    private val week = StatsState(
        isLoading = false,
        period = StatsPeriod.Week,
        averageMinutes = 95,
        apps = listOf(StatsAppItem("app.chat", "Chat", 50))
    )

    @Test
    fun choosingAPeriodSendsPeriodSelected() {
        composeRule.setScreen { StatsScreen(state = week, onIntent = recorder) }

        composeRule.onNodeWithText("Month").performClick()
        assertEquals(StatsIntent.PeriodSelected(StatsPeriod.Month), recorder.last())

        composeRule.onNodeWithText("Day").performClick()
        assertEquals(StatsIntent.PeriodSelected(StatsPeriod.Day), recorder.last())
    }

    @Test
    fun dayShowsTodayAndWeekShowsDailyAverage() {
        var state by mutableStateOf(week)
        composeRule.setScreen { StatsScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("Daily average").assertIsDisplayed()
        composeRule.onNodeWithText("1h 35m").assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertDoesNotExist()

        state = week.copy(period = StatsPeriod.Day)
        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Daily average").assertDoesNotExist()
    }

    @Test
    fun changeLineReadsUpDownOrNone() {
        var state by mutableStateOf(week.copy(changePercent = 12))
        composeRule.setScreen { StatsScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("+12% vs previous").assertIsDisplayed()

        state = week.copy(changePercent = -8)
        composeRule.onNodeWithText("−8% vs previous").assertIsDisplayed()

        state = week.copy(changePercent = 0)
        composeRule.onNodeWithText("0% vs previous").assertIsDisplayed()
    }

    @Test
    fun noAppsShowsTheEmptyTextAndTappingAnAppOpensIt() {
        var state by mutableStateOf(week.copy(apps = emptyList()))
        composeRule.setScreen { StatsScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("No app usage in this period.").performScrollTo().assertIsDisplayed()

        state = week
        composeRule.onNodeWithText("No app usage in this period.").assertDoesNotExist()
        composeRule.onNodeWithText("Chat").performScrollTo().performClick()
        assertEquals(StatsIntent.AppClicked("app.chat"), recorder.last())
    }
}
