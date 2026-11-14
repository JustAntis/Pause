package com.justantis.pause.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.justantis.pause.R

/**
 * The three notification channels (spec §8). Users can turn each off in system settings.
 */
object NotificationChannels {
    const val LIMITS = "limits"
    const val FOCUS = "focus"
    const val SUMMARY = "summary"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(LIMITS, context.getString(R.string.channel_limits), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(FOCUS, context.getString(R.string.channel_focus), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(SUMMARY, context.getString(R.string.channel_summary), NotificationManager.IMPORTANCE_DEFAULT)
            )
        )
    }
}
