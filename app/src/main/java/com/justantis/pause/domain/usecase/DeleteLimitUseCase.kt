package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.LimitRepository
import javax.inject.Inject

/**
 * Removes the limit of one app. Its past attempts and bypasses stay in the history.
 */
class DeleteLimitUseCase @Inject constructor(
    private val limitRepository: LimitRepository
) {
    suspend operator fun invoke(packageName: String) = limitRepository.deleteLimit(packageName)
}
