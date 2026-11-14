package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.FocusSession

/**
 * Posts the app's notifications (spec §8). Implementations post nothing when the user has turned
 * notifications off; the app works the same without them.
 */
interface Notifier {
    fun showLimitWarning(packageName: String, minutesLeft: Int)

    /** Ongoing notification with a countdown to the planned end of [session]. */
    fun showFocusRunning(session: FocusSession)

    fun showFocusFinished(withBreaks: Boolean)

    fun clearFocus()

    fun showWeeklySummary(summary: WeeklySummary)

    fun clearAll()
}

/** Last week in numbers: average daily screen time, change vs the week before, current streak. */
data class WeeklySummary(
    val averageScreenTimeMinutes: Int,
    val changePercent: Int?,
    val streakDays: Int
)
