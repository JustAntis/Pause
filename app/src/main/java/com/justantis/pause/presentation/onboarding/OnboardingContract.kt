package com.justantis.pause.presentation.onboarding

import com.justantis.pause.domain.model.UserSettings

enum class OnboardingStep { WELCOME, PERMISSIONS, GOALS }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val userName: String = "",
    val usageAccessGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val screenTimeGoalMinutes: Int = UserSettings.DEFAULT_SCREEN_TIME_GOAL_MINUTES,
    val unlockGoal: Int = UserSettings.DEFAULT_UNLOCK_GOAL,
    val isSaving: Boolean = false
) {
    val progress: Float
        get() = (step.ordinal + 1f) / OnboardingStep.entries.size

    val isLastStep: Boolean
        get() = step == OnboardingStep.entries.last()

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.PERMISSIONS -> usageAccessGranted && accessibilityGranted
            else -> !isSaving
        }

    val canDecreaseScreenTimeGoal: Boolean
        get() = screenTimeGoalMinutes > UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES

    val canIncreaseScreenTimeGoal: Boolean
        get() = screenTimeGoalMinutes < UserSettings.SCREEN_TIME_GOAL_MAX_MINUTES

    val canDecreaseUnlockGoal: Boolean
        get() = unlockGoal > UserSettings.UNLOCK_GOAL_MIN

    val canIncreaseUnlockGoal: Boolean
        get() = unlockGoal < UserSettings.UNLOCK_GOAL_MAX
}

sealed interface OnboardingIntent {
    data class NameChanged(val name: String) : OnboardingIntent
    data object Next : OnboardingIntent
    data object Back : OnboardingIntent
    data object RefreshPermissions : OnboardingIntent
    data object GrantUsageAccess : OnboardingIntent
    data object GrantAccessibility : OnboardingIntent
    data object DecreaseScreenTimeGoal : OnboardingIntent
    data object IncreaseScreenTimeGoal : OnboardingIntent
    data object DecreaseUnlockGoal : OnboardingIntent
    data object IncreaseUnlockGoal : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object OpenUsageAccessSettings : OnboardingEffect
    data object OpenAccessibilitySettings : OnboardingEffect
    data object Finished : OnboardingEffect
}
