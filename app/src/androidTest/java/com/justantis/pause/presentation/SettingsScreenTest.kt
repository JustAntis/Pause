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
import com.justantis.pause.presentation.settings.SettingsIntent
import com.justantis.pause.presentation.settings.SettingsScreen
import com.justantis.pause.presentation.settings.SettingsState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<SettingsIntent>()

    private val state = SettingsState(
        isLoading = false,
        notifyBeforeLimit = true,
        notifyWeeklySummary = false
    )

    @Test
    fun switchesSendTheNewValueAndBlockedNotificationsAreExplained() {
        var current by mutableStateOf(state)
        composeRule.setScreen { SettingsScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("Notifications are turned off for Pause in system settings, so none will be shown.")
            .assertDoesNotExist()

        composeRule.onNodeWithText("5 min before a limit ends").performScrollTo().performClick()
        assertEquals(SettingsIntent.LimitWarningToggled(false), recorder.last())

        composeRule.onNodeWithText("Weekly summary").performScrollTo().performClick()
        assertEquals(SettingsIntent.WeeklySummaryToggled(true), recorder.last())

        current = state.copy(notificationsAllowed = false)
        composeRule.onNodeWithText("Notifications are turned off for Pause in system settings, so none will be shown.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun nameRowOpensTheDialogAndSaveConfirmsIt() {
        var current by mutableStateOf(state)
        composeRule.setScreen { SettingsScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("Not set").performScrollTo().performClick()
        assertEquals(SettingsIntent.EditNameClicked, recorder.last())

        current = state.copy(nameDraft = "Ann")
        composeRule.onNodeWithText("Save").performClick()
        assertEquals(SettingsIntent.NameConfirmed, recorder.last())
    }

    @Test
    fun deleteAllDataAsksForConfirmation() {
        var current by mutableStateOf(state)
        composeRule.setScreen { SettingsScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("Delete all data").performScrollTo().performClick()
        assertEquals(SettingsIntent.DeleteClicked, recorder.last())

        current = state.copy(isDeleteDialogVisible = true)
        composeRule.onNodeWithText("Delete all data?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals(SettingsIntent.DeleteConfirmed, recorder.last())
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(SettingsIntent.DeleteDismissed, recorder.last())
    }
}
