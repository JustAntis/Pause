package com.justantis.pause.presentation.appdetails

import com.justantis.pause.domain.usecase.AppDetails
import com.justantis.pause.presentation.components.BarStyle
import com.justantis.pause.presentation.limits.LimitsStateMapper

/**
 * A day of the last-7-days chart is a warning when the app's limit applied on that day and was
 * reached; past limits are not stored, so the current limit is used. Today is highlighted.
 */
object AppDetailsStateMapper {

    private const val MILLIS_PER_MINUTE = 60_000L

    fun map(details: AppDetails) = AppDetailsState(
        isLoading = false,
        packageName = details.packageName,
        label = details.app?.label ?: details.packageName,
        category = details.app?.category,
        todayMinutes = ((details.todayUsage?.foregroundMs ?: 0L) / MILLIS_PER_MINUTE).toInt(),
        opensToday = details.todayUsage?.openCount ?: 0,
        bypassesToday = details.bypassesToday,
        bars = details.lastDays.map { day ->
            val minutes = (day.foregroundMs / MILLIS_PER_MINUTE).toInt()
            val limit = details.limit
            val limitReached = limit != null && limit.appliesOn(day.date) && minutes >= limit.limitMinutes
            AppDayBar(
                date = day.date,
                minutes = minutes,
                style = when {
                    limitReached -> BarStyle.WARNING
                    day.date == details.today -> BarStyle.HIGHLIGHT
                    else -> BarStyle.NORMAL
                }
            )
        },
        limitMinutes = details.limit?.limitMinutes,
        limitDays = details.limit?.let { LimitsStateMapper.describe(it.activeDays) },
        pausedInFocus = details.pausedInFocus
    )
}
