package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.BlockRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Bypasses made today. [BypassesToday.total] counts all apps and Focus Mode and sets the length of
 * the next pause (assumption A1); [BypassesToday.limit] counts only limit bypasses, which decide
 * the streak.
 */
class GetBypassesTodayUseCase @Inject constructor(
    private val blockRepository: BlockRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(): BypassesToday {
        val today = LocalDate.now(clock)
        val bypasses = blockRepository.observeEvents(today, today).first()
            .filter { it.outcome == BlockOutcome.BYPASSED }
        return BypassesToday(
            total = bypasses.size,
            limit = bypasses.count { it.reason == BlockReason.LIMIT }
        )
    }
}

data class BypassesToday(val total: Int, val limit: Int)
