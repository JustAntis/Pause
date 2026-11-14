package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.FocusRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Confirms a bypass (spec §6.3): the attempt becomes a bypass with the chosen purpose, minutes and
 * the pause the user waited through, the app gets time-boxed access from now on, and a Focus Mode
 * session is marked as interrupted ("with breaks") but keeps running.
 */
class GrantAccessUseCase @Inject constructor(
    private val blockRepository: BlockRepository,
    private val focusRepository: FocusRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        eventId: Long,
        packageName: String,
        reason: BlockReason,
        focusSessionId: Long?,
        purpose: AccessPurpose,
        minutes: Int,
        pauseSeconds: Int
    ) {
        blockRepository.recordBypass(eventId, purpose, minutes, pauseSeconds)
        blockRepository.grantAccess(packageName, clock.instant().plus(Duration.ofMinutes(minutes.toLong())))
        if (reason == BlockReason.FOCUS && focusSessionId != null) {
            focusRepository.markInterrupted(focusSessionId)
        }
    }
}
