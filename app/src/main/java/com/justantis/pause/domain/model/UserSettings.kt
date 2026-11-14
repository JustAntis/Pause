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

        const val SCREEN_TIME_GOAL_STEP_MINUTES = 15
        const val SCREEN_TIME_GOAL_MIN_MINUTES = 30
        const val SCREEN_TIME_GOAL_MAX_MINUTES = 12 * 60

        const val UNLOCK_GOAL_STEP = 5
        const val UNLOCK_GOAL_MIN = 5
        const val UNLOCK_GOAL_MAX = 200
    }
}
