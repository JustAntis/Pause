package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.repository.AppCatalogRepository
import javax.inject.Inject

/**
 * Name and category of one installed app, or null when it is no longer installed.
 */
class GetAppUseCase @Inject constructor(
    private val appCatalogRepository: AppCatalogRepository
) {
    suspend operator fun invoke(packageName: String): InstalledApp? = appCatalogRepository.app(packageName)
}
