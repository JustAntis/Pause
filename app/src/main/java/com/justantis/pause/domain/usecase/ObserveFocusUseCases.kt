package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * The Focus Mode session that is running, or null; updated whenever it starts, changes or ends.
 */
class ObserveRunningFocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    operator fun invoke(): Flow<FocusSession?> = focusRepository.observeRunningSession()
}

/**
 * Focus Mode sessions that started on [date], in any state.
 */
class ObserveFocusSessionsUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<FocusSession>> = focusRepository.observeSessions(date, date)
}

/**
 * Package names of the apps paused during Focus Mode.
 */
class ObserveFocusAppsUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    operator fun invoke(): Flow<Set<String>> = focusRepository.observeFocusApps()
}
