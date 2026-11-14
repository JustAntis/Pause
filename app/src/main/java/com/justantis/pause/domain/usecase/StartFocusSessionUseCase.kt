package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.Notifier
import javax.inject.Inject

/**
 * Starts a Focus Mode session (spec §4.3). The chosen apps become the apps paused in Focus Mode, the
 * session is stored at once so that it survives the app process being killed, and work is scheduled
 * to finish it at its planned end. An ongoing notification counts down to the end. Only one session runs at a time; if one is running it is returned.
 */
class StartFocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val scheduler: BackgroundWorkScheduler,
    private val notifier: Notifier
) {
    suspend operator fun invoke(plannedMinutes: Int, pausedApps: Set<String>): FocusSession {
        require(pausedApps.isNotEmpty()) { "A Focus Mode session needs at least one paused app" }
        focusRepository.getRunningSession()?.let { return it }

        focusRepository.setFocusApps(pausedApps)
        val session = focusRepository.startSession(plannedMinutes)
        scheduler.scheduleFocusSessionEnd(session.plannedEnd)
        notifier.showFocusRunning(session)
        return session
    }
}
