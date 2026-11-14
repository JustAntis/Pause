package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_grant")
data class AccessGrantEntity(
    @PrimaryKey val packageName: String,
    val expiresAt: Long
)
