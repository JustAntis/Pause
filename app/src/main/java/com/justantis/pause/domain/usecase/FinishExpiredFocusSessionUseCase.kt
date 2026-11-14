package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.Notifier
import java.time.Clock
import javax.inject.Inject

/**
 * Finishes the running session once its planned end has passed: CLEAN when nothing was bypassed,
 * otherwise INTERRUPTED ("with breaks"). The end time is the planned end, even when this runs later
 * (after the app process was killed). It is called by the end-of-session work, on app start and by
 * the Focus screen, whichever comes first; running it again changes nothing. The ongoing
 * notification is replaced by one with the result.
 */
class FinishExpiredFocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val scheduler: BackgroundWorkScheduler,
    private val notifier: Notifier,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        val session = focusRepository.getRunningSession() ?: return
        if (clock.instant().isBefore(session.plannedEnd)) return

        val status = if (session.status == FocusSessionStatus.INTERRUPTED) {
            FocusSessionStatus.INTERRUPTED
        } else {
            FocusSessionStatus.CLEAN
        }
        focusRepository.finishSession(session.id, status, session.plannedEnd)
        scheduler.cancelFocusSessionEnd()
        notifier.showFocusFinished(withBreaks = status == FocusSessionStatus.INTERRUPTED)
    }
}
