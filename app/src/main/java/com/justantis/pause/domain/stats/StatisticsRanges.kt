package com.justantis.pause.domain.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Date ranges of the Statistics screen (assumption A7): Day = today, Week = the current week from
 * Monday to Sunday, Month = the last 30 days including today. The previous period is the one of the
 * same length right before it.
 */
object StatisticsRanges {

    private const val MONTH_DAYS = 30L

    fun current(period: StatisticsPeriod, today: LocalDate): DateRange = when (period) {
        StatisticsPeriod.DAY -> DateRange(today, today)
        StatisticsPeriod.WEEK -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(monday, monday.plusDays(6))
        }
        StatisticsPeriod.MONTH -> DateRange(today.minusDays(MONTH_DAYS - 1), today)
    }

    fun previous(period: StatisticsPeriod, today: LocalDate): DateRange {
        val current = current(period, today)
        val length = current.days.size.toLong()
        return DateRange(current.start.minusDays(length), current.start.minusDays(1))
    }
}
