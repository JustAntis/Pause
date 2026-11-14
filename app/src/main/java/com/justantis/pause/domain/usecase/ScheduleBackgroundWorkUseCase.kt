package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Makes sure the periodic history sync is scheduled, and the weekly summary when it is turned on.
 * Safe to call on every app start.
 */
class ScheduleBackgroundWorkUseCase @Inject constructor(
    private val scheduler: BackgroundWorkScheduler,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        scheduler.scheduleUsageSync()
        if (settingsRepository.settings.first().notifyWeeklySummary) scheduler.scheduleWeeklySummary()
    }
}
