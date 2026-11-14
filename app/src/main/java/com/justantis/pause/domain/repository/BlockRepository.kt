package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.AccessGrant
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockEvent
import com.justantis.pause.domain.model.BlockReason
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Attempts, bypasses and time-boxed access grants. An attempt is every time the break screen was
 * shown; a bypass is an attempt that ended with granted access. Counts are per local day.
 */
interface BlockRepository {
    /** Stores a new attempt (outcome CLOSED) and returns its id. */
    suspend fun recordAttempt(packageName: String, reason: BlockReason, focusSessionId: Long?): Long

    /** Turns the attempt [eventId] into a bypass. */
    suspend fun recordBypass(eventId: Long, purpose: AccessPurpose, grantedMinutes: Int, pauseSeconds: Int)

    suspend fun getEvent(eventId: Long): BlockEvent?

    /** All bypasses on [date] across all apps and Focus Mode (assumption A1). */
    suspend fun countBypasses(date: LocalDate): Int

    fun observeBypassCount(date: LocalDate): Flow<Int>

    fun observeAttemptCount(date: LocalDate, packageName: String): Flow<Int>

    /** Events from [from] to [to], both inclusive. */
    fun observeEvents(from: LocalDate, to: LocalDate): Flow<List<BlockEvent>>

    suspend fun grantAccess(packageName: String, expiresAt: Instant)

    /** The grant for [packageName] if it has not expired yet. */
    suspend fun getActiveGrant(packageName: String): AccessGrant?
}
