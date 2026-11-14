package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.Notifier
import com.justantis.pause.domain.repository.SettingsRepository
import com.justantis.pause.domain.repository.StreakRepository
import com.justantis.pause.domain.repository.UsageRepository
import com.justantis.pause.domain.stats.StatisticsPeriod
import com.justantis.pause.domain.stats.StatisticsRanges
import com.justantis.pause.domain.stats.WeeklySummaryCalculator
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Posts the weekly summary notification if the user turned it on and last week has data.
 */
class SendWeeklySummaryUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: Notifier,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        if (!settingsRepository.settings.first().notifyWeeklySummary) return
        val today = LocalDate.now(clock)
        val lastWeek = StatisticsRanges.previous(StatisticsPeriod.WEEK, today)
        val summaries = usageRepository.observeDailySummaries(lastWeek.start.minusWeeks(1), lastWeek.endInclusive).first()
        val streak = streakRepository.observeStreak(today).first().current
        WeeklySummaryCalculator.calculate(today, summaries, streak)?.let { notifier.showWeeklySummary(it) }
    }
}
