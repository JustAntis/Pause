package com.justantis.pause.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.justantis.pause.R

/**
 * The single duration format used on every screen: "45 min", "3h", "2h 14m".
 */
@Composable
fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, minutes)
        minutes == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
}
