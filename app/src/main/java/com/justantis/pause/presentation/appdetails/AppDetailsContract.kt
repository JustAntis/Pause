package com.justantis.pause.presentation.appdetails

import com.justantis.pause.domain.model.AppCategory
import com.justantis.pause.presentation.components.BarStyle
import com.justantis.pause.presentation.limits.ActiveDays
import java.time.LocalDate

data class AppDayBar(val date: LocalDate, val minutes: Int, val style: BarStyle)

data class AppDetailsState(
    val isLoading: Boolean = true,
    val packageName: String = "",
    val label: String = "",
    val category: AppCategory? = null,
    val todayMinutes: Int = 0,
    val opensToday: Int = 0,
    val bypassesToday: Int = 0,
    val bars: List<AppDayBar> = emptyList(),
    val limitMinutes: Int? = null,
    val limitDays: ActiveDays? = null,
    val pausedInFocus: Boolean = false
)

sealed interface AppDetailsIntent {
    data object Refresh : AppDetailsIntent
    data object BackClicked : AppDetailsIntent
    data object LimitClicked : AppDetailsIntent
    data class PausedInFocusChanged(val paused: Boolean) : AppDetailsIntent
}

sealed interface AppDetailsEffect {
    data object Close : AppDetailsEffect
    data class OpenLimitEdit(val packageName: String) : AppDetailsEffect
}
