package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.UsageSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Screen time data. A day is computed live from system events (for today the window ends now);
 * past days come from the stored history because the system keeps raw events only for a few days.
 */
interface UsageRepository {
    suspend fun usageOn(date: LocalDate): UsageSummary

    /**
     * Recomputes [date] from system events and stores it in the history. A stored day is never
     * replaced by an empty result, because the system deletes old events after a few days.
     */
    suspend fun syncDay(date: LocalDate)

    /** Marks [date] as finished with its streak eligibility; its totals stay as stored. */
    suspend fun closeDay(date: LocalDate, tracked: Boolean)

    suspend fun lastClosedDay(): LocalDate?

    /** Stored day totals from [from] to [to], both inclusive. */
    fun observeDailySummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummary>>

    /** Stored per-app usage from [from] to [to], both inclusive. */
    fun observeDailyAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>>
}
