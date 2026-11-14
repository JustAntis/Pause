package com.justantis.pause.domain.repository

/**
 * Starts the periodic background work. Scheduling again keeps the already scheduled work.
 */
interface BackgroundWorkScheduler {
    fun scheduleUsageSync()
}
