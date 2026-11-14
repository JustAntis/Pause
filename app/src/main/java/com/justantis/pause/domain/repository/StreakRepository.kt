package com.justantis.pause.domain.repository

import com.justantis.pause.domain.streak.StreakState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The streak computed from the stored day history and limit bypasses, from the first known day
 * until [today].
 */
interface StreakRepository {
    fun observeStreak(today: LocalDate): Flow<StreakState>
}
