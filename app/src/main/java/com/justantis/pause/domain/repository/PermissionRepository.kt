package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.PermissionStatus

/**
 * The current state of the special access rights. It can change at any time in system settings,
 * so callers read it again when the app returns to the foreground.
 */
interface PermissionRepository {
    fun currentStatus(): PermissionStatus
}
