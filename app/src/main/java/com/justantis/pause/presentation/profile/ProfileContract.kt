package com.justantis.pause.presentation.profile

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.streak.DayStatus
import com.justantis.pause.domain.streak.StreakConfig
import java.time.LocalDate

data class CalendarCell(
    val date: LocalDate,
    val status: DayStatus,
    val isToday: Boolean,
    val isFuture: Boolean
)

enum class GoalType { SCREEN_TIME, UNLOCKS }

data class GoalDialog(val type: GoalType, val value: Int) {
    val canDecrease: Boolean
        get() = when (type) {
            GoalType.SCREEN_TIME -> value > UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES
            GoalType.UNLOCKS -> value > UserSettings.UNLOCK_GOAL_MIN
        }

    val canIncrease: Boolean
        get() = when (type) {
            GoalType.SCREEN_TIME -> value < UserSettings.SCREEN_TIME_GOAL_MAX_MINUTES
            GoalType.UNLOCKS -> value < UserSettings.UNLOCK_GOAL_MAX
        }
}

data class ProfileState(
    val isLoading: Boolean = true,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val freezes: Int = 0,
    val maxFreezes: Int = StreakConfig.MAX_FREEZES,
    val daysUntilNextFreeze: Int? = null,
    val weeks: List<List<CalendarCell>> = emptyList(),
    val screenTimeGoalMinutes: Int = UserSettings.DEFAULT_SCREEN_TIME_GOAL_MINUTES,
    val unlockGoal: Int = UserSettings.DEFAULT_UNLOCK_GOAL,
    val goalDialog: GoalDialog? = null
)

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data object SettingsClicked : ProfileIntent
    data class EditGoal(val type: GoalType) : ProfileIntent
    data object GoalDecreased : ProfileIntent
    data object GoalIncreased : ProfileIntent
    data object GoalConfirmed : ProfileIntent
    data object GoalDismissed : ProfileIntent
}

sealed interface ProfileEffect {
    data object OpenSettings : ProfileEffect
}
