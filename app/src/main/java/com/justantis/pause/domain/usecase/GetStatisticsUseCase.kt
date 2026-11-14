package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.UsageRepository
import com.justantis.pause.domain.stats.PeriodStatistics
import com.justantis.pause.domain.stats.StatisticsCalculator
import com.justantis.pause.domain.stats.StatisticsPeriod
import com.justantis.pause.domain.stats.StatisticsRanges
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Statistics of a period ending today: stored past days plus today's live usage. "Limit bypasses"
 * counts bypasses of app limits only, not Focus Mode.
 */
class GetStatisticsUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val blockRepository: BlockRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(period: StatisticsPeriod): PeriodStatistics {
        val today = LocalDate.now(clock)
        val current = StatisticsRanges.current(period, today)
        val previous = StatisticsRanges.previous(period, today)

        val limitBypassDates = blockRepository.observeEvents(current.start, current.endInclusive).first()
            .filter { it.reason == BlockReason.LIMIT && it.outcome == BlockOutcome.BYPASSED }
            .map { it.date }

        return StatisticsCalculator.calculate(
            period = period,
            today = today,
            summaries = usageRepository.observeDailySummaries(previous.start, current.endInclusive).first(),
            todayUsage = usageRepository.usageOn(today),
            appUsage = usageRepository.observeDailyAppUsage(current.start, current.endInclusive).first(),
            limitBypassDates = limitBypassDates
        )
    }
}
