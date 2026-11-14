package com.justantis.pause.domain.stats

import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.repository.WeeklySummary
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * The weekly summary sent on Monday (spec §8): the average daily screen time of last week
 * (Monday–Sunday, days with data), its change against the week before (none without data there)
 * and the current streak. Without any data last week there is nothing to report.
 */
object WeeklySummaryCalculator {

    fun calculate(today: LocalDate, summaries: List<DailySummary>, streakDays: Int): WeeklySummary? {
        val lastWeek = StatisticsRanges.previous(StatisticsPeriod.WEEK, today)
        val weekBefore = DateRange(lastWeek.start.minusWeeks(1), lastWeek.endInclusive.minusWeeks(1))

        val lastWeekAverage = averageMinutes(summaries, lastWeek) ?: return null
        val weekBeforeAverage = averageMinutes(summaries, weekBefore)
        return WeeklySummary(
            averageScreenTimeMinutes = lastWeekAverage,
            changePercent = weekBeforeAverage
                ?.takeIf { it > 0 }
                ?.let { ((lastWeekAverage - it) * 100.0 / it).roundToInt() },
            streakDays = streakDays
        )
    }

    private fun averageMinutes(summaries: List<DailySummary>, range: DateRange): Int? {
        val days = summaries.filter { it.date in range && (it.screenTimeMs > 0 || it.unlockCount > 0) }
        if (days.isEmpty()) return null
        return (days.sumOf { it.screenTimeMs } / days.size / MILLIS_PER_MINUTE).toInt()
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
