package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.streak.StreakState
import com.justantis.pause.testing.FakeNotifier
import com.justantis.pause.testing.FakeSettingsRepository
import com.justantis.pause.testing.FakeStreakRepository
import com.justantis.pause.testing.FakeUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class SendWeeklySummaryUseCaseTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val monday = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(monday.atTime(9, 0).atZone(zone).toInstant(), zone)
    private val usage = object : FakeUsageRepository() {
        override fun observeDailySummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> =
            flowOf(listOf(DailySummary(LocalDate.of(2026, 11, 12), 100 * 60_000L, 30, closed = true, tracked = true)))
    }
    private val notifier = FakeNotifier()

    private fun useCase(enabled: Boolean) = SendWeeklySummaryUseCase(
        usageRepository = usage,
        streakRepository = FakeStreakRepository(StreakState.EMPTY.copy(current = 5)),
        settingsRepository = FakeSettingsRepository(UserSettings(notifyWeeklySummary = enabled)),
        notifier = notifier,
        clock = clock
    )

    @Test
    fun `summary is posted when the setting is on`() = runTest {
        useCase(enabled = true)()

        assertEquals(100, notifier.weeklySummaries.single().averageScreenTimeMinutes)
        assertEquals(5, notifier.weeklySummaries.single().streakDays)
    }

    @Test
    fun `nothing is posted when the setting is off`() = runTest {
        useCase(enabled = false)()

        assertTrue(notifier.weeklySummaries.isEmpty())
    }
}
