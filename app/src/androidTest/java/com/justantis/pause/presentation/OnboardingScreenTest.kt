package com.justantis.pause.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.presentation.onboarding.OnboardingIntent
import com.justantis.pause.presentation.onboarding.OnboardingScreen
import com.justantis.pause.presentation.onboarding.OnboardingState
import com.justantis.pause.presentation.onboarding.OnboardingStep
import com.justantis.pause.presentation.testing.IntentRecorder
import com.justantis.pause.presentation.testing.setScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recorder = IntentRecorder<OnboardingIntent>()

    private fun show(state: OnboardingState) {
        composeRule.setScreen { OnboardingScreen(state = state, onIntent = recorder) }
    }

    @Test
    fun welcomeStepAllowsNextAndSendsTheTypedName() {
        show(OnboardingState(step = OnboardingStep.WELCOME))

        composeRule.onNodeWithText("Welcome to Pause").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsEnabled()

        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Ann")

        assertEquals(OnboardingIntent.NameChanged("Ann"), recorder.last())
    }

    @Test
    fun permissionsStepWithoutPermissionsBlocksNextAndSendsGrantIntents() {
        show(OnboardingState(step = OnboardingStep.PERMISSIONS))

        composeRule.onNodeWithText("Next").assertIsNotEnabled()

        composeRule.onAllNodesWithText("Grant")[0].performScrollTo().performClick()
        assertEquals(OnboardingIntent.GrantUsageAccess, recorder.last())

        composeRule.onAllNodesWithText("Grant")[1].performScrollTo().performClick()
        assertEquals(OnboardingIntent.GrantAccessibility, recorder.last())
    }

    @Test
    fun permissionsStepWithBothPermissionsShowsGrantedAndAllowsNext() {
        show(
            OnboardingState(
                step = OnboardingStep.PERMISSIONS,
                usageAccessGranted = true,
                accessibilityGranted = true
            )
        )

        composeRule.onAllNodesWithText("Granted").assertCountEquals(2)
        composeRule.onAllNodesWithText("Grant").assertCountEquals(0)
        composeRule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun goalsStepShowsFinishAndSendsStepperIntents() {
        show(OnboardingState(step = OnboardingStep.GOALS))

        composeRule.onNodeWithText("Finish").assertIsEnabled()
        composeRule.onNodeWithText("3h").assertIsDisplayed()
        composeRule.onNodeWithText("40").assertIsDisplayed()

        val decrease = composeRule.onAllNodesWithContentDescription("Decrease")
        val increase = composeRule.onAllNodesWithContentDescription("Increase")

        decrease[0].performScrollTo().performClick()
        assertEquals(OnboardingIntent.DecreaseScreenTimeGoal, recorder.last())
        increase[0].performScrollTo().performClick()
        assertEquals(OnboardingIntent.IncreaseScreenTimeGoal, recorder.last())
        decrease[1].performScrollTo().performClick()
        assertEquals(OnboardingIntent.DecreaseUnlockGoal, recorder.last())
        increase[1].performScrollTo().performClick()
        assertEquals(OnboardingIntent.IncreaseUnlockGoal, recorder.last())
    }

    @Test
    fun goalAtItsMinimumDisablesDecrease() {
        show(
            OnboardingState(
                step = OnboardingStep.GOALS,
                screenTimeGoalMinutes = UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES
            )
        )

        composeRule.onAllNodesWithContentDescription("Decrease")[0].assertIsNotEnabled()
        composeRule.onAllNodesWithContentDescription("Increase")[0].assertIsEnabled()
        composeRule.onAllNodesWithContentDescription("Decrease")[1].assertIsEnabled()
    }
}
