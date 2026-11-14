package com.justantis.pause.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.justantis.pause.domain.model.UsageEvent
import com.justantis.pause.domain.model.UsageEventType
import javax.inject.Inject

/**
 * Reads raw events from [UsageStatsManager] and keeps only the types the aggregator needs.
 * Only ACTIVITY_PAUSED is mapped to BACKGROUND: ACTIVITY_STOPPED of the previous activity often
 * arrives after the next activity of the same app has resumed and would end its session too early.
 */
class UsageStatsDataSource @Inject constructor(
    private val usageStatsManager: UsageStatsManager
) {
    fun queryEvents(startMs: Long, endMs: Long): List<UsageEvent> {
        val events = usageStatsManager.queryEvents(startMs, endMs) ?: return emptyList()
        val result = mutableListOf<UsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = mapType(event.eventType) ?: continue
            result += UsageEvent(event.packageName, event.timeStamp, type)
        }
        return result
    }

    private fun mapType(eventType: Int): UsageEventType? = when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.FOREGROUND
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.BACKGROUND
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.UNLOCK
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_OFF
        UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.SHUTDOWN
        else -> null
    }
}
