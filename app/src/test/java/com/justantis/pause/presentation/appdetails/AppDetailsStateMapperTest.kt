package com.justantis.pause.presentation.appdetails

import com.justantis.pause.domain.model.AppCategory
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.usecase.AppDayUsage
import com.justantis.pause.domain.usecase.AppDetails
import com.justantis.pause.presentation.components.BarStyle
import com.justantis.pause.presentation.limits.ActiveDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class AppDetailsStateMapperTest {

    private val saturday = LocalDate.of(2026, 11, 14)

    private fun minutes(value: Int) = value * 60_000L

    private fun details(
        limit: AppLimit? = null,
        lastDays: List<Int> = listOf(10, 50, 20, 45, 0, 30, 12),
        paused: Boolean = false
    ) = AppDetails(
        packageName = INSTAGRAM,
        app = InstalledApp(INSTAGRAM, "Instagram", AppCategory.SOCIAL),
        today = saturday,
        todayUsage = AppUsage(INSTAGRAM, minutes(12), 7),
        lastDays = lastDays.mapIndexed { index, value -> AppDayUsage(saturday.minusDays(6L - index), minutes(value)) },
        limit = limit,
        pausedInFocus = paused,
        bypassesToday = 2
    )

    private fun limit(minutes: Int, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        AppLimit(INSTAGRAM, minutes, days, enabled = true, createdAt = Instant.EPOCH)

    @Test
    fun `tiles show today's time, opens and bypasses`() {
        val state = AppDetailsStateMapper.map(details())

        assertEquals("Instagram", state.label)
        assertEquals(AppCategory.SOCIAL, state.category)
        assertEquals(12, state.todayMinutes)
        assertEquals(7, state.opensToday)
        assertEquals(2, state.bypassesToday)
    }

    @Test
    fun `days where the limit was reached are warnings and today is highlighted`() {
        val state = AppDetailsStateMapper.map(details(limit = limit(45)))

        assertEquals(
            listOf(
                BarStyle.NORMAL, BarStyle.WARNING, BarStyle.NORMAL, BarStyle.WARNING,
                BarStyle.NORMAL, BarStyle.NORMAL, BarStyle.HIGHLIGHT
            ),
            state.bars.map { it.style }
        )
    }

    @Test
    fun `limit only counts on its active days`() {
        val onlySaturday = limit(15, setOf(DayOfWeek.SATURDAY))

        val state = AppDetailsStateMapper.map(details(limit = onlySaturday, lastDays = listOf(10, 50, 20, 45, 0, 30, 20)))

        assertTrue(state.bars.dropLast(1).none { it.style == BarStyle.WARNING })
        assertEquals(BarStyle.WARNING, state.bars.last().style)
    }

    @Test
    fun `limit row describes the limit and its days`() {
        val state = AppDetailsStateMapper.map(details(limit = limit(45)))

        assertEquals(45, state.limitMinutes)
        assertEquals(ActiveDays.EveryDay, state.limitDays)
    }

    @Test
    fun `app without a limit has no limit row values`() {
        val state = AppDetailsStateMapper.map(details(paused = true))

        assertNull(state.limitMinutes)
        assertNull(state.limitDays)
        assertTrue(state.pausedInFocus)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
    }
}
