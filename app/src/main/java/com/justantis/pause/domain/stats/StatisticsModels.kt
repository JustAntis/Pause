package com.justantis.pause.domain.stats

import java.time.LocalDate

enum class StatisticsPeriod { DAY, WEEK, MONTH }

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    val days: List<LocalDate>
        get() = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(endInclusive) }.toList()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

/** Screen time and unlocks of one day. [hasData] is false for days without any recorded usage. */
data class DayTotal(
    val date: LocalDate,
    val screenTimeMs: Long,
    val unlocks: Int,
    val hasData: Boolean,
    val isFuture: Boolean
)

data class AppTotal(val packageName: String, val foregroundMs: Long)

/**
 * Statistics of one period. [changePercent] compares the daily average with the preceding period
 * of the same length and is null when that period has no data. [hourlyScreenTimeMs] is filled for
 * today only.
 */
data class PeriodStatistics(
    val period: StatisticsPeriod,
    val days: List<DayTotal>,
    val hourlyScreenTimeMs: List<Long>,
    val averageScreenTimeMs: Long,
    val changePercent: Int?,
    val averageUnlocks: Int,
    val limitBypasses: Int,
    val apps: List<AppTotal>
)
