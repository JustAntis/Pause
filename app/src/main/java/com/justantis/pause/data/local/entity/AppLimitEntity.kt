package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limit")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val limitMinutes: Int,
    val daysMask: Int,
    val enabled: Boolean,
    val createdAt: Long
)
