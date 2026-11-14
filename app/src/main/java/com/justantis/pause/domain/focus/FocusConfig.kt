package com.justantis.pause.domain.focus

/**
 * Focus Mode session lengths the user can choose (spec §4.3).
 */
object FocusConfig {
    val DURATIONS_MINUTES = listOf(15, 25, 45, 60)
    const val DEFAULT_MINUTES = 25
}
