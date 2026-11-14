package com.justantis.pause.domain.stats

import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.repository.WeeklySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeeklySummaryCalculatorTest {

    private val monday = LocalDate.of(2026, 11, 16)

    private fun summary(date: LocalDate, minutes: Int) =
        DailySummary(date, minutes * 60_000L, 40, closed = true, tracked = true)

    @Test
    fun `last week is averaged over its days with data and compared with the week before`() {
        val result = WeeklySummaryCalculator.calculate(
            today = monday,
            summaries = listOf(
                summary(LocalDate.of(2026, 11, 9), 120),
                summary(LocalDate.of(2026, 11, 15), 180),
                summary(LocalDate.of(2026, 11, 3), 200)
            ),
            streakDays = 12
        )

        assertEquals(WeeklySummary(averageScreenTimeMinutes = 150, changePercent = -25, streakDays = 12), result)
    }

    @Test
    fun `no comparison without data in the week before`() {
        val result = WeeklySummaryCalculator.calculate(
            today = monday,
            summaries = listOf(summary(LocalDate.of(2026, 11, 10), 90)),
            streakDays = 0
        )

        assertEquals(90, result!!.averageScreenTimeMinutes)
        assertNull(result.changePercent)
    }

    @Test
    fun `no summary without data last week`() {
        assertNull(WeeklySummaryCalculator.calculate(monday, listOf(summary(LocalDate.of(2026, 11, 3), 200)), 3))
    }
}
