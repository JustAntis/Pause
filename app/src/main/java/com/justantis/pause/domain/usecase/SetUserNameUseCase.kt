package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Changes the name shown in the greeting; a blank name removes it.
 */
class SetUserNameUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(name: String?) = settingsRepository.setUserName(name)
}
