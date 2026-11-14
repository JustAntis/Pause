package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * How many times the break screen was shown on [date] for each app (assumption A1: the attempt
 * numbers shown next to an app are per app). Apps without attempts are missing from the map.
 */
class ObserveAttemptCountsByAppUseCase @Inject constructor(
    private val blockRepository: BlockRepository
) {
    operator fun invoke(date: LocalDate): Flow<Map<String, Int>> =
        blockRepository.observeEvents(date, date).map { events ->
            events.groupingBy { it.packageName }.eachCount()
        }
}
