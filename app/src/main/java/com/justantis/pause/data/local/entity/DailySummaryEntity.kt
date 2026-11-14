package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val date: LocalDate,
    val screenTimeMs: Long,
    val unlockCount: Int,
    val closed: Boolean,
    val tracked: Boolean
)
