package com.justantis.pause.presentation.limitedit

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.presentation.components.PickerApp
import java.time.DayOfWeek

data class LimitEditState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val isAppFixed: Boolean = false,
    val selectedApp: PickerApp? = null,
    val limitMinutes: Int = DEFAULT_LIMIT_MINUTES,
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val availableApps: List<PickerApp> = emptyList(),
    val isPickerVisible: Boolean = false,
    val pickerQuery: String = "",
    val isDeleteDialogVisible: Boolean = false,
    val isSaving: Boolean = false
) {
    val canSave: Boolean
        get() = selectedApp != null && !isLoading && !isSaving

    val canDecrease: Boolean
        get() = limitMinutes > AppLimit.MIN_MINUTES

    val canIncrease: Boolean
        get() = limitMinutes < AppLimit.MAX_MINUTES

    val filteredApps: List<PickerApp>
        get() = if (pickerQuery.isBlank()) {
            availableApps
        } else {
            availableApps.filter { it.label.contains(pickerQuery.trim(), ignoreCase = true) }
        }

    companion object {
        const val DEFAULT_LIMIT_MINUTES = 30
        val PRESETS_MINUTES = listOf(15, 30, 45, 60)
    }
}

sealed interface LimitEditIntent {
    data object AppRowClicked : LimitEditIntent
    data class PickerQueryChanged(val query: String) : LimitEditIntent
    data class AppPicked(val packageName: String) : LimitEditIntent
    data object PickerDismissed : LimitEditIntent
    data object DecreaseClicked : LimitEditIntent
    data object IncreaseClicked : LimitEditIntent
    data class PresetSelected(val minutes: Int) : LimitEditIntent
    data class DayToggled(val day: DayOfWeek) : LimitEditIntent
    data object SaveClicked : LimitEditIntent
    data object DeleteClicked : LimitEditIntent
    data object DeleteConfirmed : LimitEditIntent
    data object DeleteDismissed : LimitEditIntent
    data object BackClicked : LimitEditIntent
}

sealed interface LimitEditEffect {
    data object Close : LimitEditEffect
}
