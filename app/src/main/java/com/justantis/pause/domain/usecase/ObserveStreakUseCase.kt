package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.StreakRepository
import com.justantis.pause.domain.streak.StreakState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * The current streak, record, freezes and the status of every day up to [today].
 */
class ObserveStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository
) {
    operator fun invoke(today: LocalDate): Flow<StreakState> = streakRepository.observeStreak(today)
}
