package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.repository.AppCatalogRepository
import javax.inject.Inject

/**
 * Apps the user can open from the launcher, sorted by name. Pause itself and the home launcher are
 * not included, so they never appear in app lists or pickers.
 */
class GetLaunchableAppsUseCase @Inject constructor(
    private val appCatalogRepository: AppCatalogRepository
) {
    suspend operator fun invoke(): List<InstalledApp> = appCatalogRepository.launchableApps()
}
