package com.justantis.pause.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.presentation.components.PickerApp
import com.justantis.pause.presentation.focus.FocusIntent
import com.justantis.pause.presentation.focus.FocusPhase
import com.justantis.pause.presentation.focus.FocusScreen
import com.justantis.pause.presentation.focus.FocusState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<FocusIntent>()

    private val apps = listOf(PickerApp("app.chat", "Chat"), PickerApp("app.video", "Video"))

    private val running = FocusState(
        isLoading = false,
        phase = FocusPhase.RUNNING,
        pausedApps = apps,
        totalSeconds = 25 * 60,
        secondsLeft = 10 * 60
    )

    private fun show(state: FocusState) {
        composeRule.setScreen { FocusScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun idleWithoutAppsCannotStart() {
        show(FocusState(isLoading = false))

        composeRule.onNodeWithText("No apps selected. Choose at least one app to pause.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Start session").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun idleWithAppsSendsDurationAndStart() {
        show(FocusState(isLoading = false, pausedApps = apps))

        composeRule.onNodeWithText("Chat").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("45 min").performScrollTo().performClick()
        assertEquals(FocusIntent.DurationSelected(45), recorder.last())

        composeRule.onNodeWithText("Start session").performScrollTo().assertIsEnabled().performClick()
        assertEquals(FocusIntent.StartClicked, recorder.last())
    }

    @Test
    fun runningShowsTheStatusAndEndSendsEndClicked() {
        show(running)

        composeRule.onNodeWithText("Clean so far").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start session").assertDoesNotExist()

        composeRule.onNodeWithText("End session").performScrollTo().performClick()
        assertEquals(FocusIntent.EndClicked, recorder.last())
    }

    @Test
    fun endDialogConfirmsOrKeepsGoing() {
        show(running.copy(withBreaks = true, isEndDialogVisible = true))

        composeRule.onNodeWithText("End the session early?").assertIsDisplayed()

        composeRule.onNode(hasText("End session") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(FocusIntent.EndConfirmed, recorder.last())

        composeRule.onNodeWithText("Keep going").performClick()
        assertEquals(FocusIntent.EndDismissed, recorder.last())
    }

    @Test
    fun finishedShowsTheResultAndDoneDismissesIt() {
        show(running.copy(phase = FocusPhase.FINISHED, secondsLeft = 0, cleanToday = 1))

        composeRule.onNodeWithText("Clean session. Well done!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Today · 1 clean · 0 with breaks").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Done").performScrollTo().performClick()
        assertEquals(FocusIntent.FinishedDismissed, recorder.last())
    }
}
