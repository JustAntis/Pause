package com.justantis.pause.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.justantis.pause.data.usage.InstalledAppsDataSource
import com.justantis.pause.data.usage.UsageStatsDataSource
import com.justantis.pause.di.ApplicationScope
import com.justantis.pause.di.IoDispatcher
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.usage.ForegroundAppResolver
import com.justantis.pause.presentation.block.BlockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Receives only window-state changes and reads nothing but the package name of the window that
 * came to the front (`canRetrieveWindowContent="false"`). It tells [EnforcementController] which
 * app is in front:
 * - a launchable app → that app;
 * - Pause itself or the home screen → no app;
 * - anything else (system UI, keyboards, dialogs) → ignored, the previous app stays in front.
 *
 * When the service starts while an app is already open (after an app update, a crash or turning
 * the service on), no window event arrives for that app, so the app in front is taken from recent
 * usage events instead.
 */
@AndroidEntryPoint
class PauseAccessibilityService : AccessibilityService(), BlockScreenLauncher {

    @Inject
    lateinit var controller: EnforcementController

    @Inject
    lateinit var installedApps: InstalledAppsDataSource

    @Inject
    lateinit var usageStats: UsageStatsDataSource

    @Inject
    lateinit var clock: Clock

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val launchableCache = HashMap<String, Boolean>()
    private var keyboardPackages: Set<String> = emptySet()
    private var windowEventReceived = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        keyboardPackages = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList
            .map { it.packageName }
            .toSet()
        controller.start(launcher = this)
        checkAppAlreadyInFront()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        windowEventReceived = true
        onPackageInFront(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }

    override fun showBlockScreen(packageName: String, eventId: Long, reason: BlockReason, focusSessionId: Long?) {
        startActivity(BlockActivity.intent(this, packageName, eventId, reason, focusSessionId))
    }

    private fun onPackageInFront(packageName: String) {
        when {
            packageName == this.packageName -> controller.onNoAppInForeground()
            packageName in keyboardPackages -> Unit
            isLaunchable(packageName) -> controller.onAppInForeground(packageName)
            packageName == installedApps.homeLauncherPackage() -> controller.onNoAppInForeground()
        }
    }

    private fun checkAppAlreadyInFront() {
        scope.launch {
            val now = clock.millis()
            val events = withContext(ioDispatcher) {
                usageStats.queryEvents(now - RECENT_EVENTS.toMillis(), now)
            }
            val inFront = ForegroundAppResolver.appInFront(events) ?: return@launch
            if (!windowEventReceived) onPackageInFront(inFront)
        }
    }

    private fun isLaunchable(packageName: String): Boolean =
        launchableCache.getOrPut(packageName) { installedApps.isLaunchable(packageName) }

    private companion object {
        val RECENT_EVENTS: Duration = Duration.ofMinutes(30)
    }
}
