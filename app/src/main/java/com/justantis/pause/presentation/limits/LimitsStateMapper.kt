package com.justantis.pause.presentation.limits

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.UsageSummary
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What the Limits screen reads from the system on refresh. [appLabels] holds launchable apps.
 */
data class LimitsSnapshot(
    val date: LocalDate,
    val usage: UsageSummary,
    val appLabels: Map<String, String>
)

object LimitsStateMapper {

    private const val MILLIS_PER_MINUTE = 60_000L
    private val WEEKDAYS = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
    private val WEEKENDS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun map(
        limits: List<AppLimit>,
        snapshot: LimitsSnapshot,
        attemptsByApp: Map<String, Int>
    ) = LimitsState(
        isLoading = false,
        items = limits
            .sortedBy { it.createdAt }
            .map { limit -> toItem(limit, snapshot, attemptsByApp[limit.packageName] ?: 0) }
    )

    private fun toItem(limit: AppLimit, snapshot: LimitsSnapshot, attempts: Int): LimitItem {
        val usedMinutes = ((snapshot.usage.usageOf(limit.packageName)?.foregroundMs ?: 0L) / MILLIS_PER_MINUTE).toInt()
        val appliesToday = limit.appliesOn(snapshot.date)
        val status = when {
            !appliesToday -> LimitStatus.NotActiveToday
            usedMinutes >= limit.limitMinutes -> LimitStatus.UsedUp(attempts)
            else -> LimitStatus.Remaining(limit.limitMinutes - usedMinutes)
        }
        return LimitItem(
            packageName = limit.packageName,
            label = snapshot.appLabels[limit.packageName] ?: limit.packageName,
            limitMinutes = limit.limitMinutes,
            usedMinutes = usedMinutes,
            progress = if (appliesToday) (usedMinutes.toFloat() / limit.limitMinutes).coerceAtMost(1f) else 0f,
            activeDays = describe(limit.activeDays),
            status = status
        )
    }

    private fun describe(days: Set<DayOfWeek>): ActiveDays = when (days) {
        DayOfWeek.entries.toSet() -> ActiveDays.EveryDay
        WEEKDAYS -> ActiveDays.Weekdays
        WEEKENDS -> ActiveDays.Weekends
        else -> if (days.size > DayOfWeek.entries.size / 2) {
            ActiveDays.Except((DayOfWeek.entries - days).sorted())
        } else {
            ActiveDays.Custom(days.sorted())
        }
    }
}
