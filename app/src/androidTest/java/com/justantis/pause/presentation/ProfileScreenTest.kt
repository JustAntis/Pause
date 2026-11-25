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
import com.justantis.pause.domain.streak.DayStatus
import com.justantis.pause.presentation.profile.CalendarCell
import com.justantis.pause.presentation.profile.GoalDialog
import com.justantis.pause.presentation.profile.GoalType
import com.justantis.pause.presentation.profile.ProfileIntent
import com.justantis.pause.presentation.profile.ProfileScreen
import com.justantis.pause.presentation.profile.ProfileState
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<ProfileIntent>()

    private val state = ProfileState(
        isLoading = false,
        currentStreak = 5,
        bestStreak = 12,
        freezes = 1,
        maxFreezes = 2,
        daysUntilNextFreeze = 3,
        weeks = listOf(
            listOf(
                CalendarCell(LocalDate.of(2026, 11, 23), DayStatus.CLEAN, isToday = false, isFuture = false),
                CalendarCell(LocalDate.of(2026, 11, 25), DayStatus.IN_PROGRESS, isToday = true, isFuture = false),
                CalendarCell(LocalDate.of(2026, 11, 26), DayStatus.UNTRACKED, isToday = false, isFuture = true)
            )
        )
    )

    private fun show(state: ProfileState) {
        composeRule.setScreen { ProfileScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun streakCardShowsCurrentRecordAndFreezes() {
        var current by mutableStateOf(state)
        composeRule.setScreen { ProfileScreen(state = current, onIntent = recorder) }

        composeRule.onNodeWithText("5 days").assertIsDisplayed()
        composeRule.onNodeWithText("12 days").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("freezes · next in 3 days").assertIsDisplayed()

        current = state.copy(freezes = 2, daysUntilNextFreeze = null)
        composeRule.onNodeWithText("freezes · maximum held").assertIsDisplayed()
    }

    @Test
    fun settingsAndGoalRowsSendTheirIntents() {
        show(state)

        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertEquals(ProfileIntent.SettingsClicked, recorder.last())

        composeRule.onNodeWithText("Daily screen time").performScrollTo().performClick()
        assertEquals(ProfileIntent.EditGoal(GoalType.SCREEN_TIME), recorder.last())

        composeRule.onNodeWithText("Daily unlocks").performScrollTo().performClick()
        assertEquals(ProfileIntent.EditGoal(GoalType.UNLOCKS), recorder.last())
    }

    @Test
    fun goalDialogShowsTheValueAndSendsStepperSaveAndCancel() {
        show(state.copy(goalDialog = GoalDialog(GoalType.UNLOCKS, 40)))

        composeRule.onNodeWithText("40").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        assertEquals(ProfileIntent.GoalDecreased, recorder.last())
        composeRule.onNodeWithContentDescription("Increase").performClick()
        assertEquals(ProfileIntent.GoalIncreased, recorder.last())
        composeRule.onNodeWithText("Save").performClick()
        assertEquals(ProfileIntent.GoalConfirmed, recorder.last())
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(ProfileIntent.GoalDismissed, recorder.last())
    }

    @Test
    fun calendarCellsDescribeTheirDayForScreenReaders() {
        show(state)

        composeRule.onNodeWithContentDescription("Monday, 23 November: Clean").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wednesday, 25 November: In progress").assertExists()
        composeRule.onNodeWithContentDescription("Thursday, 26 November: Not yet").assertExists()
    }
}
