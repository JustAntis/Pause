package com.justantis.pause.presentation.stats

import com.justantis.pause.domain.stats.AppTotal
import com.justantis.pause.domain.stats.DayTotal
import com.justantis.pause.domain.stats.PeriodStatistics
import com.justantis.pause.domain.stats.StatisticsPeriod
import com.justantis.pause.presentation.components.BarStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsStateMapperTest {

    private val saturday = LocalDate.of(2026, 11, 14)

    private fun minutes(value: Int) = value * 60_000L

    private fun day(date: LocalDate, screenMinutes: Int, future: Boolean = false) =
        DayTotal(date, minutes(screenMinutes), 30, hasData = screenMinutes > 0, isFuture = future)

    private fun stats(
        period: StatisticsPeriod,
        days: List<DayTotal> = emptyList(),
        hourly: List<Long> = emptyList(),
        apps: List<AppTotal> = emptyList()
    ) = PeriodStatistics(period, days, hourly, minutes(150), changePercent = -18, averageUnlocks = 41, limitBypasses = 3, apps = apps)

    private fun map(stats: PeriodStatistics, hour: Int = 10) = StatsStateMapper.map(
        stats = stats,
        screenTimeGoalMinutes = 180,
        appLabels = mapOf(INSTAGRAM to "Instagram", TELEGRAM to "Telegram"),
        today = saturday,
        currentHour = hour
    )

    @Test
    fun `day shows 24 hourly bars with the current hour highlighted`() {
        val state = map(stats(StatisticsPeriod.DAY, hourly = List(24) { minutes(it) }), hour = 9)

        assertEquals(24, state.bars.size)
        assertEquals(9, state.bars[9].minutes)
        assertEquals(BarStyle.HIGHLIGHT, state.bars[9].style)
        assertEquals(BarStyle.NORMAL, state.bars[8].style)
    }

    @Test
    fun `days above the goal are warnings and today is highlighted`() {
        val state = map(
            stats(
                StatisticsPeriod.WEEK,
                days = listOf(
                    day(saturday.minusDays(2), 200),
                    day(saturday.minusDays(1), 120),
                    day(saturday, 90),
                    day(saturday.plusDays(1), 0, future = true)
                )
            )
        )

        assertEquals(
            listOf(BarStyle.WARNING, BarStyle.NORMAL, BarStyle.HIGHLIGHT, BarStyle.NORMAL),
            state.bars.map { it.style }
        )
    }

    @Test
    fun `today above the goal is a warning`() {
        val state = map(stats(StatisticsPeriod.WEEK, days = listOf(day(saturday, 200))))

        assertEquals(BarStyle.WARNING, state.bars.single().style)
    }

    @Test
    fun `apps without a launcher entry or under a minute are left out`() {
        val state = map(
            stats(
                StatisticsPeriod.WEEK,
                apps = listOf(
                    AppTotal("com.android.launcher", minutes(300)),
                    AppTotal(TELEGRAM, minutes(100)),
                    AppTotal(INSTAGRAM, 30_000)
                )
            )
        )

        assertEquals(listOf(StatsAppItem(TELEGRAM, "Telegram", 100)), state.apps)
    }

    @Test
    fun `summary values are passed through`() {
        val state = map(stats(StatisticsPeriod.MONTH))

        assertEquals(150, state.averageMinutes)
        assertEquals(-18, state.changePercent)
        assertEquals(41, state.averageUnlocks)
        assertEquals(3, state.limitBypasses)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
    }
}
