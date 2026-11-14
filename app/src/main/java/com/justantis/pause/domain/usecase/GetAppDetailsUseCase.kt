package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.repository.AppCatalogRepository
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.LimitRepository
import com.justantis.pause.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class AppDayUsage(val date: LocalDate, val foregroundMs: Long)

/**
 * Everything the App details screen shows about one app. [lastDays] holds the last 7 days ending
 * today; today comes from live usage, earlier days from the stored history.
 */
data class AppDetails(
    val packageName: String,
    val app: InstalledApp?,
    val today: LocalDate,
    val todayUsage: AppUsage?,
    val lastDays: List<AppDayUsage>,
    val limit: AppLimit?,
    val pausedInFocus: Boolean,
    val bypassesToday: Int
)

/**
 * Collects the details of one app: today's time and opens, the last 7 days, its limit, whether it
 * is paused in Focus Mode and how many bypasses it had today (limits and Focus Mode together).
 */
class GetAppDetailsUseCase @Inject constructor(
    private val appCatalogRepository: AppCatalogRepository,
    private val usageRepository: UsageRepository,
    private val limitRepository: LimitRepository,
    private val focusRepository: FocusRepository,
    private val blockRepository: BlockRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(packageName: String): AppDetails {
        val today = LocalDate.now(clock)
        val firstDay = today.minusDays(LAST_DAYS - 1)
        val todayUsage = usageRepository.usageOn(today).usageOf(packageName)
        val stored = usageRepository.observeDailyAppUsage(firstDay, today.minusDays(1)).first()
            .filter { it.packageName == packageName }
            .associate { it.date to it.foregroundMs }

        return AppDetails(
            packageName = packageName,
            app = appCatalogRepository.app(packageName),
            today = today,
            todayUsage = todayUsage,
            lastDays = (0 until LAST_DAYS).map { offset ->
                val date = firstDay.plusDays(offset)
                AppDayUsage(date, if (date == today) todayUsage?.foregroundMs ?: 0L else stored[date] ?: 0L)
            },
            limit = limitRepository.getLimit(packageName),
            pausedInFocus = focusRepository.isFocusApp(packageName),
            bypassesToday = blockRepository.observeEvents(today, today).first()
                .count { it.packageName == packageName && it.outcome == BlockOutcome.BYPASSED }
        )
    }

    private companion object {
        const val LAST_DAYS = 7L
    }
}
