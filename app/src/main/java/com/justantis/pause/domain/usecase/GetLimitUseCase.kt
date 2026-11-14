package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.repository.LimitRepository
import javax.inject.Inject

/**
 * The limit of one app, or null when the app has none.
 */
class GetLimitUseCase @Inject constructor(
    private val limitRepository: LimitRepository
) {
    suspend operator fun invoke(packageName: String): AppLimit? = limitRepository.getLimit(packageName)
}
