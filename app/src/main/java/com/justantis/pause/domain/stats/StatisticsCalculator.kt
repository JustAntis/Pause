package com.justantis.pause.domain.stats

import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.UsageSummary
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Builds the statistics of a period from the stored day history and today's live usage. Today
 * always comes from the live usage; stored values for today are ignored. Averages are taken over the
 * days of the period that have data, so days before the app was installed and future days do not
 * pull them down. Today counts with its usage so far.
 */
object StatisticsCalculator {

    fun calculate(
        period: StatisticsPeriod,
        today: LocalDate,
        summaries: List<DailySummary>,
        todayUsage: UsageSummary,
        appUsage: List<DailyAppUsage>,
        limitBypassDates: List<LocalDate>
    ): PeriodStatistics {
        val summaryByDate = summaries.associateBy { it.date }
        val current = StatisticsRanges.current(period, today)
        val previous = StatisticsRanges.previous(period, today)

        val days = current.days.map { date -> dayTotal(date, today, summaryByDate, todayUsage) }
        val withData = days.filter { it.hasData }
        val previousWithData = previous.days
            .map { date -> dayTotal(date, today, summaryByDate, todayUsage) }
            .filter { it.hasData }

        val average = withData.averageOf { it.screenTimeMs }
        val previousAverage = previousWithData.averageOf { it.screenTimeMs }

        return PeriodStatistics(
            period = period,
            days = days,
            hourlyScreenTimeMs = if (period == StatisticsPeriod.DAY) todayUsage.hourlyScreenTimeMs else emptyList(),
            averageScreenTimeMs = average,
            changePercent = if (previousWithData.isEmpty() || previousAverage == 0L) {
                null
            } else {
                ((average - previousAverage) * 100.0 / previousAverage).roundToInt()
            },
            averageUnlocks = withData.averageOf { it.unlocks.toLong() }.toInt(),
            limitBypasses = limitBypassDates.count { it in current },
            apps = appTotals(current, today, todayUsage, appUsage)
        )
    }

    private fun dayTotal(
        date: LocalDate,
        today: LocalDate,
        summaryByDate: Map<LocalDate, DailySummary>,
        todayUsage: UsageSummary
    ): DayTotal {
        if (date.isAfter(today)) return DayTotal(date, 0, 0, hasData = false, isFuture = true)
        if (date == today) {
            return DayTotal(
                date = date,
                screenTimeMs = todayUsage.totalScreenTimeMs,
                unlocks = todayUsage.unlockCount,
                hasData = todayUsage.totalScreenTimeMs > 0 || todayUsage.unlockCount > 0,
                isFuture = false
            )
        }
        val summary = summaryByDate[date]
        return DayTotal(
            date = date,
            screenTimeMs = summary?.screenTimeMs ?: 0,
            unlocks = summary?.unlockCount ?: 0,
            hasData = summary != null && (summary.screenTimeMs > 0 || summary.unlockCount > 0),
            isFuture = false
        )
    }

    private fun appTotals(
        range: DateRange,
        today: LocalDate,
        todayUsage: UsageSummary,
        appUsage: List<DailyAppUsage>
    ): List<AppTotal> {
        val totals = mutableMapOf<String, Long>()
        appUsage
            .filter { it.date in range && it.date != today }
            .forEach { totals[it.packageName] = (totals[it.packageName] ?: 0L) + it.foregroundMs }
        if (today in range) {
            todayUsage.apps.forEach { totals[it.packageName] = (totals[it.packageName] ?: 0L) + it.foregroundMs }
        }
        return totals.map { (packageName, ms) -> AppTotal(packageName, ms) }.sortedByDescending { it.foregroundMs }
    }

    private fun List<DayTotal>.averageOf(value: (DayTotal) -> Long): Long =
        if (isEmpty()) 0 else sumOf(value) / size
}
