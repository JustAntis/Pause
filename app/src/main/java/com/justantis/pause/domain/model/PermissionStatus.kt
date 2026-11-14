package com.justantis.pause.domain.model

/**
 * Which of the special access rights the app currently has. Usage access and the accessibility
 * service are both required; notifications are optional.
 */
data class PermissionStatus(
    val usageAccess: Boolean,
    val accessibilityService: Boolean,
    val notifications: Boolean
) {
    val allRequiredGranted: Boolean
        get() = usageAccess && accessibilityService
}
