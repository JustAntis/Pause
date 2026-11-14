package com.justantis.pause.domain.usage

import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.UsageEvent
import com.justantis.pause.domain.model.UsageEventType
import com.justantis.pause.domain.model.UsageSummary
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Turns raw usage events into per-app foreground time, open counts, total screen time, unlocks
 * and screen time per hour for the window `[windowStart, windowEnd)`.
 *
 * Rules:
 * - Only one app is in the foreground at a time; an app coming to the front closes the previous one.
 * - Several resumes of the same package in a row (moving between its activities) are one session.
 * - A session without a BACKGROUND event is closed by SCREEN_OFF or SHUTDOWN, or by the window end
 *   if the app is still in front.
 * - Events may start before the window; their time is clipped to the window. A session is counted
 *   as an "open" only in the window where it started.
 * - A BACKGROUND event without a matching FOREGROUND event adds no time. The system history can
 *   start in the middle of a day (for example after a reboot), so the start of such a session is
 *   unknown. Callers pass events from before the window so that sessions crossing its start are found.
 * - Unlocks: see [countUnlocks].
 */
object UsageEventAggregator {

    fun aggregate(
        events: List<UsageEvent>,
        windowStart: Long,
        windowEnd: Long,
        zone: ZoneId
    ): UsageSummary {
        val totals = WindowTotals(windowStart, windowEnd, zone, countUnlocks(events))
        val session = SessionTracker(totals)

        for (event in events.sortedBy { it.timestamp }) {
            if (event.timestamp >= windowEnd) break
            when (event.type) {
                UsageEventType.FOREGROUND -> session.onForeground(event.packageName, event.timestamp)
                UsageEventType.BACKGROUND -> session.onBackground(event.packageName, event.timestamp)
                UsageEventType.SCREEN_OFF,
                UsageEventType.SHUTDOWN -> session.onScreenOff(event.timestamp)
                UsageEventType.UNLOCK,
                UsageEventType.SCREEN_ON -> totals.onUnlockCandidate(event.type, event.timestamp)
                UsageEventType.KEYGUARD_SHOWN -> Unit
            }
        }
        session.onWindowEnd(windowEnd)
        return totals.toSummary()
    }

    /**
     * Which event counts as an unlock. A device with a lock screen reports the lock screen being
     * shown and dismissed; then only dismissals (UNLOCK) count. A device without a lock screen never
     * reports them; then every time the screen turns on counts instead. On such a device a screen
     * woken by a notification and turned off again without use is also counted (overcounting).
     * All passed events, including those before the window, are used to recognise a lock screen.
     */
    private fun countUnlocks(events: List<UsageEvent>): UsageEventType {
        val hasLockScreen = events.any {
            it.type == UsageEventType.UNLOCK || it.type == UsageEventType.KEYGUARD_SHOWN
        }
        return if (hasLockScreen) UsageEventType.UNLOCK else UsageEventType.SCREEN_ON
    }

    private class SessionTracker(private val totals: WindowTotals) {
        private var currentPackage: String? = null
        private var currentStart = 0L
        private var pausedAt: Long? = null

        fun onForeground(packageName: String, time: Long) {
            val current = currentPackage
            if (packageName == current) {
                val paused = pausedAt
                if (paused != null) {
                    totals.addInterval(current, currentStart, paused)
                    currentStart = time
                    pausedAt = null
                }
            } else {
                close(pausedAt ?: time)
                currentPackage = packageName
                currentStart = time
                totals.addOpen(packageName, time)
            }
        }

        fun onBackground(packageName: String, time: Long) {
            if (packageName == currentPackage && pausedAt == null) pausedAt = time
        }

        fun onScreenOff(time: Long) {
            close(pausedAt ?: time)
        }

        fun onWindowEnd(windowEnd: Long) {
            close(pausedAt ?: windowEnd)
        }

        private fun close(time: Long) {
            currentPackage?.let { totals.addInterval(it, currentStart, time) }
            currentPackage = null
            pausedAt = null
        }
    }

    private class WindowTotals(
        private val windowStart: Long,
        private val windowEnd: Long,
        private val zone: ZoneId,
        private val unlockType: UsageEventType
    ) {
        private val foregroundMs = mutableMapOf<String, Long>()
        private val openCounts = mutableMapOf<String, Int>()
        private val hourlyMs = LongArray(24)
        private var unlocks = 0

        fun addInterval(packageName: String, start: Long, end: Long) {
            val from = maxOf(start, windowStart)
            val to = minOf(end, windowEnd)
            if (to <= from) return
            foregroundMs[packageName] = (foregroundMs[packageName] ?: 0L) + (to - from)
            addToHours(from, to)
        }

        fun addOpen(packageName: String, time: Long) {
            if (time < windowStart) return
            openCounts[packageName] = (openCounts[packageName] ?: 0) + 1
        }

        fun onUnlockCandidate(type: UsageEventType, time: Long) {
            if (type == unlockType && time >= windowStart) unlocks++
        }

        private fun addToHours(from: Long, to: Long) {
            var start = from
            while (start < to) {
                val hourStart = Instant.ofEpochMilli(start).atZone(zone).truncatedTo(ChronoUnit.HOURS)
                val nextHour = hourStart.plusHours(1).toInstant().toEpochMilli()
                val end = minOf(nextHour, to)
                hourlyMs[hourStart.hour] += end - start
                start = end
            }
        }

        fun toSummary(): UsageSummary {
            val apps = (foregroundMs.keys + openCounts.keys)
                .map { packageName ->
                    AppUsage(
                        packageName = packageName,
                        foregroundMs = foregroundMs[packageName] ?: 0L,
                        openCount = openCounts[packageName] ?: 0
                    )
                }
                .sortedByDescending { it.foregroundMs }
            return UsageSummary(
                apps = apps,
                totalScreenTimeMs = foregroundMs.values.sum(),
                unlockCount = unlocks,
                hourlyScreenTimeMs = hourlyMs.toList()
            )
        }
    }
}
