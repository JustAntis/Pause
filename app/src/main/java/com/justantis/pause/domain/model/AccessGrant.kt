package com.justantis.pause.domain.model

import java.time.Instant

/**
 * Time-boxed access to an app granted after a bypass. Until [expiresAt] the app is not blocked.
 */
data class AccessGrant(
    val packageName: String,
    val expiresAt: Instant
) {
    fun isActiveAt(now: Instant): Boolean = now.isBefore(expiresAt)
}
