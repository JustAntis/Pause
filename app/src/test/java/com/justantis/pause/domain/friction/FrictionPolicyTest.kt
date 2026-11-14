package com.justantis.pause.domain.friction

import com.justantis.pause.domain.model.AccessPurpose
import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionPolicyTest {

    private val policy = FrictionPolicy()

    @Test
    fun `first bypass of the day is preceded by 10 seconds`() {
        assertEquals(10, policy.pauseSecondsFor(bypassesToday = 0))
    }

    @Test
    fun `second bypass is preceded by 30 seconds`() {
        assertEquals(30, policy.pauseSecondsFor(bypassesToday = 1))
    }

    @Test
    fun `third bypass is preceded by 60 seconds`() {
        assertEquals(60, policy.pauseSecondsFor(bypassesToday = 2))
    }

    @Test
    fun `later bypasses stay at 60 seconds`() {
        assertEquals(60, policy.pauseSecondsFor(bypassesToday = 7))
    }

    @Test
    fun `access times and purposes come from the config`() {
        assertEquals(listOf(1, 5, 10), policy.allowedAccessMinutes)
        assertEquals(AccessPurpose.entries, policy.purposes)
    }
}
