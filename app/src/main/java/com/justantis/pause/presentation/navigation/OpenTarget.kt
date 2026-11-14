package com.justantis.pause.presentation.navigation

import android.content.Context
import android.content.Intent
import com.justantis.pause.MainActivity

/**
 * A screen to open when the app is started from a notification.
 */
sealed interface OpenTarget {
    data object Focus : OpenTarget
    data object Statistics : OpenTarget
    data class AppDetails(val packageName: String) : OpenTarget

    companion object {
        private const val EXTRA_OPEN = "open"
        private const val EXTRA_PACKAGE = "packageName"
        private const val FOCUS = "focus"
        private const val STATISTICS = "statistics"
        private const val APP_DETAILS = "appDetails"

        fun intent(context: Context, target: OpenTarget): Intent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return when (target) {
                Focus -> intent.putExtra(EXTRA_OPEN, FOCUS)
                Statistics -> intent.putExtra(EXTRA_OPEN, STATISTICS)
                is AppDetails -> intent.putExtra(EXTRA_OPEN, APP_DETAILS).putExtra(EXTRA_PACKAGE, target.packageName)
            }
        }

        fun from(intent: Intent?): OpenTarget? = when (intent?.getStringExtra(EXTRA_OPEN)) {
            FOCUS -> Focus
            STATISTICS -> Statistics
            APP_DETAILS -> intent.getStringExtra(EXTRA_PACKAGE)?.let { AppDetails(it) }
            else -> null
        }
    }
}
