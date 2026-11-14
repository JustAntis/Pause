package com.justantis.pause.domain.model

import java.time.Duration
import java.time.Instant

/**
 * One Focus Mode session. It is running while [endedAt] is null. A bypass during the session
 * changes [status] to INTERRUPTED without ending it.
 */
data class FocusSession(
    val id: Long,
    val startedAt: Instant,
    val plannedMinutes: Int,
    val endedAt: Instant?,
    val status: FocusSessionStatus
) {
    val plannedEnd: Instant
        get() = startedAt.plus(Duration.ofMinutes(plannedMinutes.toLong()))

    val isRunning: Boolean
        get() = endedAt == null
}

/**
 * RUNNING and INTERRUPTED describe a session so far; CLEAN, INTERRUPTED and CANCELLED are final
 * results. A session ended early is CANCELLED (assumption A3).
 */
enum class FocusSessionStatus { RUNNING, CLEAN, INTERRUPTED, CANCELLED }
