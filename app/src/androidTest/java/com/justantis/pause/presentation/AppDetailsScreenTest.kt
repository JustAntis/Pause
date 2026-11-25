package com.justantis.pause.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.presentation.appdetails.AppDetailsIntent
import com.justantis.pause.presentation.appdetails.AppDetailsScreen
import com.justantis.pause.presentation.appdetails.AppDetailsState
import com.justantis.pause.presentation.limits.ActiveDays
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<AppDetailsIntent>()

    private val state = AppDetailsState(
        isLoading = false,
        packageName = "app.chat",
        label = "Chat",
        todayMinutes = 75,
        opensToday = 7,
        bypassesToday = 2
    )

    private fun show(state: AppDetailsState) {
        composeRule.setScreen { AppDetailsScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun statTilesShowTimeOpensAndBypasses() {
        show(state)

        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("1h 15m").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("opens today").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("bypasses today").assertIsDisplayed()
    }

    @Test
    fun limitCardOffersSetWithoutALimitAndEditWithOne() {
        var current by mutableStateOf(state)
        composeRule.setScreen { AppDetailsScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("No limit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Set").performScrollTo().performClick()
        assertEquals(AppDetailsIntent.LimitClicked, recorder.last())

        current = state.copy(limitMinutes = 45, limitDays = ActiveDays.EveryDay)
        composeRule.onNodeWithText("No limit").assertDoesNotExist()
        composeRule.onNodeWithText("45 min · every day").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Edit").performScrollTo().performClick()
        assertEquals(2, recorder.intents.count { it == AppDetailsIntent.LimitClicked })
    }

    @Test
    fun pauseInFocusSwitchSendsTheNewValue() {
        show(state.copy(pausedInFocus = false))

        composeRule.onNodeWithText("Pause in Focus Mode").performScrollTo().performClick()

        assertEquals(AppDetailsIntent.PausedInFocusChanged(true), recorder.last())
    }

    @Test
    fun backSendsBackClicked() {
        show(state)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(AppDetailsIntent.BackClicked, recorder.last())
    }
}
