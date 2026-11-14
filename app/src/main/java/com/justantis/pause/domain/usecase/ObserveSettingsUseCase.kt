package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The user's current preferences, updated whenever they change.
 */
class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> = settingsRepository.settings
}
