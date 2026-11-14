package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.justantis.pause.domain.model.FocusSessionStatus

@Entity(tableName = "focus_session")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val plannedMinutes: Int,
    val endedAt: Long? = null,
    val status: FocusSessionStatus
)
