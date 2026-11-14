package com.justantis.pause.domain.streak

import java.time.LocalDate

/**
 * What the streak needs to know about one local day: whether the day is over ([closed]), whether
 * it was eligible for the streak ([tracked]) and how many app-limit bypasses it had (Focus Mode
 * bypasses are not included, assumption A2).
 */
data class DayRecord(
    val date: LocalDate,
    val closed: Boolean,
    val tracked: Boolean,
    val limitBypasses: Int,
    val isToday: Boolean
)

enum class DayStatus { CLEAN, FROZEN, BROKEN, UNTRACKED, IN_PROGRESS }

data class DayStreak(val date: LocalDate, val status: DayStatus)

/**
 * The streak after the whole history. [daysUntilNextFreeze] is null while the maximum number of
 * freezes is held.
 */
data class StreakState(
    val current: Int,
    val best: Int,
    val freezes: Int,
    val maxFreezes: Int,
    val daysUntilNextFreeze: Int?,
    val days: List<DayStreak>
) {
    companion object {
        val EMPTY = StreakState(
            current = 0,
            best = 0,
            freezes = 0,
            maxFreezes = StreakConfig.MAX_FREEZES,
            daysUntilNextFreeze = StreakConfig.FREEZE_EVERY_DAYS,
            days = emptyList()
        )
    }
}
