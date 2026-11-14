package com.justantis.pause.data.local

import java.time.DayOfWeek

/**
 * Stores a set of weekdays as one integer: bit 0 is Monday, bit 6 is Sunday.
 */
object DaysMask {
    const val EVERY_DAY = 0b111_1111

    fun fromDays(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or bitOf(day) }

    fun toDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and bitOf(it) != 0 }.toSet()

    private fun bitOf(day: DayOfWeek): Int = 1 shl (day.value - 1)
}
