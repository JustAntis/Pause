package com.justantis.pause.domain.model

/**
 * User preferences. The two goals are personal targets used only for display and chart
 * highlighting; they do not affect the streak.
 */
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val userName: String? = null,
    val screenTimeGoalMinutes: Int = DEFAULT_SCREEN_TIME_GOAL_MINUTES,
    val unlockGoal: Int = DEFAULT_UNLOCK_GOAL,
    val notifyBeforeLimit: Boolean = true,
    val notifyWeeklySummary: Boolean = false
) {
    companion object {
        const val DEFAULT_SCREEN_TIME_GOAL_MINUTES = 180
        const val DEFAULT_UNLOCK_GOAL = 40
    }
}
