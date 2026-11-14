package com.justantis.pause.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.justantis.pause.data.usage.InstalledAppsDataSource
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.presentation.block.BlockActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives only window-state changes and reads nothing but the package name of the window that
 * came to the front (`canRetrieveWindowContent="false"`). It tells [EnforcementController] which
 * app is in front:
 * - a launchable app → that app;
 * - Pause itself or the home screen → no app;
 * - anything else (system UI, keyboards, dialogs) → ignored, the previous app stays in front.
 */
@AndroidEntryPoint
class PauseAccessibilityService : AccessibilityService(), BlockScreenLauncher {

    @Inject
    lateinit var controller: EnforcementController

    @Inject
    lateinit var installedApps: InstalledAppsDataSource

    private val launchableCache = HashMap<String, Boolean>()
    private var keyboardPackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        keyboardPackages = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList
            .map { it.packageName }
            .toSet()
        controller.start(launcher = this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        when {
            packageName == this.packageName -> controller.onNoAppInForeground()
            packageName in keyboardPackages -> Unit
            isLaunchable(packageName) -> controller.onAppInForeground(packageName)
            packageName == installedApps.homeLauncherPackage() -> controller.onNoAppInForeground()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }

    override fun showBlockScreen(packageName: String, eventId: Long, reason: BlockReason, focusSessionId: Long?) {
        startActivity(BlockActivity.intent(this, packageName, eventId, reason, focusSessionId))
    }

    private fun isLaunchable(packageName: String): Boolean =
        launchableCache.getOrPut(packageName) { installedApps.isLaunchable(packageName) }
}
