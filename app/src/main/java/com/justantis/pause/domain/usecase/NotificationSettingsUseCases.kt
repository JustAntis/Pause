package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Turns the "5 minutes before a limit ends" notification on or off.
 */
class SetLimitWarningEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.setNotifyBeforeLimit(enabled)
}

/**
 * Turns the weekly summary on or off; its periodic work is scheduled only while it is on.
 */
class SetWeeklySummaryEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: BackgroundWorkScheduler
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setNotifyWeeklySummary(enabled)
        if (enabled) scheduler.scheduleWeeklySummary() else scheduler.cancelWeeklySummary()
    }
}
