package com.justantis.pause.presentation.stats

import com.justantis.pause.domain.stats.PeriodStatistics
import com.justantis.pause.domain.stats.StatisticsPeriod
import com.justantis.pause.presentation.components.BarStyle
import java.time.LocalDate

/**
 * Turns period statistics into what the screen shows. Day: one bar per hour of today with the
 * current hour highlighted. Week and Month: one bar per day; days above the screen-time goal are
 * warnings (also today), today is highlighted, other days are muted.
 */
object StatsStateMapper {

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MAX_APPS = 10

    fun map(
        stats: PeriodStatistics,
        screenTimeGoalMinutes: Int,
        appLabels: Map<String, String>,
        today: LocalDate,
        currentHour: Int
    ) = StatsState(
        isLoading = false,
        period = StatsPeriod.entries.first { it.domain == stats.period },
        averageMinutes = (stats.averageScreenTimeMs / MILLIS_PER_MINUTE).toInt(),
        changePercent = stats.changePercent,
        bars = if (stats.period == StatisticsPeriod.DAY) {
            stats.hourlyScreenTimeMs.mapIndexed { hour, ms ->
                StatsBar(
                    minutes = (ms / MILLIS_PER_MINUTE).toInt(),
                    style = if (hour == currentHour) BarStyle.HIGHLIGHT else BarStyle.NORMAL,
                    hour = hour
                )
            }
        } else {
            stats.days.map { day ->
                val minutes = (day.screenTimeMs / MILLIS_PER_MINUTE).toInt()
                StatsBar(
                    minutes = minutes,
                    style = when {
                        minutes > screenTimeGoalMinutes -> BarStyle.WARNING
                        day.date == today -> BarStyle.HIGHLIGHT
                        else -> BarStyle.NORMAL
                    },
                    date = day.date
                )
            }
        },
        averageUnlocks = stats.averageUnlocks,
        limitBypasses = stats.limitBypasses,
        apps = stats.apps
            .filter { it.packageName in appLabels && it.foregroundMs >= MILLIS_PER_MINUTE }
            .take(MAX_APPS)
            .map { StatsAppItem(it.packageName, appLabels.getValue(it.packageName), (it.foregroundMs / MILLIS_PER_MINUTE).toInt()) },
        screenTimeGoalMinutes = screenTimeGoalMinutes
    )
}
