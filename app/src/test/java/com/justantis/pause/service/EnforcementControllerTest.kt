package com.justantis.pause.service

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.testing.FakeBlockRepository
import com.justantis.pause.testing.FakeFocusRepository
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.FakeNotifier
import com.justantis.pause.testing.FakeSettingsRepository
import com.justantis.pause.testing.FakeUsageRepository
import com.justantis.pause.testing.TestClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EnforcementControllerTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val saturday = LocalDate.of(2026, 11, 14)

    private class FakeLauncher : BlockScreenLauncher {
        val shown = mutableListOf<Shown>()

        data class Shown(val packageName: String, val eventId: Long, val reason: BlockReason, val focusSessionId: Long?)

        override fun showBlockScreen(packageName: String, eventId: Long, reason: BlockReason, focusSessionId: Long?) {
            shown += Shown(packageName, eventId, reason, focusSessionId)
        }
    }

    private inner class Setup(scope: TestScope, startAt: ZonedDateTime) {
        val clock = TestClock(scope.testScheduler, startAt.toInstant(), zone)
        val limits = FakeLimitRepository()
        val focus = FakeFocusRepository(clock)
        val blocks = FakeBlockRepository(clock)
        val usage = FakeUsageRepository()
        val launcher = FakeLauncher()
        val settings = FakeSettingsRepository()
        val notifier = FakeNotifier()
        val controller = EnforcementController(
            limitRepository = limits,
            focusRepository = focus,
            blockRepository = blocks,
            usageRepository = usage,
            settingsRepository = settings,
            notifier = notifier,
            clock = clock,
            scope = scope.backgroundScope
        ).also { it.start(launcher) }

        suspend fun limit(pkg: String, minutes: Int, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
            limits.saveLimit(AppLimit(pkg, minutes, days, enabled = true, createdAt = Instant.EPOCH))

        fun used(pkg: String, minutes: Int, date: LocalDate? = null) {
            val summary = UsageSummary.EMPTY.copy(apps = listOf(AppUsage(pkg, minutes * 60_000L, 1)))
            if (date == null) usage.summary = summary else usage.summaryByDate[date] = summary
        }
    }

    private fun TestScope.setup(startAt: ZonedDateTime = saturday.atTime(10, 0).atZone(zone)) =
        Setup(this, startAt).also { runCurrent() }

    private fun minutes(value: Long) = value * 60_000L

    @Test
    fun `app under its limit is checked again when the remainder is used up`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 40)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        advanceTimeBy(minutes(5) - 1_000)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        s.used(INSTAGRAM, 45)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(BlockReason.LIMIT, s.launcher.shown.single().reason)
        assertEquals(1, s.blocks.events.value.size)
    }

    @Test
    fun `app at its limit is blocked at once`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        val shown = s.launcher.shown.single()
        assertEquals(INSTAGRAM, shown.packageName)
        assertEquals(s.blocks.events.value.single().id, shown.eventId)
    }

    @Test
    fun `active grant allows the app and the flow starts again when it expires`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 50)
        s.blocks.grantAccess(INSTAGRAM, s.clock.instant().plusSeconds(5 * 60))
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        advanceTimeBy(minutes(5) - 1_000)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(BlockReason.LIMIT, s.launcher.shown.single().reason)
    }

    @Test
    fun `limit that does not apply on this weekday does nothing`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45, setOf(DayOfWeek.MONDAY))
        s.used(INSTAGRAM, 90)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        advanceTimeBy(minutes(60))
        runCurrent()

        assertTrue(s.launcher.shown.isEmpty())
    }

    @Test
    fun `focus app during a running session is blocked with the focus reason`() = runTest {
        val s = setup()
        s.focus.setFocusApps(setOf(INSTAGRAM))
        val session = s.focus.startSession(plannedMinutes = 25)
        runCurrent()

        s.controller.onAppInForeground(TELEGRAM)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        val shown = s.launcher.shown.single()
        assertEquals(BlockReason.FOCUS, shown.reason)
        assertEquals(session.id, shown.focusSessionId)
    }

    @Test
    fun `focus session past its planned end no longer blocks`() = runTest {
        val s = setup()
        s.focus.setFocusApps(setOf(INSTAGRAM))
        s.focus.startSession(plannedMinutes = 25)
        advanceTimeBy(minutes(26))
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertTrue(s.launcher.shown.isEmpty())
    }

    @Test
    fun `starting a focus session blocks the focus app already in front`() = runTest {
        val s = setup()
        s.focus.setFocusApps(setOf(INSTAGRAM))
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        s.focus.startSession(plannedMinutes = 25)
        runCurrent()

        assertEquals(BlockReason.FOCUS, s.launcher.shown.single().reason)
    }

    @Test
    fun `time used on the previous day does not count after midnight`() = runTest {
        val s = setup(startAt = saturday.atTime(23, 50).atZone(zone))
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 30, date = saturday)
        s.used(INSTAGRAM, 0, date = saturday.plusDays(1))
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        advanceTimeBy(minutes(20))
        runCurrent()

        assertTrue(s.launcher.shown.isEmpty())
    }

    @Test
    fun `limit applying from the next day blocks after midnight`() = runTest {
        val s = setup(startAt = saturday.atTime(23, 50).atZone(zone))
        s.limit(INSTAGRAM, 5, setOf(DayOfWeek.SUNDAY))
        s.used(INSTAGRAM, 0, date = saturday)
        s.used(INSTAGRAM, 10, date = saturday.plusDays(1))
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        advanceTimeBy(minutes(10) + 1_000)
        runCurrent()
        assertEquals(BlockReason.LIMIT, s.launcher.shown.single().reason)
    }

    @Test
    fun `switching to another app cancels the pending check`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 40)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        s.controller.onAppInForeground(TELEGRAM)
        s.used(INSTAGRAM, 45)
        advanceTimeBy(minutes(10))
        runCurrent()

        assertTrue(s.launcher.shown.isEmpty())
    }

    @Test
    fun `leaving to the home screen cancels the pending check`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 40)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        s.controller.onNoAppInForeground()
        s.used(INSTAGRAM, 45)
        advanceTimeBy(minutes(10))
        runCurrent()

        assertTrue(s.launcher.shown.isEmpty())
    }

    @Test
    fun `repeated events for the app in front do not record a second attempt`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertEquals(1, s.blocks.events.value.size)
    }

    @Test
    fun `the blocked app covering the break screen brings the same break screen back`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertEquals(2, s.launcher.shown.size)
        assertEquals(s.launcher.shown[0], s.launcher.shown[1])
        assertEquals(1, s.blocks.events.value.size)
    }

    @Test
    fun `an app event after the break screen was in front is a new arrival`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        s.controller.onNoAppInForeground()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertEquals(2, s.blocks.events.value.size)
    }

    @Test
    fun `new limit applies to the app already in front`() = runTest {
        val s = setup()
        s.used(INSTAGRAM, 40)
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        assertTrue(s.launcher.shown.isEmpty())

        s.limit(INSTAGRAM, 30)
        runCurrent()

        assertEquals(BlockReason.LIMIT, s.launcher.shown.single().reason)
    }

    @Test
    fun `a change of limits while the break screen is shown does not show it again`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        runCurrent()
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        s.limit(TELEGRAM, 30)
        runCurrent()

        assertEquals(1, s.launcher.shown.size)
    }

    @Test
    fun `a change arriving while an attempt is being recorded does not record a second one`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 45)
        s.blocks.recordDelayMs = 100
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        advanceTimeBy(50)
        s.limit(TELEGRAM, 30)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, s.blocks.events.value.size)
        assertEquals(1, s.launcher.shown.size)
        assertEquals(s.blocks.events.value.single().id, s.launcher.shown.single().eventId)
    }

    @Test
    fun `a warning is shown 5 minutes before the limit ends and the block follows`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 30)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        advanceTimeBy(minutes(10) - 1_000)
        runCurrent()
        assertTrue(s.notifier.limitWarnings.isEmpty())

        s.used(INSTAGRAM, 40)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(INSTAGRAM to 5), s.notifier.limitWarnings)

        s.used(INSTAGRAM, 45)
        advanceTimeBy(minutes(5))
        runCurrent()
        assertEquals(BlockReason.LIMIT, s.launcher.shown.single().reason)
    }

    @Test
    fun `less than 5 minutes left when the app is opened warns at once`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 42)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertEquals(listOf(INSTAGRAM to 3), s.notifier.limitWarnings)
    }

    @Test
    fun `the warning is shown once per app per day`() = runTest {
        val s = setup()
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 42)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()
        s.controller.onAppInForeground(TELEGRAM)
        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertEquals(1, s.notifier.limitWarnings.size)
    }

    @Test
    fun `no warning when the setting is off`() = runTest {
        val s = setup()
        s.settings.setNotifyBeforeLimit(false)
        s.limit(INSTAGRAM, 45)
        s.used(INSTAGRAM, 42)
        runCurrent()

        s.controller.onAppInForeground(INSTAGRAM)
        runCurrent()

        assertTrue(s.notifier.limitWarnings.isEmpty())
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
    }
}
