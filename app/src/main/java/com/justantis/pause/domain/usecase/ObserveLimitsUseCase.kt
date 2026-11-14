package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.repository.LimitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * All app limits, including disabled ones and ones that do not apply today.
 */
class ObserveLimitsUseCase @Inject constructor(
    private val limitRepository: LimitRepository
) {
    operator fun invoke(): Flow<List<AppLimit>> = limitRepository.observeLimits()
}
