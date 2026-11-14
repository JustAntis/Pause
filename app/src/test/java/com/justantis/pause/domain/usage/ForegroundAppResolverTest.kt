package com.justantis.pause.domain.usage

import com.justantis.pause.domain.model.UsageEvent
import com.justantis.pause.domain.model.UsageEventType.BACKGROUND
import com.justantis.pause.domain.model.UsageEventType.FOREGROUND
import com.justantis.pause.domain.model.UsageEventType.SCREEN_OFF
import com.justantis.pause.domain.model.UsageEventType.UNLOCK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppResolverTest {

    @Test
    fun `the last app brought to the front is in front`() {
        val events = listOf(
            UsageEvent(INSTAGRAM, 1, FOREGROUND),
            UsageEvent(INSTAGRAM, 2, BACKGROUND),
            UsageEvent(CHROME, 3, FOREGROUND),
            UsageEvent(SYSTEM, 4, UNLOCK)
        )

        assertEquals(CHROME, ForegroundAppResolver.appInFront(events))
    }

    @Test
    fun `an app that went to the background is not in front`() {
        val events = listOf(UsageEvent(CHROME, 1, FOREGROUND), UsageEvent(CHROME, 2, BACKGROUND))

        assertNull(ForegroundAppResolver.appInFront(events))
    }

    @Test
    fun `nothing is in front after the screen turned off`() {
        val events = listOf(UsageEvent(CHROME, 1, FOREGROUND), UsageEvent(SYSTEM, 2, SCREEN_OFF))

        assertNull(ForegroundAppResolver.appInFront(events))
    }

    @Test
    fun `moving between screens of the same app keeps it in front`() {
        val events = listOf(
            UsageEvent(CHROME, 1, FOREGROUND),
            UsageEvent(CHROME, 2, BACKGROUND),
            UsageEvent(CHROME, 2, FOREGROUND)
        )

        assertEquals(CHROME, ForegroundAppResolver.appInFront(events))
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val CHROME = "com.android.chrome"
        const val SYSTEM = "android"
    }
}
