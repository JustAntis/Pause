package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Apps paused during Focus Mode and the history of Focus Mode sessions. A session is persisted
 * as soon as it starts so that it survives process death.
 */
interface FocusRepository {
    fun observeFocusApps(): Flow<Set<String>>

    suspend fun isFocusApp(packageName: String): Boolean

    suspend fun setFocusApp(packageName: String, paused: Boolean)

    suspend fun setFocusApps(packageNames: Set<String>)

    suspend fun startSession(plannedMinutes: Int): FocusSession

    suspend fun getRunningSession(): FocusSession?

    fun observeRunningSession(): Flow<FocusSession?>

    /** Marks a running session as interrupted ("with breaks"); it keeps running. */
    suspend fun markInterrupted(sessionId: Long)

    /** Ends a running session with [status] at [endedAt]. Does nothing if it has already ended. */
    suspend fun finishSession(sessionId: Long, status: FocusSessionStatus, endedAt: Instant)

    /** Sessions that started from [from] to [to], both inclusive. */
    fun observeSessions(from: LocalDate, to: LocalDate): Flow<List<FocusSession>>
}
