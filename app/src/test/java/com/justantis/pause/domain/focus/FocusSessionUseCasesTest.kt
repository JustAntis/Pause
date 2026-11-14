package com.justantis.pause.domain.focus

import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.usecase.EndFocusSessionEarlyUseCase
import com.justantis.pause.domain.usecase.FinishExpiredFocusSessionUseCase
import com.justantis.pause.domain.usecase.StartFocusSessionUseCase
import com.justantis.pause.testing.FakeFocusRepository
import com.justantis.pause.testing.FakeWorkScheduler
import com.justantis.pause.testing.TestClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionUseCasesTest {

    private inner class Setup(scope: TestScope) {
        val clock = TestClock(scope.testScheduler, Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
        val focus = FakeFocusRepository(clock)
        val scheduler = FakeWorkScheduler()
        val start = StartFocusSessionUseCase(focus, scheduler)
        val finishExpired = FinishExpiredFocusSessionUseCase(focus, scheduler, clock)
        val endEarly = EndFocusSessionEarlyUseCase(focus, scheduler, clock)
    }

    @Test
    fun `starting saves the paused apps, persists the session and schedules its end`() = runTest {
        val s = Setup(this)

        val session = s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))

        assertEquals(setOf(INSTAGRAM), s.focus.focusApps.value)
        assertEquals(session, s.focus.getRunningSession())
        assertEquals(session.plannedEnd, s.scheduler.focusEndAt)
    }

    @Test
    fun `starting while a session runs returns the running one`() = runTest {
        val s = Setup(this)
        val first = s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))

        val second = s.start(plannedMinutes = 60, pausedApps = setOf(INSTAGRAM))

        assertEquals(first, second)
        assertEquals(1, s.focus.sessions.value.size)
    }

    @Test
    fun `a session is not finished before its planned end`() = runTest {
        val s = Setup(this)
        s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))
        advanceTimeBy(24 * 60_000L)

        s.finishExpired()

        assertEquals(FocusSessionStatus.RUNNING, s.focus.getRunningSession()!!.status)
    }

    @Test
    fun `an expired session without bypasses finishes clean at its planned end`() = runTest {
        val s = Setup(this)
        val session = s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))
        advanceTimeBy(3 * 60 * 60_000L)

        s.finishExpired()

        val finished = s.focus.sessions.value.single()
        assertEquals(FocusSessionStatus.CLEAN, finished.status)
        assertEquals(session.plannedEnd, finished.endedAt)
        assertNull(s.focus.getRunningSession())
    }

    @Test
    fun `an expired session with a bypass finishes with breaks`() = runTest {
        val s = Setup(this)
        val session = s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))
        s.focus.markInterrupted(session.id)
        advanceTimeBy(25 * 60_000L)

        s.finishExpired()

        assertEquals(FocusSessionStatus.INTERRUPTED, s.focus.sessions.value.single().status)
    }

    @Test
    fun `finishing twice changes nothing`() = runTest {
        val s = Setup(this)
        s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))
        advanceTimeBy(25 * 60_000L)
        s.finishExpired()
        val afterFirst = s.focus.sessions.value

        advanceTimeBy(60_000L)
        s.finishExpired()

        assertEquals(afterFirst, s.focus.sessions.value)
    }

    @Test
    fun `ending early cancels the session now and the scheduled end`() = runTest {
        val s = Setup(this)
        s.start(plannedMinutes = 25, pausedApps = setOf(INSTAGRAM))
        advanceTimeBy(10 * 60_000L)

        s.endEarly()

        val ended = s.focus.sessions.value.single()
        assertEquals(FocusSessionStatus.CANCELLED, ended.status)
        assertEquals(s.clock.instant(), ended.endedAt)
        assertNull(s.scheduler.focusEndAt)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
    }
}
