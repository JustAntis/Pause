package com.justantis.pause.domain.streak

import com.justantis.pause.domain.model.AppLimit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DayTrackingTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val friday = LocalDate.of(2026, 11, 13)
    private val onboarding = LocalDate.of(2026, 11, 1)

    private fun limit(days: Set<DayOfWeek> = DayOfWeek.entries.toSet(), createdOn: LocalDate = onboarding, enabled: Boolean = true) =
        AppLimit("app", 30, days, enabled, createdOn.atTime(12, 0).atZone(zone).toInstant())

    private fun tracked(
        date: LocalDate = friday,
        onboardingDate: LocalDate? = onboarding,
        limits: List<AppLimit> = listOf(limit()),
        serviceOn: Boolean = true
    ) = DayTracking.isTracked(date, onboardingDate, limits, serviceOn, zone)

    @Test
    fun `day with an applying limit after onboarding with the service on is tracked`() {
        assertTrue(tracked())
    }

    @Test
    fun `day before onboarding or without onboarding is untracked`() {
        assertFalse(tracked(date = onboarding.minusDays(1)))
        assertFalse(tracked(onboardingDate = null))
    }

    @Test
    fun `day without any limit is untracked`() {
        assertFalse(tracked(limits = emptyList()))
    }

    @Test
    fun `limit that does not apply on that weekday or is disabled does not count`() {
        assertFalse(tracked(limits = listOf(limit(days = setOf(DayOfWeek.MONDAY)))))
        assertFalse(tracked(limits = listOf(limit(enabled = false))))
    }

    @Test
    fun `limit created after the day does not count, created during the day does`() {
        assertFalse(tracked(limits = listOf(limit(createdOn = friday.plusDays(1)))))
        assertTrue(tracked(limits = listOf(limit(createdOn = friday))))
    }

    @Test
    fun `day closed with the accessibility service off is untracked`() {
        assertFalse(tracked(serviceOn = false))
    }
}
