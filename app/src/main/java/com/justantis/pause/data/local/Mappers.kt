package com.justantis.pause.data.local

import com.justantis.pause.data.local.entity.AccessGrantEntity
import com.justantis.pause.data.local.entity.AppLimitEntity
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.data.local.entity.DailySummaryEntity
import com.justantis.pause.data.local.entity.FocusSessionEntity
import com.justantis.pause.domain.model.AccessGrant
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.BlockEvent
import com.justantis.pause.domain.model.DailyAppUsage
import com.justantis.pause.domain.model.DailySummary
import com.justantis.pause.domain.model.FocusSession
import java.time.Instant

fun AppLimitEntity.toDomain() = AppLimit(
    packageName = packageName,
    limitMinutes = limitMinutes,
    activeDays = DaysMask.toDays(daysMask),
    enabled = enabled,
    createdAt = Instant.ofEpochMilli(createdAt)
)

fun AppLimit.toEntity() = AppLimitEntity(
    packageName = packageName,
    limitMinutes = limitMinutes,
    daysMask = DaysMask.fromDays(activeDays),
    enabled = enabled,
    createdAt = createdAt.toEpochMilli()
)

fun DailySummaryEntity.toDomain() = DailySummary(
    date = date,
    screenTimeMs = screenTimeMs,
    unlockCount = unlockCount,
    closed = closed,
    tracked = tracked
)

fun DailyAppUsageEntity.toDomain() = DailyAppUsage(
    date = date,
    packageName = packageName,
    foregroundMs = foregroundMs,
    openCount = openCount
)

fun BlockEventEntity.toDomain() = BlockEvent(
    id = id,
    timestamp = Instant.ofEpochMilli(timestamp),
    date = date,
    packageName = packageName,
    reason = reason,
    outcome = outcome,
    purpose = purpose,
    grantedMinutes = grantedMinutes,
    pauseSeconds = pauseSeconds,
    focusSessionId = focusSessionId
)

fun AccessGrantEntity.toDomain() = AccessGrant(
    packageName = packageName,
    expiresAt = Instant.ofEpochMilli(expiresAt)
)

fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAt),
    plannedMinutes = plannedMinutes,
    endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
    status = status
)
