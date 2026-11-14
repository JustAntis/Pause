package com.justantis.pause.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class DaysMaskTest {

    @Test
    fun `monday is the lowest bit and sunday the highest`() {
        assertEquals(0b000_0001, DaysMask.fromDays(setOf(DayOfWeek.MONDAY)))
        assertEquals(0b100_0000, DaysMask.fromDays(setOf(DayOfWeek.SUNDAY)))
    }

    @Test
    fun `all days give the every day mask`() {
        assertEquals(DaysMask.EVERY_DAY, DaysMask.fromDays(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `mask converts back to the same days`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        assertEquals(weekdays, DaysMask.toDays(DaysMask.fromDays(weekdays)))
    }

    @Test
    fun `empty mask has no days`() {
        assertEquals(emptySet<DayOfWeek>(), DaysMask.toDays(0))
    }
}
