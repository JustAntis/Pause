package com.justantis.pause.presentation.profile

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.streak.DayStatus
import com.justantis.pause.domain.streak.StreakState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object ProfileStateMapper {

    private const val CALENDAR_WEEKS = 4L

    fun map(streak: StreakState, settings: UserSettings, today: LocalDate) = ProfileState(
        isLoading = false,
        currentStreak = streak.current,
        bestStreak = streak.best,
        freezes = streak.freezes,
        maxFreezes = streak.maxFreezes,
        daysUntilNextFreeze = streak.daysUntilNextFreeze,
        weeks = calendar(streak, today),
        screenTimeGoalMinutes = settings.screenTimeGoalMinutes,
        unlockGoal = settings.unlockGoal
    )

    /** Four rows Monday–Sunday; the last row is the current week. */
    private fun calendar(streak: StreakState, today: LocalDate): List<List<CalendarCell>> {
        val statusByDate = streak.days.associate { it.date to it.status }
        val firstMonday = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks(CALENDAR_WEEKS - 1)
        return (0 until CALENDAR_WEEKS).map { week ->
            (0 until 7L).map { day ->
                val date = firstMonday.plusWeeks(week).plusDays(day)
                CalendarCell(
                    date = date,
                    status = statusByDate[date] ?: DayStatus.UNTRACKED,
                    isToday = date == today,
                    isFuture = date.isAfter(today)
                )
            }
        }
    }
}
