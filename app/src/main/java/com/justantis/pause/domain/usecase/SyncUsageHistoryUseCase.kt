package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.PermissionRepository
import com.justantis.pause.domain.repository.SettingsRepository
import com.justantis.pause.domain.repository.UsageRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Keeps the stored history up to date (spec §5.1). The system keeps raw events only for a few
 * days, so every run stores yesterday and today. The first run also imports older days, going back
 * from the day before yesterday until the system has no events for a day (at most 30 days).
 * Finished days are then closed.
 */
class SyncUsageHistoryUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
    private val closeFinishedDays: CloseFinishedDaysUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        if (!permissionRepository.currentStatus().usageAccess) return
        val today = LocalDate.now(clock)

        if (!settingsRepository.isHistoryBackfilled()) {
            var date = today.minusDays(2)
            var imported = 0
            while (imported < MAX_BACKFILL_DAYS && usageRepository.syncDay(date)) {
                date = date.minusDays(1)
                imported++
            }
            settingsRepository.setHistoryBackfilled()
        }

        usageRepository.syncDay(today.minusDays(1))
        usageRepository.syncDay(today)
        closeFinishedDays()
    }

    private companion object {
        const val MAX_BACKFILL_DAYS = 30
    }
}
