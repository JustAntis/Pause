package com.justantis.pause.data.repository

import com.justantis.pause.data.datastore.SettingsDataStore
import com.justantis.pause.data.local.PauseDatabase
import com.justantis.pause.di.IoDispatcher
import com.justantis.pause.domain.repository.DataWiper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalDataWiper @Inject constructor(
    private val database: PauseDatabase,
    private val settingsDataStore: SettingsDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DataWiper {

    override suspend fun wipe() = withContext(ioDispatcher) {
        database.clearAllTables()
        settingsDataStore.clear()
        settingsDataStore.setHistoryBackfilled()
    }
}
