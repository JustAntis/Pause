package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_app_usage WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsageEntity>>

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    suspend fun getSummary(date: LocalDate): DailySummaryEntity?

    @Upsert
    suspend fun upsertSummary(summary: DailySummaryEntity)

    @Upsert
    suspend fun upsertAppUsage(usage: List<DailyAppUsageEntity>)

    @Query("DELETE FROM daily_app_usage WHERE date = :date")
    suspend fun deleteAppUsage(date: LocalDate)

    /** Replaces the totals of [date] but keeps its closed/tracked flags. */
    @Transaction
    suspend fun replaceDay(
        date: LocalDate,
        screenTimeMs: Long,
        unlockCount: Int,
        apps: List<DailyAppUsageEntity>
    ) {
        val existing = getSummary(date)
        upsertSummary(
            DailySummaryEntity(
                date = date,
                screenTimeMs = screenTimeMs,
                unlockCount = unlockCount,
                closed = existing?.closed ?: false,
                tracked = existing?.tracked ?: false
            )
        )
        deleteAppUsage(date)
        upsertAppUsage(apps)
    }
}
