package com.justantis.pause.domain.model

import java.time.LocalDate

/**
 * Stored totals of one local calendar day. A day is [closed] once it is over and its totals are
 * final; [tracked] then says whether the day counts for the streak at all.
 */
data class DailySummary(
    val date: LocalDate,
    val screenTimeMs: Long,
    val unlockCount: Int,
    val closed: Boolean,
    val tracked: Boolean
)

/**
 * Stored usage of one app on one local calendar day.
 */
data class DailyAppUsage(
    val date: LocalDate,
    val packageName: String,
    val foregroundMs: Long,
    val openCount: Int
)
