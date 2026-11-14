package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import javax.inject.Inject

/**
 * Makes sure the periodic history sync is scheduled. Safe to call on every app start.
 */
class ScheduleBackgroundWorkUseCase @Inject constructor(
    private val scheduler: BackgroundWorkScheduler
) {
    operator fun invoke() = scheduler.scheduleUsageSync()
}
