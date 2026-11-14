package com.justantis.pause.domain.streak

import com.justantis.pause.domain.model.BlockReason

/**
 * What a bypass does to the streak (spec §4.2), shown to the user before confirming access.
 * Only the first limit bypass of a day matters: it uses a freeze when one is available and breaks
 * the streak otherwise. Later bypasses that day change nothing, and Focus Mode bypasses never touch
 * the streak (assumption A2). Without a streak there is nothing to lose.
 */
enum class BypassConsequence {
    NONE,
    USES_FREEZE,
    BREAKS_STREAK;

    companion object {
        fun of(reason: BlockReason, limitBypassesToday: Int, currentStreak: Int, freezes: Int): BypassConsequence = when {
            reason == BlockReason.FOCUS -> NONE
            limitBypassesToday > 0 -> NONE
            freezes > 0 && currentStreak > 0 -> USES_FREEZE
            currentStreak > 0 -> BREAKS_STREAK
            else -> NONE
        }
    }
}
