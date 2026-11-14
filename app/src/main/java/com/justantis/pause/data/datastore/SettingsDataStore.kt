package com.justantis.pause.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.justantis.pause.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            onboardingCompletedOn = preferences[ONBOARDING_COMPLETED_ON]?.let { LocalDate.ofEpochDay(it) },
            userName = preferences[USER_NAME],
            screenTimeGoalMinutes = preferences[SCREEN_TIME_GOAL_MINUTES]
                ?: UserSettings.DEFAULT_SCREEN_TIME_GOAL_MINUTES,
            unlockGoal = preferences[UNLOCK_GOAL] ?: UserSettings.DEFAULT_UNLOCK_GOAL,
            notifyBeforeLimit = preferences[NOTIFY_BEFORE_LIMIT] ?: true,
            notifyWeeklySummary = preferences[NOTIFY_WEEKLY_SUMMARY] ?: false
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = set(ONBOARDING_COMPLETED, completed)

    suspend fun setOnboardingCompletedOn(date: LocalDate) = set(ONBOARDING_COMPLETED_ON, date.toEpochDay())

    suspend fun setUserName(name: String?) {
        dataStore.edit { preferences ->
            if (name.isNullOrBlank()) preferences.remove(USER_NAME) else preferences[USER_NAME] = name.trim()
        }
    }

    suspend fun setScreenTimeGoalMinutes(minutes: Int) = set(SCREEN_TIME_GOAL_MINUTES, minutes)

    suspend fun setUnlockGoal(unlocks: Int) = set(UNLOCK_GOAL, unlocks)

    suspend fun setNotifyBeforeLimit(enabled: Boolean) = set(NOTIFY_BEFORE_LIMIT, enabled)

    suspend fun setNotifyWeeklySummary(enabled: Boolean) = set(NOTIFY_WEEKLY_SUMMARY, enabled)

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    companion object {
        const val FILE_NAME = "settings"

        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboardingCompleted")
        private val ONBOARDING_COMPLETED_ON = longPreferencesKey("onboardingCompletedOn")
        private val USER_NAME = stringPreferencesKey("userName")
        private val SCREEN_TIME_GOAL_MINUTES = intPreferencesKey("screenTimeGoalMinutes")
        private val UNLOCK_GOAL = intPreferencesKey("unlockGoal")
        private val NOTIFY_BEFORE_LIMIT = booleanPreferencesKey("notifyBeforeLimit")
        private val NOTIFY_WEEKLY_SUMMARY = booleanPreferencesKey("notifyWeeklySummary")
    }
}
