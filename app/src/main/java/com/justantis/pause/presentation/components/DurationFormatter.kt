package com.justantis.pause.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.justantis.pause.R

/**
 * The single duration format used on every screen: "45 min", "3h", "2h 14m".
 */
class DurationTexts(
    private val minutesTemplate: String,
    private val hoursTemplate: String,
    private val hoursMinutesTemplate: String
) {
    fun format(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> minutesTemplate.format(minutes)
            minutes == 0 -> hoursTemplate.format(hours)
            else -> hoursMinutesTemplate.format(hours, minutes)
        }
    }
}

/** For places that format outside composition, such as chart axes. */
@Composable
fun rememberDurationTexts(): DurationTexts {
    val minutes = stringResource(R.string.duration_minutes)
    val hours = stringResource(R.string.duration_hours)
    val hoursMinutes = stringResource(R.string.duration_hours_minutes)
    return remember(minutes, hours, hoursMinutes) { DurationTexts(minutes, hours, hoursMinutes) }
}

@Composable
fun formatDuration(totalMinutes: Int): String = rememberDurationTexts().format(totalMinutes)
