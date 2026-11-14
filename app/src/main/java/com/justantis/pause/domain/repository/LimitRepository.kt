package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.AppLimit
import kotlinx.coroutines.flow.Flow

/**
 * Daily app limits, at most one per app.
 */
interface LimitRepository {
    fun observeLimits(): Flow<List<AppLimit>>

    fun observeLimit(packageName: String): Flow<AppLimit?>

    suspend fun getLimits(): List<AppLimit>

    suspend fun getLimit(packageName: String): AppLimit?

    suspend fun saveLimit(limit: AppLimit)

    suspend fun deleteLimit(packageName: String)
}
