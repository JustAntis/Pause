package com.justantis.pause.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val start = LocalDate.of(2026, 10, 1)

    /**
     * Builds consecutive closed days from a pattern: C = clean, B = limit bypass, U = untracked,
     * T = today without a bypass, X = today with a bypass.
     */
    private fun days(pattern: String): List<DayRecord> = pattern.mapIndexed { index, symbol ->
        val date = start.plusDays(index.toLong())
        when (symbol) {
            'C' -> DayRecord(date, closed = true, tracked = true, limitBypasses = 0, isToday = false)
            'B' -> DayRecord(date, closed = true, tracked = true, limitBypasses = 1, isToday = false)
            'U' -> DayRecord(date, closed = true, tracked = false, limitBypasses = 0, isToday = false)
            'T' -> DayRecord(date, closed = false, tracked = false, limitBypasses = 0, isToday = true)
            'X' -> DayRecord(date, closed = false, tracked = false, limitBypasses = 2, isToday = true)
            else -> error("Unknown symbol $symbol")
        }
    }

    private fun calculate(pattern: String) = StreakCalculator.calculate(days(pattern))

    @Test
    fun `clean days increment the streak`() {
        assertEquals(3, calculate("CCC").current)
    }

    @Test
    fun `a freeze is earned at 7 and 14 clean days and capped at 2`() {
        assertEquals(1, calculate("C".repeat(7)).freezes)
        assertEquals(1, calculate("C".repeat(13)).freezes)
        assertEquals(2, calculate("C".repeat(14)).freezes)
        assertEquals(2, calculate("C".repeat(21)).freezes)
    }

    @Test
    fun `first bypass with a freeze freezes the day and keeps the streak`() {
        val result = calculate("CCCCCCCB")

        assertEquals(7, result.current)
        assertEquals(0, result.freezes)
        assertEquals(DayStatus.FROZEN, result.days.last().status)
    }

    @Test
    fun `bypass without a freeze breaks the streak`() {
        val result = calculate("CCCB")

        assertEquals(0, result.current)
        assertEquals(DayStatus.BROKEN, result.days.last().status)
    }

    @Test
    fun `untracked days neither count nor break`() {
        val result = calculate("CCUUC")

        assertEquals(3, result.current)
        assertEquals(DayStatus.UNTRACKED, result.days[2].status)
    }

    @Test
    fun `untracked day with a bypass is still decided by the bypass`() {
        val records = days("CC") + DayRecord(start.plusDays(2), closed = true, tracked = false, limitBypasses = 1, isToday = false)

        val result = StreakCalculator.calculate(records)

        assertEquals(0, result.current)
        assertEquals(DayStatus.BROKEN, result.days.last().status)
    }

    @Test
    fun `best streak is the longest run`() {
        assertEquals(5, calculate("CCCCCBCCC").best)
    }

    @Test
    fun `days until the next freeze count down to the next multiple of 7`() {
        assertEquals(7, calculate("").daysUntilNextFreeze)
        assertEquals(2, calculate("C".repeat(12)).daysUntilNextFreeze)
        assertEquals(7, calculate("C".repeat(7)).daysUntilNextFreeze)
    }

    @Test
    fun `no next freeze while the maximum is held`() {
        assertNull(calculate("C".repeat(15)).daysUntilNextFreeze)
    }

    @Test
    fun `today without a bypass is in progress and not counted`() {
        val result = calculate("CCT")

        assertEquals(2, result.current)
        assertEquals(DayStatus.IN_PROGRESS, result.days.last().status)
    }

    @Test
    fun `bypass today breaks or freezes immediately and more bypasses change nothing`() {
        assertEquals(DayStatus.BROKEN, calculate("CCX").days.last().status)
        assertEquals(0, calculate("CCX").current)

        val frozen = calculate("C".repeat(7) + "X")
        assertEquals(DayStatus.FROZEN, frozen.days.last().status)
        assertEquals(7, frozen.current)
    }

    @Test
    fun `past day not closed yet is untracked`() {
        val records = days("CC") + DayRecord(start.plusDays(2), closed = false, tracked = false, limitBypasses = 0, isToday = false)

        val result = StreakCalculator.calculate(records)

        assertEquals(2, result.current)
        assertEquals(DayStatus.UNTRACKED, result.days.last().status)
    }

    @Test
    fun `same history gives the same result`() {
        assertEquals(calculate("CCBUCCCCCCCBCT"), calculate("CCBUCCCCCCCBCT"))
    }
}
