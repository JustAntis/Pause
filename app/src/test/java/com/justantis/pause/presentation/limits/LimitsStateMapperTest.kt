package com.justantis.pause.presentation.limits

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.UsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate

class LimitsStateMapperTest {

    private val saturday = LocalDate.of(2026, 11, 14)
    private val everyDay = DayOfWeek.entries.toSet()
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    private fun limit(pkg: String, minutes: Int, days: Set<DayOfWeek> = everyDay, createdAt: Long = 0) =
        AppLimit(pkg, minutes, days, enabled = true, createdAt = Instant.ofEpochMilli(createdAt))

    private fun usage(vararg apps: Pair<String, Int>) = UsageSummary.EMPTY.copy(
        apps = apps.map { (pkg, minutes) -> AppUsage(pkg, minutes * 60_000L, 1) }
    )

    private fun map(
        limits: List<AppLimit>,
        usage: UsageSummary = UsageSummary.EMPTY,
        attempts: Map<String, Int> = emptyMap()
    ) = LimitsStateMapper.map(
        limits = limits,
        snapshot = LimitsSnapshot(saturday, usage, mapOf(INSTAGRAM to "Instagram")),
        attemptsByApp = attempts
    )

    @Test
    fun `limit under its value shows the minutes left`() {
        val item = map(listOf(limit(INSTAGRAM, 45)), usage(INSTAGRAM to 17)).items.single()

        assertEquals(LimitStatus.Remaining(28), item.status)
        assertEquals(17, item.usedMinutes)
        assertEquals(17f / 45f, item.progress)
    }

    @Test
    fun `used up limit shows the attempts of this app`() {
        val item = map(
            limits = listOf(limit(INSTAGRAM, 45)),
            usage = usage(INSTAGRAM to 50),
            attempts = mapOf(INSTAGRAM to 4, TELEGRAM to 9)
        ).items.single()

        assertEquals(LimitStatus.UsedUp(4), item.status)
        assertEquals(1f, item.progress)
    }

    @Test
    fun `limit not applying today is not active and has no progress`() {
        val item = map(listOf(limit(INSTAGRAM, 45, weekdays)), usage(INSTAGRAM to 50)).items.single()

        assertEquals(LimitStatus.NotActiveToday, item.status)
        assertEquals(0f, item.progress)
    }

    @Test
    fun `active days are described`() {
        val items = map(
            listOf(
                limit("a", 30, everyDay, createdAt = 1),
                limit("b", 30, weekdays, createdAt = 2),
                limit("c", 30, setOf(SATURDAY, SUNDAY), createdAt = 3),
                limit("d", 30, setOf(FRIDAY, MONDAY), createdAt = 4),
                limit("e", 30, everyDay - SATURDAY, createdAt = 5)
            )
        ).items

        assertEquals(ActiveDays.EveryDay, items[0].activeDays)
        assertEquals(ActiveDays.Weekdays, items[1].activeDays)
        assertEquals(ActiveDays.Weekends, items[2].activeDays)
        assertEquals(ActiveDays.Custom(listOf(MONDAY, FRIDAY)), items[3].activeDays)
        assertEquals(ActiveDays.Except(listOf(SATURDAY)), items[4].activeDays)
    }

    @Test
    fun `label falls back to the package name of an app that is not installed`() {
        val item = map(listOf(limit(TELEGRAM, 30))).items.single()

        assertEquals(TELEGRAM, item.label)
    }

    @Test
    fun `no limits give an empty state`() {
        val state = map(emptyList())

        assertTrue(state.isEmpty)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
    }
}
