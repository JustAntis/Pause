package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.justantis.pause.data.local.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitDao {
    @Query("SELECT * FROM app_limit ORDER BY createdAt")
    fun observeAll(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limit WHERE packageName = :packageName")
    fun observe(packageName: String): Flow<AppLimitEntity?>

    @Query("SELECT * FROM app_limit ORDER BY createdAt")
    suspend fun getAll(): List<AppLimitEntity>

    @Query("SELECT * FROM app_limit WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppLimitEntity?

    @Upsert
    suspend fun upsert(limit: AppLimitEntity)

    @Query("DELETE FROM app_limit WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
