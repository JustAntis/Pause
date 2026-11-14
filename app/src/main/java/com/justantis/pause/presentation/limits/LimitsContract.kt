package com.justantis.pause.presentation.limits

import java.time.DayOfWeek

sealed interface ActiveDays {
    data object EveryDay : ActiveDays
    data object Weekdays : ActiveDays
    data object Weekends : ActiveDays
    data class Custom(val days: List<DayOfWeek>) : ActiveDays
    data class Except(val days: List<DayOfWeek>) : ActiveDays
}

sealed interface LimitStatus {
    data class Remaining(val minutes: Int) : LimitStatus
    data class UsedUp(val attempts: Int) : LimitStatus
    data object NotActiveToday : LimitStatus
}

data class LimitItem(
    val packageName: String,
    val label: String,
    val limitMinutes: Int,
    val usedMinutes: Int,
    val progress: Float,
    val activeDays: ActiveDays,
    val status: LimitStatus
)

data class LimitsState(
    val isLoading: Boolean = true,
    val items: List<LimitItem> = emptyList()
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty()
}

sealed interface LimitsIntent {
    data object Refresh : LimitsIntent
    data object AddLimitClicked : LimitsIntent
    data class LimitClicked(val packageName: String) : LimitsIntent
}

sealed interface LimitsEffect {
    data class OpenLimitEdit(val packageName: String?) : LimitsEffect
}
