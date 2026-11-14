package com.justantis.pause.testing

import com.justantis.pause.domain.model.AccessGrant
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.BlockEvent
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.repository.AppCatalogRepository
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.LimitRepository
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

class FakeUsageRepository(var summary: UsageSummary = UsageSummary.EMPTY) : UsageRepository {
    override suspend fun usageOn(date: LocalDate): UsageSummary = summary

    override suspend fun syncDay(date: LocalDate) = Unit

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

    override suspend fun recordAttempt(packageName: String, reason: BlockReason, focusSessionId: Long?): Long {
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
