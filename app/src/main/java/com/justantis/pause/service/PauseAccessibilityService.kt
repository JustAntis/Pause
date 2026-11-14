package com.justantis.pause.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Receives only window-state changes and never reads screen content
 * (`canRetrieveWindowContent="false"`). The enforcement logic is connected in Phase 4.
 */
class PauseAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit
}
