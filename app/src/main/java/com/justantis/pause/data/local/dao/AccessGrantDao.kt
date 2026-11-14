package com.justantis.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.justantis.pause.data.local.entity.AccessGrantEntity

@Dao
interface AccessGrantDao {
    @Query("SELECT * FROM access_grant WHERE packageName = :packageName")
    suspend fun get(packageName: String): AccessGrantEntity?

    @Upsert
    suspend fun upsert(grant: AccessGrantEntity)

    @Query("DELETE FROM access_grant WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long)
}
