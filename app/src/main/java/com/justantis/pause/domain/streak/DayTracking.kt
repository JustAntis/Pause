package com.justantis.pause.domain.streak

import com.justantis.pause.domain.model.AppLimit
import java.time.LocalDate
import java.time.ZoneId

/**
 * Whether a finished day is eligible for the streak (spec §4.2, assumption A6). It is untracked
 * when onboarding was not finished yet, when no limit applied on that day, or when the
 * accessibility service was off when the day was closed.
 *
 * Past limits are not stored, so "a limit applied" means a current, enabled limit that was created
 * by the end of that day and is active on its weekday.
 */
object DayTracking {

    fun isTracked(
        date: LocalDate,
        onboardingDate: LocalDate?,
        limits: List<AppLimit>,
        accessibilityServiceOn: Boolean,
        zone: ZoneId
    ): Boolean {
        if (onboardingDate == null || date.isBefore(onboardingDate)) return false
        if (!accessibilityServiceOn) return false
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()
        return limits.any { it.appliesOn(date) && it.createdAt.isBefore(endOfDay) }
    }
}
