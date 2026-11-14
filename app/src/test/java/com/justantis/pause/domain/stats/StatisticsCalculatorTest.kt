package com.justantis.pause.domain.stats

import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.UsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatisticsCalculatorTest {

    private val saturday = LocalDate.of(2026, 11, 14)

    private fun minutes(value: Int) = value * 60_000L

    private fun summary(date: LocalDate, screenMinutes: Int, unlocks: Int = 40) =
        DailySummary(date, minutes(screenMinutes), unlocks, closed = true, tracked = true)

    private fun live(screenMinutes: Int, unlocks: Int = 10, apps: List<AppUsage> = emptyList()) = UsageSummary(
        apps = apps,
        totalScreenTimeMs = minutes(screenMinutes),
        unlockCount = unlocks,
        hourlyScreenTimeMs = List(24) { if (it == 9) minutes(screenMinutes) else 0L }
    )

    private fun calculate(
        period: StatisticsPeriod,
        summaries: List<DailySummary> = emptyList(),
        today: UsageSummary = live(0, 0),
        appUsage: List<DailyAppUsage> = emptyList(),
        limitBypassDates: List<LocalDate> = emptyList()
    ) = StatisticsCalculator.calculate(period, saturday, summaries, today, appUsage, limitBypassDates)

    @Test
    fun `ranges follow assumption A7`() {
        assertEquals(DateRange(saturday, saturday), StatisticsRanges.current(StatisticsPeriod.DAY, saturday))
        assertEquals(
            DateRange(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15)),
            StatisticsRanges.current(StatisticsPeriod.WEEK, saturday)
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8)),
            StatisticsRanges.previous(StatisticsPeriod.WEEK, saturday)
        )
        assertEquals(DateRange(saturday.minusDays(29), saturday), StatisticsRanges.current(StatisticsPeriod.MONTH, saturday))
        assertEquals(
            DateRange(saturday.minusDays(59), saturday.minusDays(30)),
            StatisticsRanges.previous(StatisticsPeriod.MONTH, saturday)
        )
    }

    @Test
    fun `day shows today, its hours and compares with yesterday`() {
        val result = calculate(
            StatisticsPeriod.DAY,
            summaries = listOf(summary(saturday.minusDays(1), 100)),
            today = live(80, unlocks = 30)
        )

        assertEquals(minutes(80), result.averageScreenTimeMs)
        assertEquals(minutes(80), result.hourlyScreenTimeMs[9])
        assertEquals(-20, result.changePercent)
        assertEquals(30, result.averageUnlocks)
    }

    @Test
    fun `week averages days with data and marks future days`() {
        val result = calculate(
            StatisticsPeriod.WEEK,
            summaries = listOf(summary(LocalDate.of(2026, 11, 12), 120, 50), summary(LocalDate.of(2026, 11, 13), 180, 30)),
            today = live(60, unlocks = 10)
        )

        assertEquals(7, result.days.size)
        assertEquals(minutes(120), result.averageScreenTimeMs)
        assertEquals(30, result.averageUnlocks)
        assertTrue(result.days.last().isFuture)
        assertFalse(result.days.first().hasData)
    }

    @Test
    fun `change is hidden when the previous period has no data`() {
        val result = calculate(StatisticsPeriod.WEEK, today = live(60))

        assertNull(result.changePercent)
    }

    @Test
    fun `change compares the averages of both periods`() {
        val result = calculate(
            StatisticsPeriod.WEEK,
            summaries = listOf(summary(LocalDate.of(2026, 11, 3), 200), summary(LocalDate.of(2026, 11, 13), 100)),
            today = live(0, 0)
        )

        assertEquals(-50, result.changePercent)
    }

    @Test
    fun `month has 30 days and counts limit bypasses inside it`() {
        val result = calculate(
            StatisticsPeriod.MONTH,
            limitBypassDates = listOf(saturday, saturday.minusDays(5), saturday.minusDays(29))
        )

        assertEquals(30, result.days.size)
        assertEquals(3, result.limitBypasses)
    }

    @Test
    fun `apps are summed over stored days and today, longest first`() {
        val result = calculate(
            StatisticsPeriod.WEEK,
            today = live(30, apps = listOf(AppUsage(INSTAGRAM, minutes(30), 2))),
            appUsage = listOf(
                DailyAppUsage(LocalDate.of(2026, 11, 13), INSTAGRAM, minutes(20), 3),
                DailyAppUsage(LocalDate.of(2026, 11, 13), TELEGRAM, minutes(60), 3),
                DailyAppUsage(saturday, TELEGRAM, minutes(999), 3)
            )
        )

        assertEquals(listOf(TELEGRAM, INSTAGRAM), result.apps.map { it.packageName })
        assertEquals(minutes(60), result.apps[0].foregroundMs)
        assertEquals(minutes(50), result.apps[1].foregroundMs)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
    }
}
