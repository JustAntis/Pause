package com.justantis.pause.presentation.home

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.model.UserSettings
import java.time.LocalDate

/**
 * Everything read from the system when Home is refreshed. [appLabels] holds launchable apps only.
 */
data class TodaySnapshot(
    val date: LocalDate,
    val hour: Int,
    val usage: UsageSummary,
    val appLabels: Map<String, String>,
    val permissions: PermissionStatus
)

object HomeStateMapper {

    private const val TOP_APP_COUNT = 3
    private const val MILLIS_PER_MINUTE = 60_000L

    fun map(
        settings: UserSettings,
        limits: List<AppLimit>,
        snapshot: TodaySnapshot,
        attemptsToday: Int
    ) = HomeState(
        date = snapshot.date,
        isLoading = false,
        userName = settings.userName,
        timeOfDay = timeOfDay(snapshot.hour),
        screenTimeMinutes = (snapshot.usage.totalScreenTimeMs / MILLIS_PER_MINUTE).toInt(),
        screenTimeGoalMinutes = settings.screenTimeGoalMinutes,
        unlocks = snapshot.usage.unlockCount,
        attemptsToday = attemptsToday,
        topApps = topApps(snapshot, limits),
        usageAccessMissing = !snapshot.permissions.usageAccess,
        accessibilityMissing = !snapshot.permissions.accessibilityService
    )

    private fun timeOfDay(hour: Int): TimeOfDay = when (hour) {
        in 5..11 -> TimeOfDay.MORNING
        in 12..17 -> TimeOfDay.AFTERNOON
        else -> TimeOfDay.EVENING
    }

    private fun topApps(snapshot: TodaySnapshot, limits: List<AppLimit>): List<HomeAppItem> {
        val limitsToday = limits
            .filter { it.appliesOn(snapshot.date) }
            .associate { it.packageName to it.limitMinutes }
        val top = snapshot.usage.apps
            .filter { it.packageName in snapshot.appLabels && it.foregroundMs >= MILLIS_PER_MINUTE }
            .take(TOP_APP_COUNT)
        val longestMinutes = top.maxOfOrNull { it.foregroundMs / MILLIS_PER_MINUTE }?.toInt() ?: 0

        return top.map { app ->
            val usedMinutes = (app.foregroundMs / MILLIS_PER_MINUTE).toInt()
            val limitMinutes = limitsToday[app.packageName]
            val reference = limitMinutes ?: longestMinutes
            HomeAppItem(
                packageName = app.packageName,
                label = snapshot.appLabels.getValue(app.packageName),
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                progress = if (reference > 0) (usedMinutes.toFloat() / reference).coerceAtMost(1f) else 0f
            )
        }
    }
}
