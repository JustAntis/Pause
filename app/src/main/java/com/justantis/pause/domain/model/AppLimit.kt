package com.justantis.pause.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * A daily time limit for one app. The same value applies on every day in [activeDays]
 * (assumption A8); on other days, or when [enabled] is false, the app is not limited.
 */
data class AppLimit(
    val packageName: String,
    val limitMinutes: Int,
    val activeDays: Set<DayOfWeek>,
    val enabled: Boolean,
    val createdAt: Instant
) {
    fun appliesOn(date: LocalDate): Boolean = enabled && date.dayOfWeek in activeDays

    val appliesEveryDay: Boolean
        get() = activeDays.size == DayOfWeek.entries.size

    companion object {
        const val STEP_MINUTES = 5
        const val MIN_MINUTES = 5
        const val MAX_MINUTES = 8 * 60
    }
}
