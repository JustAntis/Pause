package com.justantis.pause.domain.streak

/**
 * Computes the streak by walking through the days in chronological order (spec §4.2):
 * - a day with a limit bypass uses a freeze when one is held (FROZEN, streak kept) and otherwise
 *   resets the streak to 0 (BROKEN). This applies on the day of the bypass, also today;
 * - today without a bypass is IN_PROGRESS: its clean day is added only when it closes;
 * - a closed, tracked day without a bypass is CLEAN and adds 1; every multiple of 7 earns a freeze,
 *   up to 2;
 * - any other day is UNTRACKED and changes nothing.
 * The result depends only on the input, so nothing about the streak is stored or mutated.
 */
object StreakCalculator {

    fun calculate(days: List<DayRecord>): StreakState {
        var current = 0
        var best = 0
        var freezes = 0
        val statuses = days.sortedBy { it.date }.map { day ->
            val status = when {
                day.limitBypasses > 0 && freezes > 0 -> {
                    freezes--
                    DayStatus.FROZEN
                }
                day.limitBypasses > 0 -> {
                    current = 0
                    DayStatus.BROKEN
                }
                day.isToday -> DayStatus.IN_PROGRESS
                day.closed && day.tracked -> {
                    current++
                    best = maxOf(best, current)
                    if (current % StreakConfig.FREEZE_EVERY_DAYS == 0) {
                        freezes = minOf(freezes + 1, StreakConfig.MAX_FREEZES)
                    }
                    DayStatus.CLEAN
                }
                else -> DayStatus.UNTRACKED
            }
            DayStreak(day.date, status)
        }
        return StreakState(
            current = current,
            best = best,
            freezes = freezes,
            maxFreezes = StreakConfig.MAX_FREEZES,
            daysUntilNextFreeze = if (freezes >= StreakConfig.MAX_FREEZES) {
                null
            } else {
                StreakConfig.FREEZE_EVERY_DAYS - current % StreakConfig.FREEZE_EVERY_DAYS
            },
            days = statuses
        )
    }
}
