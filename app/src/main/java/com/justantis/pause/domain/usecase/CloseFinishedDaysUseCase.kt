package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.LimitRepository
import com.justantis.pause.domain.repository.PermissionRepository
import com.justantis.pause.domain.repository.SettingsRepository
import com.justantis.pause.domain.repository.UsageRepository
import com.justantis.pause.domain.streak.DayTracking
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Closes every finished day that is not closed yet (spec §8, day close): its final totals are
 * stored and it is marked tracked or untracked for the streak by [DayTracking]. Days before
 * onboarding are never closed. Running it again changes nothing, so it can run on every app start
 * and from background work.
 *
 * Users who finished onboarding before its date was stored get today as their onboarding date.
 */
class CloseFinishedDaysUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    private val limitRepository: LimitRepository,
    private val permissionRepository: PermissionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        val settings = settingsRepository.settings.first()
        if (!settings.onboardingCompleted) return
        val today = LocalDate.now(clock)
        val onboardingDate = settings.onboardingCompletedOn
            ?: today.also { settingsRepository.setOnboardingCompletedOn(it) }

        val afterLastClosed = usageRepository.lastClosedDay()?.plusDays(1)
        var date = if (afterLastClosed == null) onboardingDate else maxOf(afterLastClosed, onboardingDate)
        if (!date.isBefore(today)) return

        val limits = limitRepository.getLimits()
        val serviceOn = permissionRepository.currentStatus().accessibilityService
        while (date.isBefore(today)) {
            usageRepository.syncDay(date)
            usageRepository.closeDay(date, DayTracking.isTracked(date, onboardingDate, limits, serviceOn, clock.zone))
            date = date.plusDays(1)
        }
    }
}
