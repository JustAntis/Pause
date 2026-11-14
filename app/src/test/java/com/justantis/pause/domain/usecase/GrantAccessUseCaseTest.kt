package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.testing.FakeBlockRepository
import com.justantis.pause.testing.FakeFocusRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class GrantAccessUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
    private val blocks = FakeBlockRepository(clock)
    private val focus = FakeFocusRepository(clock)
    private val grantAccess = GrantAccessUseCase(blocks, focus, clock)

    @Test
    fun `limit bypass is recorded and access is granted for the chosen minutes`() = runTest {
        val eventId = blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, focusSessionId = null)

        grantAccess(eventId, INSTAGRAM, BlockReason.LIMIT, null, AccessPurpose.REPLY_TO_MESSAGE, minutes = 5, pauseSeconds = 30)

        val event = blocks.getEvent(eventId)!!
        assertEquals(BlockOutcome.BYPASSED, event.outcome)
        assertEquals(AccessPurpose.REPLY_TO_MESSAGE, event.purpose)
        assertEquals(5, event.grantedMinutes)
        assertEquals(30, event.pauseSeconds)
        assertEquals(clock.instant().plusSeconds(300), blocks.getActiveGrant(INSTAGRAM)!!.expiresAt)
    }

    @Test
    fun `focus bypass marks the session as interrupted without ending it`() = runTest {
        val session = focus.startSession(plannedMinutes = 25)
        val eventId = blocks.recordAttempt(INSTAGRAM, BlockReason.FOCUS, session.id)

        grantAccess(eventId, INSTAGRAM, BlockReason.FOCUS, session.id, AccessPurpose.OTHER, minutes = 1, pauseSeconds = 10)

        val running = focus.getRunningSession()!!
        assertEquals(FocusSessionStatus.INTERRUPTED, running.status)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
    }
}
