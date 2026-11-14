package com.justantis.pause.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import java.time.LocalDate

@Entity(tableName = "block_event", indices = [Index("date")])
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val date: LocalDate,
    val packageName: String,
    val reason: BlockReason,
    val outcome: BlockOutcome,
    val purpose: AccessPurpose? = null,
    val grantedMinutes: Int? = null,
    val pauseSeconds: Int? = null,
    val focusSessionId: Long? = null
)
