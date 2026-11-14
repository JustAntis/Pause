package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Reads the current state of usage access, the accessibility service and notifications.
 * The user can change them in system settings at any time, so screens read it again on resume.
 */
class GetPermissionStatusUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(): PermissionStatus = permissionRepository.currentStatus()
}
