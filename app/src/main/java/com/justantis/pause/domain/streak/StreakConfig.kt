package com.justantis.pause.domain.streak

/**
 * Streak rules in one place (spec §4.2): a freeze is earned every [FREEZE_EVERY_DAYS] days of the
 * streak and at most [MAX_FREEZES] are held at once.
 */
object StreakConfig {
    const val FREEZE_EVERY_DAYS = 7
    const val MAX_FREEZES = 2
}
