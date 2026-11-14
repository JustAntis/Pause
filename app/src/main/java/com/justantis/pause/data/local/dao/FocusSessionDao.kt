package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.justantis.pause.data.local.entity.FocusSessionEntity
import com.justantis.pause.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_session WHERE id = :id")
    suspend fun get(id: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getRunning(): FocusSessionEntity?

    @Query("SELECT * FROM focus_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeRunning(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_session WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt")
    fun observeStartedBetween(from: Long, to: Long): Flow<List<FocusSessionEntity>>

    @Query("UPDATE focus_session SET status = :status WHERE id = :id AND status = :expected")
    suspend fun updateStatusIf(id: Long, expected: FocusSessionStatus, status: FocusSessionStatus)

    @Query("UPDATE focus_session SET status = :status, endedAt = :endedAt WHERE id = :id AND endedAt IS NULL")
    suspend fun finish(id: Long, endedAt: Long, status: FocusSessionStatus)
}
