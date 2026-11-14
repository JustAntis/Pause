package com.justantis.pause.domain.streak

import com.justantis.pause.domain.model.BlockReason
import org.junit.Assert.assertEquals
import org.junit.Test

class BypassConsequenceTest {

    @Test
    fun `first limit bypass of the day uses a freeze when one is available`() {
        assertEquals(
            BypassConsequence.USES_FREEZE,
            BypassConsequence.of(BlockReason.LIMIT, limitBypassesToday = 0, currentStreak = 12, freezes = 1)
        )
    }

    @Test
    fun `first limit bypass without a freeze breaks the streak`() {
        assertEquals(
            BypassConsequence.BREAKS_STREAK,
            BypassConsequence.of(BlockReason.LIMIT, limitBypassesToday = 0, currentStreak = 12, freezes = 0)
        )
    }

    @Test
    fun `later bypasses on the same day change nothing`() {
        assertEquals(
            BypassConsequence.NONE,
            BypassConsequence.of(BlockReason.LIMIT, limitBypassesToday = 1, currentStreak = 12, freezes = 0)
        )
    }

    @Test
    fun `focus mode bypasses never affect the streak`() {
        assertEquals(
            BypassConsequence.NONE,
            BypassConsequence.of(BlockReason.FOCUS, limitBypassesToday = 0, currentStreak = 12, freezes = 0)
        )
    }

    @Test
    fun `there is nothing to break without a streak`() {
        assertEquals(
            BypassConsequence.NONE,
            BypassConsequence.of(BlockReason.LIMIT, limitBypassesToday = 0, currentStreak = 0, freezes = 0)
        )
    }
}
