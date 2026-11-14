package com.justantis.pause.testing

import com.justantis.pause.domain.model.AccessGrant
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.BlockEvent
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.repository.AppCatalogRepository
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.LimitRepository
import com.justantis.pause.domain.repository.Notifier
import com.justantis.pause.domain.repository.WeeklySummary
import com.justantis.pause.domain.repository.PermissionRepository
import com.justantis.pause.domain.repository.SettingsRepository
import com.justantis.pause.domain.repository.StreakRepository
import com.justantis.pause.domain.streak.StreakState
import com.justantis.pause.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class FakeLimitRepository(initial: List<AppLimit> = emptyList()) : LimitRepository {
    val limits = MutableStateFlow(initial)

    override fun observeLimits(): Flow<List<AppLimit>> = limits

    override fun observeLimit(packageName: String): Flow<AppLimit?> =
        limits.map { list -> list.firstOrNull { it.packageName == packageName } }

    override suspend fun getLimits(): List<AppLimit> = limits.value

    override suspend fun getLimit(packageName: String): AppLimit? =
        limits.value.firstOrNull { it.packageName == packageName }

    override suspend fun saveLimit(limit: AppLimit) = limits.update { list ->
        list.filterNot { it.packageName == limit.packageName } + limit
    }

    override suspend fun deleteLimit(packageName: String) = limits.update { list ->
        list.filterNot { it.packageName == packageName }
    }
}

open class FakeUsageRepository(var summary: UsageSummary = UsageSummary.EMPTY) : UsageRepository {
    val summaryByDate = mutableMapOf<LocalDate, UsageSummary>()

    override suspend fun usageOn(date: LocalDate): UsageSummary = summaryByDate[date] ?: summary

    val syncedDays = mutableListOf<LocalDate>()
    val closedDays = mutableMapOf<LocalDate, Boolean>()

    /** Days for which the system still has events; null means every day has events. */
    var daysWithEvents: Set<LocalDate>? = null

    override suspend fun syncDay(date: LocalDate): Boolean {
        syncedDays += date
        return daysWithEvents?.contains(date) ?: true
    }

    override suspend fun closeDay(date: LocalDate, tracked: Boolean) {
        closedDays[date] = tracked
    }

    override suspend fun lastClosedDay(): LocalDate? = closedDays.keys.maxOrNull()

    override fun observeDailySummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> =
        flowOf(emptyList())

    override fun observeDailyAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>> =
        flowOf(emptyList())
}

class FakeAppCatalogRepository(private val apps: List<InstalledApp>) : AppCatalogRepository {
    override suspend fun launchableApps(): List<InstalledApp> = apps

    override suspend fun app(packageName: String): InstalledApp? = apps.firstOrNull { it.packageName == packageName }
}

class FakeBlockRepository(private val clock: Clock) : BlockRepository {
    val events = MutableStateFlow<List<BlockEvent>>(emptyList())
    val grants = mutableMapOf<String, AccessGrant>()

    /**
     * Makes [recordAttempt] suspend after the row is written, like a Room insert whose caller resumes
     * only after the write: cancelling the caller then leaves the row in place.
     */
    var recordDelayMs = 0L

    override suspend fun recordAttempt(packageName: String, reason: BlockReason, focusSessionId: Long?): Long {
        val id = insertAttempt(packageName, reason, focusSessionId)
        if (recordDelayMs > 0) kotlinx.coroutines.delay(recordDelayMs)
        return id
    }

    private fun insertAttempt(packageName: String, reason: BlockReason, focusSessionId: Long?): Long {
        val id = (events.value.maxOfOrNull { it.id } ?: 0L) + 1
        events.update {
            it + BlockEvent(
                id = id,
                timestamp = clock.instant(),
                date = LocalDate.now(clock),
                packageName = packageName,
                reason = reason,
                outcome = BlockOutcome.CLOSED,
                purpose = null,
                grantedMinutes = null,
                pauseSeconds = null,
                focusSessionId = focusSessionId
            )
        }
        return id
    }

    override suspend fun recordBypass(eventId: Long, purpose: AccessPurpose, grantedMinutes: Int, pauseSeconds: Int) =
        events.update { list ->
            list.map {
                if (it.id == eventId) {
                    it.copy(
                        outcome = BlockOutcome.BYPASSED,
                        purpose = purpose,
                        grantedMinutes = grantedMinutes,
                        pauseSeconds = pauseSeconds
                    )
                } else {
                    it
                }
            }
        }

    override suspend fun getEvent(eventId: Long): BlockEvent? = events.value.firstOrNull { it.id == eventId }

    override suspend fun countBypasses(date: LocalDate): Int =
        events.value.count { it.date == date && it.outcome == BlockOutcome.BYPASSED }

    override fun observeBypassCount(date: LocalDate): Flow<Int> =
        events.map { list -> list.count { it.date == date && it.outcome == BlockOutcome.BYPASSED } }

    override fun observeAttemptCount(date: LocalDate, packageName: String): Flow<Int> =
        events.map { list -> list.count { it.date == date && it.packageName == packageName } }

    override fun observeEvents(from: LocalDate, to: LocalDate): Flow<List<BlockEvent>> =
        events.map { list -> list.filter { it.date in from..to } }

    override suspend fun grantAccess(packageName: String, expiresAt: Instant) {
        grants[packageName] = AccessGrant(packageName, expiresAt)
    }

    override suspend fun getActiveGrant(packageName: String): AccessGrant? =
        grants[packageName]?.takeIf { it.isActiveAt(clock.instant()) }
}

class FakeFocusRepository(private val clock: Clock) : FocusRepository {
    val focusApps = MutableStateFlow<Set<String>>(emptySet())
    val sessions = MutableStateFlow<List<FocusSession>>(emptyList())

    override fun observeFocusApps(): Flow<Set<String>> = focusApps

    override suspend fun isFocusApp(packageName: String): Boolean = packageName in focusApps.value

    override suspend fun setFocusApp(packageName: String, paused: Boolean) = focusApps.update {
        if (paused) it + packageName else it - packageName
    }

    override suspend fun setFocusApps(packageNames: Set<String>) {
        focusApps.value = packageNames
    }

    override suspend fun startSession(plannedMinutes: Int): FocusSession {
        val session = FocusSession(
            id = (sessions.value.maxOfOrNull { it.id } ?: 0L) + 1,
            startedAt = clock.instant(),
            plannedMinutes = plannedMinutes,
            endedAt = null,
            status = FocusSessionStatus.RUNNING
        )
        sessions.update { it + session }
        return session
    }

    override suspend fun getRunningSession(): FocusSession? = sessions.value.lastOrNull { it.isRunning }

    override fun observeRunningSession(): Flow<FocusSession?> = sessions.map { list -> list.lastOrNull { it.isRunning } }

    override suspend fun markInterrupted(sessionId: Long) = sessions.update { list ->
        list.map {
            if (it.id == sessionId && it.status == FocusSessionStatus.RUNNING) {
                it.copy(status = FocusSessionStatus.INTERRUPTED)
            } else {
                it
            }
        }
    }

    override suspend fun finishSession(sessionId: Long, status: FocusSessionStatus, endedAt: Instant) = sessions.update { list ->
        list.map { if (it.id == sessionId && it.isRunning) it.copy(endedAt = endedAt, status = status) else it }
    }

    override fun observeSessions(from: LocalDate, to: LocalDate): Flow<List<FocusSession>> = sessions
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    var historyBackfilled = false

    override suspend fun isHistoryBackfilled(): Boolean = historyBackfilled

    override suspend fun setHistoryBackfilled() {
        historyBackfilled = true
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = settings.update { it.copy(onboardingCompleted = completed) }

    override suspend fun setOnboardingCompletedOn(date: LocalDate) = settings.update { it.copy(onboardingCompletedOn = date) }

    override suspend fun setUserName(name: String?) = settings.update { it.copy(userName = name) }

    override suspend fun setScreenTimeGoalMinutes(minutes: Int) = settings.update { it.copy(screenTimeGoalMinutes = minutes) }

    override suspend fun setUnlockGoal(unlocks: Int) = settings.update { it.copy(unlockGoal = unlocks) }

    override suspend fun setNotifyBeforeLimit(enabled: Boolean) = settings.update { it.copy(notifyBeforeLimit = enabled) }

    override suspend fun setNotifyWeeklySummary(enabled: Boolean) = settings.update { it.copy(notifyWeeklySummary = enabled) }
}

class FakePermissionRepository(
    var status: PermissionStatus = PermissionStatus(usageAccess = true, accessibilityService = true, notifications = true)
) : PermissionRepository {
    override fun currentStatus(): PermissionStatus = status
}

class FakeStreakRepository(initial: StreakState = StreakState.EMPTY) : StreakRepository {
    val streak = MutableStateFlow(initial)

    override fun observeStreak(today: LocalDate): Flow<StreakState> = streak
}

class FakeWorkScheduler : BackgroundWorkScheduler {
    var usageSyncScheduled = false
    var focusEndAt: Instant? = null

    override fun scheduleUsageSync() {
        usageSyncScheduled = true
    }

    override fun scheduleFocusSessionEnd(at: Instant) {
        focusEndAt = at
    }

    override fun cancelFocusSessionEnd() {
        focusEndAt = null
    }

    var weeklySummaryScheduled = false
    var cancelledAll = false

    override fun scheduleWeeklySummary() {
        weeklySummaryScheduled = true
    }

    override fun cancelWeeklySummary() {
        weeklySummaryScheduled = false
    }

    override fun cancelAll() {
        cancelledAll = true
        usageSyncScheduled = false
        focusEndAt = null
        weeklySummaryScheduled = false
    }
}

class FakeNotifier : Notifier {
    val limitWarnings = mutableListOf<Pair<String, Int>>()
    var focusRunning: FocusSession? = null
    val focusFinished = mutableListOf<Boolean>()
    val weeklySummaries = mutableListOf<WeeklySummary>()
    var clearedAll = false

    override fun showLimitWarning(packageName: String, minutesLeft: Int) {
        limitWarnings += packageName to minutesLeft
    }

    override fun showFocusRunning(session: FocusSession) {
        focusRunning = session
    }

    override fun showFocusFinished(withBreaks: Boolean) {
        focusRunning = null
        focusFinished += withBreaks
    }

    override fun clearFocus() {
        focusRunning = null
    }

    override fun showWeeklySummary(summary: WeeklySummary) {
        weeklySummaries += summary
    }

    override fun clearAll() {
        clearedAll = true
    }
}
