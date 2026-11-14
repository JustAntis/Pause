package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.SettingsRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Stores what the user entered during onboarding and marks it as completed. From then on the app
 * starts on the main screens and the streak counts days from today. The name is optional; a blank
 * name is stored as no name.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(userName: String?, screenTimeGoalMinutes: Int, unlockGoal: Int) {
        settingsRepository.setUserName(userName)
        settingsRepository.setScreenTimeGoalMinutes(screenTimeGoalMinutes)
        settingsRepository.setUnlockGoal(unlockGoal)
        settingsRepository.setOnboardingCompletedOn(LocalDate.now(clock))
        settingsRepository.setOnboardingCompleted(true)
    }
}
