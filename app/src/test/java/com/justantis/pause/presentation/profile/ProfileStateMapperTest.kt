package com.justantis.pause.presentation.profile

import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.streak.DayStatus
import com.justantis.pause.domain.streak.DayStreak
import com.justantis.pause.domain.streak.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProfileStateMapperTest {

    private val saturday = LocalDate.of(2026, 11, 14)

    private fun map(streak: StreakState = StreakState.EMPTY, settings: UserSettings = UserSettings()) =
        ProfileStateMapper.map(streak, settings, saturday)

    @Test
    fun `calendar has four weeks from monday to sunday ending with the current week`() {
        val weeks = map().weeks

        assertEquals(4, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(LocalDate.of(2026, 10, 19), weeks.first().first().date)
        assertEquals(DayOfWeek.MONDAY, weeks.first().first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 15), weeks.last().last().date)
    }

    @Test
    fun `today is marked and later days are in the future`() {
        val lastWeek = map().weeks.last()

        val today = lastWeek.single { it.isToday }
        assertEquals(saturday, today.date)
        assertFalse(today.isFuture)
        assertTrue(lastWeek.last().isFuture)
    }

    @Test
    fun `days take their status from the streak and days before the history are untracked`() {
        val streak = StreakState.EMPTY.copy(
            days = listOf(
                DayStreak(LocalDate.of(2026, 11, 12), DayStatus.FROZEN),
                DayStreak(LocalDate.of(2026, 11, 13), DayStatus.CLEAN),
                DayStreak(saturday, DayStatus.IN_PROGRESS)
            )
        )

        val cells = map(streak).weeks.flatten().associateBy { it.date }

        assertEquals(DayStatus.FROZEN, cells.getValue(LocalDate.of(2026, 11, 12)).status)
        assertEquals(DayStatus.CLEAN, cells.getValue(LocalDate.of(2026, 11, 13)).status)
        assertEquals(DayStatus.IN_PROGRESS, cells.getValue(saturday).status)
        assertEquals(DayStatus.UNTRACKED, cells.getValue(LocalDate.of(2026, 10, 19)).status)
    }

    @Test
    fun `streak numbers and goals are passed through`() {
        val state = map(
            streak = StreakState.EMPTY.copy(current = 12, best = 13, freezes = 1, daysUntilNextFreeze = 2),
            settings = UserSettings(screenTimeGoalMinutes = 150, unlockGoal = 35)
        )

        assertEquals(12, state.currentStreak)
        assertEquals(13, state.bestStreak)
        assertEquals(1, state.freezes)
        assertEquals(2, state.daysUntilNextFreeze)
        assertEquals(150, state.screenTimeGoalMinutes)
        assertEquals(35, state.unlockGoal)
    }
}
