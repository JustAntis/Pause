package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * How many times the break screen was shown on [date], across all apps. Every showing is an
 * attempt, whatever the user did next.
 */
class ObserveAttemptCountUseCase @Inject constructor(
    private val blockRepository: BlockRepository
) {
    operator fun invoke(date: LocalDate): Flow<Int> =
        blockRepository.observeEvents(date, date).map { it.size }
}
