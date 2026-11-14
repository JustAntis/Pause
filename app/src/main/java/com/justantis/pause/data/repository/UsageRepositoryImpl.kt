package com.justantis.pause.data.repository

import com.justantis.pause.data.local.dao.DailyUsageDao
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.data.local.toDomain
import com.justantis.pause.data.usage.UsageStatsDataSource
import com.justantis.pause.di.IoDispatcher
import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.repository.UsageRepository
import com.justantis.pause.domain.usage.UsageEventAggregator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val usageStats: UsageStatsDataSource,
    private val dailyUsageDao: DailyUsageDao,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UsageRepository {

    override suspend fun usageOn(date: LocalDate): UsageSummary = withContext(ioDispatcher) {
        val zone = clock.zone
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), clock.millis())
        if (end <= start) return@withContext UsageSummary.EMPTY

        val events = usageStats.queryEvents(start - LOOKBACK.toMillis(), end)
        UsageEventAggregator.aggregate(events, start, end, zone)
    }

    override suspend fun syncDay(date: LocalDate) {
        val summary = usageOn(date)
        withContext(ioDispatcher) {
            dailyUsageDao.replaceDay(
                date = date,
                screenTimeMs = summary.totalScreenTimeMs,
                unlockCount = summary.unlockCount,
                apps = summary.apps.map {
                    DailyAppUsageEntity(
                        date = date,
                        packageName = it.packageName,
                        foregroundMs = it.foregroundMs,
                        openCount = it.openCount
                    )
                }
            )
        }
    }

    override fun observeDailySummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>> =
        dailyUsageDao.observeSummaries(from, to).map { list -> list.map { it.toDomain() } }

    override fun observeDailyAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>> =
        dailyUsageDao.observeAppUsage(from, to).map { list -> list.map { it.toDomain() } }

    private companion object {
        /** Events read before the window so that a session already open at its start is found. */
        val LOOKBACK: Duration = Duration.ofHours(3)
    }
}
