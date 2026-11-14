package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.FocusRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Ends the running session before its planned end. It becomes CANCELLED and is counted neither as
 * clean nor as with breaks (assumption A3).
 */
class EndFocusSessionEarlyUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val scheduler: BackgroundWorkScheduler,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        val session = focusRepository.getRunningSession() ?: return
        focusRepository.finishSession(session.id, FocusSessionStatus.CANCELLED, clock.instant())
        scheduler.cancelFocusSessionEnd()
    }
}
