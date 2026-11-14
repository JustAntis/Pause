package com.justantis.pause.data.repository

import com.justantis.pause.data.usage.InstalledAppsDataSource
import com.justantis.pause.di.IoDispatcher
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.repository.AppCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppCatalogRepositoryImpl @Inject constructor(
    private val installedApps: InstalledAppsDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppCatalogRepository {

    override suspend fun launchableApps(): List<InstalledApp> =
        withContext(ioDispatcher) { installedApps.launchableApps() }

    override suspend fun app(packageName: String): InstalledApp? =
        withContext(ioDispatcher) { installedApps.app(packageName) }
}
