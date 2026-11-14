package com.justantis.pause.data.local.entity

import androidx.room.Entity
import java.time.LocalDate

@Entity(tableName = "daily_app_usage", primaryKeys = ["date", "packageName"])
data class DailyAppUsageEntity(
    val date: LocalDate,
    val packageName: String,
    val foregroundMs: Long,
    val openCount: Int
)
