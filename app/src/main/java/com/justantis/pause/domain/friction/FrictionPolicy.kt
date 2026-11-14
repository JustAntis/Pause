package com.justantis.pause.domain.friction

import com.justantis.pause.domain.model.AccessPurpose
import javax.inject.Inject

/**
 * The friction ladder: every bypass on the same day is preceded by a longer pause, up to the last
 * value in [FrictionConfig.pauseSeconds], which then applies to all further bypasses that day.
 */
class FrictionPolicy(private val config: FrictionConfig) {

    @Inject
    constructor() : this(FrictionConfig.DEFAULT)

    fun pauseSecondsFor(bypassesToday: Int): Int =
        config.pauseSeconds[bypassesToday.coerceIn(0, config.pauseSeconds.lastIndex)]

    val allowedAccessMinutes: List<Int>
        get() = config.accessMinutes

    val purposes: List<AccessPurpose>
        get() = config.purposes
}
