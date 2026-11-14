package com.justantis.pause.presentation.stats

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.presentation.components.BarStyle
import java.time.LocalDate

/** One bar of the chart: an hour of today ([hour]) or a whole day ([date]). */
data class StatsBar(
    val minutes: Int,
    val style: BarStyle,
    val hour: Int? = null,
    val date: LocalDate? = null
)

data class StatsAppItem(val packageName: String, val label: String, val minutes: Int)

data class StatsState(
    val isLoading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.Week,
    val averageMinutes: Int = 0,
    val changePercent: Int? = null,
    val bars: List<StatsBar> = emptyList(),
    val averageUnlocks: Int = 0,
    val limitBypasses: Int = 0,
    val apps: List<StatsAppItem> = emptyList(),
    val screenTimeGoalMinutes: Int = UserSettings.DEFAULT_SCREEN_TIME_GOAL_MINUTES
)

sealed interface StatsIntent {
    data object Refresh : StatsIntent
    data class PeriodSelected(val period: StatsPeriod) : StatsIntent
    data class AppClicked(val packageName: String) : StatsIntent
}

sealed interface StatsEffect {
    data class OpenAppDetails(val packageName: String) : StatsEffect
}
