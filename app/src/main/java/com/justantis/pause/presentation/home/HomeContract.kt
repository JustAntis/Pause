package com.justantis.pause.presentation.home

import com.justantis.pause.domain.model.UserSettings
import java.time.LocalDate

enum class TimeOfDay { MORNING, AFTERNOON, EVENING }

data class HomeAppItem(
    val packageName: String,
    val label: String,
    val usedMinutes: Int,
    val limitMinutes: Int?,
    val progress: Float
) {
    val isOverLimit: Boolean
        get() = limitMinutes != null && usedMinutes >= limitMinutes
}

data class HomeState(
    val date: LocalDate,
    val isLoading: Boolean = true,
    val userName: String? = null,
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val streakDays: Int = 0,
    val freezes: Int = 0,
    val maxFreezes: Int = 2,
    val screenTimeMinutes: Int = 0,
    val screenTimeGoalMinutes: Int = UserSettings.DEFAULT_SCREEN_TIME_GOAL_MINUTES,
    val unlocks: Int = 0,
    val attemptsToday: Int = 0,
    val topApps: List<HomeAppItem> = emptyList(),
    val usageAccessMissing: Boolean = false,
    val accessibilityMissing: Boolean = false,
    val focusMinutesLeft: Int? = null
) {
    val screenTimeProgress: Float
        get() = if (screenTimeGoalMinutes > 0) {
            (screenTimeMinutes.toFloat() / screenTimeGoalMinutes).coerceAtMost(1f)
        } else {
            1f
        }

    val isOverGoal: Boolean
        get() = screenTimeMinutes > screenTimeGoalMinutes
}

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class AppClicked(val packageName: String) : HomeIntent
    data object StartSessionClicked : HomeIntent
    data object FixUsageAccess : HomeIntent
    data object FixAccessibility : HomeIntent
}

sealed interface HomeEffect {
    data class OpenAppDetails(val packageName: String) : HomeEffect
    data object OpenFocus : HomeEffect
    data object OpenUsageAccessSettings : HomeEffect
    data object OpenAccessibilitySettings : HomeEffect
}
