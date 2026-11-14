package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockOutcome
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity): Long

    @Query(
        "UPDATE block_event SET outcome = :outcome, purpose = :purpose, " +
            "grantedMinutes = :grantedMinutes, pauseSeconds = :pauseSeconds WHERE id = :id"
    )
    suspend fun updateOutcome(
        id: Long,
        outcome: BlockOutcome,
        purpose: AccessPurpose,
        grantedMinutes: Int,
        pauseSeconds: Int
    )

    @Query("SELECT * FROM block_event WHERE id = :id")
    suspend fun get(id: Long): BlockEventEntity?

    @Query("SELECT * FROM block_event WHERE date BETWEEN :from AND :to ORDER BY timestamp")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<BlockEventEntity>>

    @Query("SELECT COUNT(*) FROM block_event WHERE date = :date AND packageName = :packageName")
    fun observeAttemptCount(date: LocalDate, packageName: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_event WHERE date = :date AND outcome = :outcome")
    suspend fun countWithOutcome(date: LocalDate, outcome: BlockOutcome): Int

    @Query("SELECT COUNT(*) FROM block_event WHERE date = :date AND outcome = :outcome")
    fun observeCountWithOutcome(date: LocalDate, outcome: BlockOutcome): Flow<Int>
}
