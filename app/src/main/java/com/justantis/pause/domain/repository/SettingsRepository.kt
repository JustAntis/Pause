package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * User preferences stored on the device.
 */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setUserName(name: String?)

    suspend fun setScreenTimeGoalMinutes(minutes: Int)

    suspend fun setUnlockGoal(unlocks: Int)

    suspend fun setNotifyBeforeLimit(enabled: Boolean)

    suspend fun setNotifyWeeklySummary(enabled: Boolean)
}
