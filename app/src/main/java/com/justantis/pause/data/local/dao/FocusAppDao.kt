package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.justantis.pause.data.local.entity.FocusAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusAppDao {
    @Query("SELECT packageName FROM focus_app")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM focus_app WHERE packageName = :packageName)")
    suspend fun contains(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(apps: List<FocusAppEntity>)

    @Query("DELETE FROM focus_app WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM focus_app")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(apps: List<FocusAppEntity>) {
        deleteAll()
        insert(apps)
    }
}
