package com.justantis.pause.data.repository

import com.justantis.pause.data.datastore.SettingsDataStore
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<UserSettings> = settingsDataStore.settings

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        settingsDataStore.setOnboardingCompleted(completed)

    override suspend fun setOnboardingCompletedOn(date: LocalDate) =
        settingsDataStore.setOnboardingCompletedOn(date)

    override suspend fun setUserName(name: String?) = settingsDataStore.setUserName(name)

    override suspend fun setScreenTimeGoalMinutes(minutes: Int) =
        settingsDataStore.setScreenTimeGoalMinutes(minutes)

    override suspend fun setUnlockGoal(unlocks: Int) = settingsDataStore.setUnlockGoal(unlocks)

    override suspend fun setNotifyBeforeLimit(enabled: Boolean) =
        settingsDataStore.setNotifyBeforeLimit(enabled)

    override suspend fun setNotifyWeeklySummary(enabled: Boolean) =
        settingsDataStore.setNotifyWeeklySummary(enabled)
}
