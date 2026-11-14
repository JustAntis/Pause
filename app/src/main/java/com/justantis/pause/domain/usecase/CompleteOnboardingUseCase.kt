package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Stores what the user entered during onboarding and marks it as completed. From then on the app
 * starts on the main screens. The name is optional; a blank name is stored as no name.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(userName: String?, screenTimeGoalMinutes: Int, unlockGoal: Int) {
        settingsRepository.setUserName(userName)
        settingsRepository.setScreenTimeGoalMinutes(screenTimeGoalMinutes)
        settingsRepository.setUnlockGoal(unlockGoal)
        settingsRepository.setOnboardingCompleted(true)
    }
}
