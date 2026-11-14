package com.justantis.pause.service

import com.justantis.pause.domain.model.BlockReason

/**
 * Shows the break screen for an app. Implemented by the accessibility service, which may start
 * an activity while another app is in front.
 */
interface BlockScreenLauncher {
    fun showBlockScreen(packageName: String, eventId: Long, reason: BlockReason, focusSessionId: Long?)
}
