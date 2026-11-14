package com.justantis.pause.domain.usecase

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
import java.time.LocalDate
import java.time.ZoneId

class SyncUsageHistoryUseCaseTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val today = LocalDate.of(2026, 11, 14)
    private val clock = Clock.fixed(today.atTime(9, 0).atZone(zone).toInstant(), zone)

    private val usage = FakeUsageRepository()
    private val settings = FakeSettingsRepository(UserSettings(onboardingCompleted = true, onboardingCompletedOn = today))
    private val permissions = FakePermissionRepository()
    private val closeFinishedDays = CloseFinishedDaysUseCase(usage, settings, FakeLimitRepository(), permissions, clock)
    private val syncUsageHistory = SyncUsageHistoryUseCase(usage, settings, permissions, closeFinishedDays, clock)

    @Test
    fun `first run backfills past days until the system has no more events`() = runTest {
        usage.daysWithEvents = (1L..4L).map { today.minusDays(it) }.toSet()

        syncUsageHistory()

        val backfilled = usage.syncedDays.filter { it.isBefore(today.minusDays(1)) }
        assertEquals((2L..5L).map { today.minusDays(it) }, backfilled)
        assertTrue(settings.historyBackfilled)
    }

    @Test
    fun `backfill stops after 30 days`() = runTest {
        usage.daysWithEvents = (1L..60L).map { today.minusDays(it) }.toSet()

        syncUsageHistory()

        assertEquals(today.minusDays(31), usage.syncedDays.min())
    }

    @Test
    fun `later runs sync only yesterday and today`() = runTest {
        settings.historyBackfilled = true

        syncUsageHistory()

        assertEquals(listOf(today.minusDays(1), today), usage.syncedDays)
    }

    @Test
    fun `nothing happens without usage access`() = runTest {
        permissions.status = PermissionStatus(usageAccess = false, accessibilityService = true, notifications = true)

        syncUsageHistory()

        assertTrue(usage.syncedDays.isEmpty())
    }
}
