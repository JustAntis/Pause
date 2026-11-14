package com.justantis.pause.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.justantis.pause.R
import com.justantis.pause.data.usage.InstalledAppsDataSource
import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.repository.Notifier
import com.justantis.pause.domain.repository.WeeklySummary
import com.justantis.pause.presentation.components.DurationTexts
import com.justantis.pause.presentation.navigation.OpenTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import kotlin.math.abs

/**
 * Builds and posts the notifications. Every notification opens the screen it is about.
 * Nothing is posted while notifications are not allowed for the app.
 */
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedApps: InstalledAppsDataSource,
    private val clock: Clock
) : Notifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun showLimitWarning(packageName: String, minutesLeft: Int) {
        val label = installedApps.app(packageName)?.label ?: packageName
        post(
            id = LIMIT_ID_BASE + packageName.hashCode(),
            builder = builder(NotificationChannels.LIMITS, OpenTarget.AppDetails(packageName))
                .setContentTitle(context.getString(R.string.notification_limit_title, label))
                .setContentText(
                    context.resources.getQuantityString(R.plurals.notification_limit_text, minutesLeft, minutesLeft)
                )
                .setAutoCancel(true)
        )
    }

    override fun showFocusRunning(session: FocusSession) {
        post(
            id = FOCUS_ID,
            builder = builder(NotificationChannels.FOCUS, OpenTarget.Focus)
                .setContentTitle(context.getString(R.string.notification_focus_running_title))
                .setContentText(context.getString(R.string.notification_focus_running_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(session.plannedEnd.toEpochMilli())
                .setTimeoutAfter((session.plannedEnd.toEpochMilli() - clock.millis()).coerceAtLeast(0))
        )
    }

    override fun showFocusFinished(withBreaks: Boolean) {
        post(
            id = FOCUS_ID,
            builder = builder(NotificationChannels.FOCUS, OpenTarget.Focus)
                .setContentTitle(context.getString(R.string.notification_focus_finished_title))
                .setContentText(
                    context.getString(
                        if (withBreaks) R.string.focus_result_with_breaks else R.string.focus_result_clean
                    )
                )
                .setAutoCancel(true)
        )
    }

    override fun clearFocus() = manager.cancel(FOCUS_ID)

    override fun showWeeklySummary(summary: WeeklySummary) {
        val average = DurationTexts(
            minutesTemplate = context.getString(R.string.duration_minutes),
            hoursTemplate = context.getString(R.string.duration_hours),
            hoursMinutesTemplate = context.getString(R.string.duration_hours_minutes)
        ).format(summary.averageScreenTimeMinutes)
        val change = summary.changePercent?.let {
            when {
                it > 0 -> context.getString(R.string.stats_change_up, it)
                it < 0 -> context.getString(R.string.stats_change_down, abs(it))
                else -> context.getString(R.string.stats_change_none, 0)
            }
        }
        val streak = context.resources.getQuantityString(R.plurals.profile_days, summary.streakDays, summary.streakDays)
        val text = if (change != null) {
            context.getString(R.string.notification_summary_text_change, average, change, streak)
        } else {
            context.getString(R.string.notification_summary_text, average, streak)
        }
        post(
            id = SUMMARY_ID,
            builder = builder(NotificationChannels.SUMMARY, OpenTarget.Statistics)
                .setContentTitle(context.getString(R.string.notification_summary_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
        )
    }

    override fun clearAll() = manager.cancelAll()

    private fun builder(channel: String, target: OpenTarget): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    target.hashCode(),
                    OpenTarget.intent(context, target),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

    @SuppressLint("MissingPermission")
    private fun post(id: Int, builder: NotificationCompat.Builder) {
        if (!manager.areNotificationsEnabled()) return
        manager.notify(id, builder.build())
    }

    private companion object {
        const val FOCUS_ID = 1
        const val SUMMARY_ID = 2
        const val LIMIT_ID_BASE = 1_000
    }
}
