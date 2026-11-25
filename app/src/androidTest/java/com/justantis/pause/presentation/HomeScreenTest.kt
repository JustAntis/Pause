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
import com.justantis.pause.presentation.home.HomeAppItem
import com.justantis.pause.presentation.home.HomeIntent
import com.justantis.pause.presentation.home.HomeScreen
import com.justantis.pause.presentation.home.HomeState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<HomeIntent>()

    private val filled = HomeState(
        date = LocalDate.of(2026, 11, 25),
        isLoading = false,
        userName = "Ann",
        streakDays = 5,
        freezes = 1,
        maxFreezes = 2,
        topApps = listOf(
            HomeAppItem("app.chat", "Chat", usedMinutes = 8, limitMinutes = 10, progress = 0.8f),
            HomeAppItem("app.video", "Video", usedMinutes = 20, limitMinutes = null, progress = 0.3f)
        )
    )

    private fun show(state: HomeState) {
        composeRule.setScreen { HomeScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun loadingShowsNoContent() {
        show(filled.copy(isLoading = true))

        composeRule.onNodeWithText("Hi Ann!").assertDoesNotExist()
        composeRule.onNodeWithText("Start session").assertDoesNotExist()
    }

    @Test
    fun filledStateShowsGreetingStreakAndAppUsage() {
        show(filled)

        composeRule.onNodeWithText("Wednesday, 25 November").assertIsDisplayed()
        composeRule.onNodeWithText("Hi Ann!").assertIsDisplayed()
        composeRule.onNodeWithText("5 days!").assertIsDisplayed()
        composeRule.onNodeWithText("1/2").assertIsDisplayed()
        composeRule.onNodeWithText("8/10 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("20 min · no limit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun eachPermissionWarningShowsOnlyWhenMissingAndOpensItsSettings() {
        var state by mutableStateOf(filled)
        composeRule.setScreen { HomeScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("Usage access is off").assertDoesNotExist()
        composeRule.onNodeWithText("Accessibility service is off").assertDoesNotExist()

        state = filled.copy(usageAccessMissing = true)
        composeRule.onNodeWithText("Usage access is off").assertIsDisplayed()
        composeRule.onNodeWithText("Accessibility service is off").assertDoesNotExist()
        composeRule.onNodeWithText("Open settings").performClick()
        assertEquals(HomeIntent.FixUsageAccess, recorder.last())

        state = filled.copy(accessibilityMissing = true)
        composeRule.onNodeWithText("Usage access is off").assertDoesNotExist()
        composeRule.onNodeWithText("Accessibility service is off").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").performClick()
        assertEquals(HomeIntent.FixAccessibility, recorder.last())
    }

    @Test
    fun tappingAnAppOpensItsDetailsAndNoAppsShowsTheEmptyText() {
        var state by mutableStateOf(filled)
        composeRule.setScreen { HomeScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("No screen time recorded yet today.").assertDoesNotExist()
        composeRule.onNodeWithText("Chat").performScrollTo().performClick()
        assertEquals(HomeIntent.AppClicked("app.chat"), recorder.last())

        state = filled.copy(topApps = emptyList())
        composeRule.onNodeWithText("No screen time recorded yet today.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun startButtonSendsStartAndShowsTimeLeftWhileFocusRuns() {
        var state by mutableStateOf(filled)
        composeRule.setScreen { HomeScreen(state = state, onIntent = recorder) }

        composeRule.onNodeWithText("Start session").performScrollTo().performClick()
        assertEquals(HomeIntent.StartSessionClicked, recorder.last())

        state = filled.copy(focusMinutesLeft = 12)
        composeRule.onNodeWithText("Start session").assertDoesNotExist()
        composeRule.onNodeWithText("Focus · 12 min left").performScrollTo().assertIsDisplayed()
    }
}
