package com.justantis.pause.data.repository

import com.justantis.pause.data.local.dao.LimitDao
import com.justantis.pause.data.local.toDomain
import com.justantis.pause.data.local.toEntity
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.repository.LimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LimitRepositoryImpl @Inject constructor(
    private val limitDao: LimitDao
) : LimitRepository {

    override fun observeLimits(): Flow<List<AppLimit>> =
        limitDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeLimit(packageName: String): Flow<AppLimit?> =
        limitDao.observe(packageName).map { it?.toDomain() }

    override suspend fun getLimits(): List<AppLimit> = limitDao.getAll().map { it.toDomain() }

    override suspend fun getLimit(packageName: String): AppLimit? = limitDao.get(packageName)?.toDomain()

    override suspend fun saveLimit(limit: AppLimit) = limitDao.upsert(limit.toEntity())

    override suspend fun deleteLimit(packageName: String) = limitDao.delete(packageName)
}
