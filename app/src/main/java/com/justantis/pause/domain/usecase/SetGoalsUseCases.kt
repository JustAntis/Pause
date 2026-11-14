package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Changes the daily screen-time goal, kept within the allowed range.
 */
class SetScreenTimeGoalUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(minutes: Int) = settingsRepository.setScreenTimeGoalMinutes(
        minutes.coerceIn(UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES, UserSettings.SCREEN_TIME_GOAL_MAX_MINUTES)
    )
}

/**
 * Changes the daily unlock goal, kept within the allowed range.
 */
class SetUnlockGoalUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(unlocks: Int) = settingsRepository.setUnlockGoal(
        unlocks.coerceIn(UserSettings.UNLOCK_GOAL_MIN, UserSettings.UNLOCK_GOAL_MAX)
    )
}
