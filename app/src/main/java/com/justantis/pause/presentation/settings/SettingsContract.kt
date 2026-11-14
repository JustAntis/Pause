package com.justantis.pause.presentation.settings

import com.justantis.pause.domain.friction.FrictionConfig
import com.justantis.pause.domain.streak.StreakConfig

data class SettingsState(
    val isLoading: Boolean = true,
    val pauseSeconds: List<Int> = FrictionConfig.DEFAULT.pauseSeconds,
    val accessMinutes: List<Int> = FrictionConfig.DEFAULT.accessMinutes,
    val freezeEveryDays: Int = StreakConfig.FREEZE_EVERY_DAYS,
    val maxFreezes: Int = StreakConfig.MAX_FREEZES,
    val notifyBeforeLimit: Boolean = true,
    val notifyWeeklySummary: Boolean = false,
    val notificationsAllowed: Boolean = true,
    val usageAccessOn: Boolean = false,
    val accessibilityOn: Boolean = false,
    val userName: String? = null,
    val nameDraft: String? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false
)

sealed interface SettingsIntent {
    data object Refresh : SettingsIntent
    data class LimitWarningToggled(val enabled: Boolean) : SettingsIntent
    data class WeeklySummaryToggled(val enabled: Boolean) : SettingsIntent
    data object UsageAccessClicked : SettingsIntent
    data object AccessibilityClicked : SettingsIntent
    data object EditNameClicked : SettingsIntent
    data class NameChanged(val name: String) : SettingsIntent
    data object NameConfirmed : SettingsIntent
    data object NameDismissed : SettingsIntent
    data object DeleteClicked : SettingsIntent
    data object DeleteConfirmed : SettingsIntent
    data object DeleteDismissed : SettingsIntent
    data object BackClicked : SettingsIntent
}

sealed interface SettingsEffect {
    data object Close : SettingsEffect
    data object OpenUsageAccessSettings : SettingsEffect
    data object OpenAccessibilitySettings : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object DataDeleted : SettingsEffect
}
