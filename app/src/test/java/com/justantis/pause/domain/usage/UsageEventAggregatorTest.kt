package com.justantis.pause.domain.usage

import com.justantis.pause.domain.model.UsageEvent
import com.justantis.pause.domain.model.UsageEventType
import com.justantis.pause.domain.model.UsageEventType.BACKGROUND
import com.justantis.pause.domain.model.UsageEventType.FOREGROUND
import com.justantis.pause.domain.model.UsageEventType.KEYGUARD_SHOWN
import com.justantis.pause.domain.model.UsageEventType.SCREEN_ON
import com.justantis.pause.domain.model.UsageEventType.SCREEN_OFF
import com.justantis.pause.domain.model.UsageEventType.SHUTDOWN
import com.justantis.pause.domain.model.UsageEventType.UNLOCK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageEventAggregatorTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val day = LocalDate.of(2026, 11, 14)
    private val dayStart = at(0, 0)
    private val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun at(hour: Int, minute: Int, date: LocalDate = day): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun event(pkg: String, time: Long, type: UsageEventType) = UsageEvent(pkg, time, type)

    private fun minutes(value: Int): Long = value * 60_000L

    private fun aggregate(events: List<UsageEvent>, start: Long = dayStart, end: Long = dayEnd) =
        UsageEventAggregator.aggregate(events, start, end, zone)

    @Test
    fun `simple session is counted once with its duration`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(10, 0), FOREGROUND),
                event(INSTAGRAM, at(10, 20), BACKGROUND)
            )
        )

        val usage = result.usageOf(INSTAGRAM)!!
        assertEquals(minutes(20), usage.foregroundMs)
        assertEquals(1, usage.openCount)
        assertEquals(minutes(20), result.totalScreenTimeMs)
    }

    @Test
    fun `app still in foreground is counted until the end of the window`() {
        val now = at(12, 30)
        val result = aggregate(listOf(event(INSTAGRAM, at(12, 0), FOREGROUND)), end = now)

        assertEquals(minutes(30), result.usageOf(INSTAGRAM)!!.foregroundMs)
    }

    @Test
    fun `session crossing midnight is split between the two days`() {
        val previousDay = day.minusDays(1)
        val events = listOf(
            event(INSTAGRAM, at(23, 50, previousDay), FOREGROUND),
            event(INSTAGRAM, at(0, 10), BACKGROUND)
        )

        val today = aggregate(events)
        val yesterday = aggregate(events, start = at(0, 0, previousDay), end = dayStart)

        assertEquals(minutes(10), today.usageOf(INSTAGRAM)!!.foregroundMs)
        assertEquals(0, today.usageOf(INSTAGRAM)!!.openCount)
        assertEquals(minutes(10), yesterday.usageOf(INSTAGRAM)!!.foregroundMs)
        assertEquals(1, yesterday.usageOf(INSTAGRAM)!!.openCount)
    }

    @Test
    fun `background event without a matching foreground event adds no time`() {
        val result = aggregate(
            listOf(
                event(SETTINGS, at(20, 58), BACKGROUND),
                event(INSTAGRAM, at(21, 0), FOREGROUND),
                event(INSTAGRAM, at(21, 10), BACKGROUND)
            )
        )

        assertNull(result.usageOf(SETTINGS))
        assertEquals(minutes(10), result.totalScreenTimeMs)
    }

    @Test
    fun `missing background event is closed by screen off`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(10, 0), FOREGROUND),
                event(SYSTEM, at(10, 5), SCREEN_OFF),
                event(INSTAGRAM, at(10, 6), BACKGROUND)
            )
        )

        assertEquals(minutes(5), result.usageOf(INSTAGRAM)!!.foregroundMs)
    }

    @Test
    fun `missing background event is closed by shutdown`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(10, 0), FOREGROUND),
                event(SYSTEM, at(10, 7), SHUTDOWN)
            )
        )

        assertEquals(minutes(7), result.usageOf(INSTAGRAM)!!.foregroundMs)
    }

    @Test
    fun `consecutive resumes of one package are a single session`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(10, 0), FOREGROUND),
                event(INSTAGRAM, at(10, 2), BACKGROUND),
                event(INSTAGRAM, at(10, 2), FOREGROUND),
                event(INSTAGRAM, at(10, 5), FOREGROUND),
                event(INSTAGRAM, at(10, 10), BACKGROUND)
            )
        )

        val usage = result.usageOf(INSTAGRAM)!!
        assertEquals(minutes(10), usage.foregroundMs)
        assertEquals(1, usage.openCount)
    }

    @Test
    fun `switching to another app closes the previous one`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(10, 0), FOREGROUND),
                event(TELEGRAM, at(10, 4), FOREGROUND),
                event(INSTAGRAM, at(10, 4), BACKGROUND),
                event(TELEGRAM, at(10, 10), BACKGROUND)
            )
        )

        assertEquals(minutes(4), result.usageOf(INSTAGRAM)!!.foregroundMs)
        assertEquals(minutes(6), result.usageOf(TELEGRAM)!!.foregroundMs)
        assertEquals(TELEGRAM, result.apps.first().packageName)
        assertEquals(minutes(10), result.totalScreenTimeMs)
    }

    @Test
    fun `unlocks inside the window are counted`() {
        val result = aggregate(
            listOf(
                event(SYSTEM, at(23, 0, day.minusDays(1)), UNLOCK),
                event(SYSTEM, at(8, 0), UNLOCK),
                event(SYSTEM, at(9, 0), UNLOCK),
                event(SYSTEM, at(0, 1, day.plusDays(1)), UNLOCK)
            )
        )

        assertEquals(2, result.unlockCount)
    }

    @Test
    fun `with a lock screen only real unlocks are counted, not screen turn-ons`() {
        val result = aggregate(
            listOf(
                event(SYSTEM, at(8, 0), SCREEN_ON),
                event(SYSTEM, at(8, 0), UNLOCK),
                event(SYSTEM, at(9, 0), SCREEN_ON),
                event(SYSTEM, at(9, 1), KEYGUARD_SHOWN),
                event(SYSTEM, at(10, 0), SCREEN_ON),
                event(SYSTEM, at(10, 0), UNLOCK)
            )
        )

        assertEquals(2, result.unlockCount)
    }

    @Test
    fun `with a lock screen a day without unlocks counts zero even if the screen turned on`() {
        val result = aggregate(
            listOf(
                event(SYSTEM, at(7, 0), KEYGUARD_SHOWN),
                event(SYSTEM, at(8, 0), SCREEN_ON),
                event(SYSTEM, at(9, 0), SCREEN_ON)
            )
        )

        assertEquals(0, result.unlockCount)
    }

    @Test
    fun `without a lock screen screen turn-ons inside the window count as unlocks`() {
        val result = aggregate(
            listOf(
                event(SYSTEM, at(23, 0, day.minusDays(1)), SCREEN_ON),
                event(SYSTEM, at(8, 0), SCREEN_ON),
                event(SYSTEM, at(8, 30), SCREEN_OFF),
                event(SYSTEM, at(21, 36), SCREEN_ON)
            )
        )

        assertEquals(2, result.unlockCount)
    }

    @Test
    fun `lock screen events from before the window decide how unlocks are counted`() {
        val result = aggregate(
            listOf(
                event(SYSTEM, at(22, 0, day.minusDays(1)), KEYGUARD_SHOWN),
                event(SYSTEM, at(8, 0), SCREEN_ON)
            )
        )

        assertEquals(0, result.unlockCount)
    }

    @Test
    fun `interval crossing a full hour is split into two hourly buckets`() {
        val result = aggregate(
            listOf(
                event(INSTAGRAM, at(9, 50), FOREGROUND),
                event(INSTAGRAM, at(10, 15), BACKGROUND)
            )
        )

        assertEquals(24, result.hourlyScreenTimeMs.size)
        assertEquals(minutes(10), result.hourlyScreenTimeMs[9])
        assertEquals(minutes(15), result.hourlyScreenTimeMs[10])
        assertEquals(minutes(25), result.hourlyScreenTimeMs.sum())
    }

    @Test
    fun `no events give an empty summary`() {
        val result = aggregate(emptyList())

        assertEquals(0L, result.totalScreenTimeMs)
        assertEquals(0, result.unlockCount)
        assertNull(result.usageOf(INSTAGRAM))
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
        const val SETTINGS = "com.android.settings"
        const val SYSTEM = "android"
    }
}
