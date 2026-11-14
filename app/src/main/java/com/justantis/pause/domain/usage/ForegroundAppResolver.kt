package com.justantis.pause.domain.usage

import com.justantis.pause.domain.model.UsageEvent
import com.justantis.pause.domain.model.UsageEventType

/**
 * Finds the app that is in front according to recent usage events: the last app brought to the
 * front that has not gone to the background since and was not followed by the screen turning off.
 * Used when the accessibility service starts while an app is already open, because no window
 * event arrives for it then.
 */
object ForegroundAppResolver {

    fun appInFront(events: List<UsageEvent>): String? {
        var inFront: String? = null
        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                UsageEventType.FOREGROUND -> inFront = event.packageName
                UsageEventType.BACKGROUND -> if (event.packageName == inFront) inFront = null
                UsageEventType.SCREEN_OFF, UsageEventType.SHUTDOWN -> inFront = null
                UsageEventType.UNLOCK, UsageEventType.KEYGUARD_SHOWN, UsageEventType.SCREEN_ON -> Unit
            }
        }
        return inFront
    }
}
