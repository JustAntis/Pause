package com.justantis.pause.domain.repository

import java.time.Instant

/**
 * Starts the periodic background work. Scheduling again keeps the already scheduled work.
 */
interface BackgroundWorkScheduler {
    fun scheduleUsageSync()

    /** One-time work that finishes the running Focus Mode session at [at]; replaces earlier ones. */
    fun scheduleFocusSessionEnd(at: Instant)

    fun cancelFocusSessionEnd()
}
