package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.FocusRepository
import javax.inject.Inject

/**
 * Adds an app to, or removes it from, the apps paused during Focus Mode.
 */
class SetPausedInFocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository
) {
    suspend operator fun invoke(packageName: String, paused: Boolean) =
        focusRepository.setFocusApp(packageName, paused)
}
