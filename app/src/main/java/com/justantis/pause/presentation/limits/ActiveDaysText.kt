package com.justantis.pause.presentation.limits

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.justantis.pause.R
import com.justantis.pause.presentation.components.dayName

/** "every day", "Weekdays only", "Weekends only", "Mon, Wed only" or "Except Sat". */
@Composable
fun activeDaysText(days: ActiveDays): String = when (days) {
    ActiveDays.EveryDay -> stringResource(R.string.limits_days_every_day)
    ActiveDays.Weekdays -> stringResource(R.string.limits_days_weekdays)
    ActiveDays.Weekends -> stringResource(R.string.limits_days_weekends)
    is ActiveDays.Custom -> stringResource(R.string.limits_days_custom, days.days.map { dayName(it) }.joinToString(", "))
    is ActiveDays.Except -> stringResource(R.string.limits_days_except, days.days.map { dayName(it) }.joinToString(", "))
}
