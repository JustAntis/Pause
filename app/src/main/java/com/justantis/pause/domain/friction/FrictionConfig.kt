package com.justantis.pause.domain.friction

import com.justantis.pause.domain.model.AccessPurpose

/**
 * All numbers of the break screen in one place (spec §4.1).
 *
 * - [pauseSeconds]: the pause before the 1st, 2nd and 3rd-and-later bypass of a day. Bypasses are
 *   counted per day across all apps and Focus Mode (assumption A1) and reset at local midnight,
 *   which follows from counting by date; no reset job exists.
 * - [accessMinutes]: the time-boxed access the user can choose after the pause.
 * - [purposes]: the reasons the user can give for opening the app.
 */
data class FrictionConfig(
    val pauseSeconds: List<Int>,
    val accessMinutes: List<Int>,
    val purposes: List<AccessPurpose>
) {
    companion object {
        val DEFAULT = FrictionConfig(
            pauseSeconds = listOf(10, 30, 60),
            accessMinutes = listOf(1, 5, 10),
            purposes = AccessPurpose.entries
        )
    }
}
