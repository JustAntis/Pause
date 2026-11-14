package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_app")
data class FocusAppEntity(
    @PrimaryKey val packageName: String
)
