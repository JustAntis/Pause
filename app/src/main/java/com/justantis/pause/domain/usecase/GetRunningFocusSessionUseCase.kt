package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.repository.FocusRepository
import javax.inject.Inject

/**
 * The Focus Mode session that is currently running, or null.
 */
class GetRunningFocusSessionUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    suspend operator fun invoke(): FocusSession? = focusRepository.getRunningSession()
}
