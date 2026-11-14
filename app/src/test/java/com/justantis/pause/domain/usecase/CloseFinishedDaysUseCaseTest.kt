package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.FakePermissionRepository
import com.justantis.pause.testing.FakeSettingsRepository
import com.justantis.pause.testing.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class CloseFinishedDaysUseCaseTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val today = LocalDate.of(2026, 11, 14)
    private val clock = Clock.fixed(today.atTime(9, 0).atZone(zone).toInstant(), zone)
    private val onboarding = today.minusDays(3)

    private val usage = FakeUsageRepository()
    private val settings = FakeSettingsRepository(UserSettings(onboardingCompleted = true, onboardingCompletedOn = onboarding))
    private val limits = FakeLimitRepository(
        listOf(
            AppLimit(
                "app",
                30,
                DayOfWeek.entries.toSet(),
                enabled = true,
                createdAt = onboarding.atTime(12, 0).atZone(zone).toInstant()
            )
        )
    )
    private val permissions = FakePermissionRepository()

    private val closeFinishedDays = CloseFinishedDaysUseCase(usage, settings, limits, permissions, clock)

    @Test
    fun `every finished day since onboarding is synced and closed as tracked`() = runTest {
        closeFinishedDays()

        val expected = listOf(onboarding, onboarding.plusDays(1), onboarding.plusDays(2))
        assertEquals(expected, usage.syncedDays)
        assertEquals(expected.associateWith { true }, usage.closedDays)
    }

    @Test
    fun `running again closes nothing new`() = runTest {
        closeFinishedDays()
        usage.syncedDays.clear()

        closeFinishedDays()

        assertTrue(usage.syncedDays.isEmpty())
    }

    @Test
    fun `closing continues after the last closed day`() = runTest {
        usage.closeDay(today.minusDays(2), tracked = true)

        closeFinishedDays()

        assertEquals(listOf(today.minusDays(1)), usage.syncedDays)
    }

    @Test
    fun `days closed with the accessibility service off are untracked`() = runTest {
        permissions.status = PermissionStatus(usageAccess = true, accessibilityService = false, notifications = false)

        closeFinishedDays()

        assertTrue(usage.closedDays.values.none { it })
    }

    @Test
    fun `nothing is closed before onboarding is finished`() = runTest {
        settings.settings.value = UserSettings()

        closeFinishedDays()

        assertTrue(usage.closedDays.isEmpty())
    }

    @Test
    fun `onboarded user without a stored date starts counting from today`() = runTest {
        settings.settings.value = UserSettings(onboardingCompleted = true, onboardingCompletedOn = null)

        closeFinishedDays()

        assertEquals(today, settings.settings.value.onboardingCompletedOn)
        assertTrue(usage.closedDays.isEmpty())
    }
}
