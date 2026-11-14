package com.justantis.pause.domain.repository

import com.justantis.pause.domain.model.InstalledApp

/**
 * Apps installed on the device that the user can limit or pause.
 */
interface AppCatalogRepository {
    /** Launchable apps sorted by name, without Pause itself and the home launcher. */
    suspend fun launchableApps(): List<InstalledApp>

    /** Any installed app, launchable or not; null when it is not installed. */
    suspend fun app(packageName: String): InstalledApp?
}
