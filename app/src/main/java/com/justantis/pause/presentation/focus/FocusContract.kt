package com.justantis.pause.presentation.focus

import com.justantis.pause.domain.focus.FocusConfig
import com.justantis.pause.presentation.components.PickerApp

enum class FocusPhase { IDLE, RUNNING, FINISHED }

data class FocusState(
    val isLoading: Boolean = true,
    val phase: FocusPhase = FocusPhase.IDLE,
    val selectedMinutes: Int = FocusConfig.DEFAULT_MINUTES,
    val durations: List<Int> = FocusConfig.DURATIONS_MINUTES,
    val pausedApps: List<PickerApp> = emptyList(),
    val totalSeconds: Int = 0,
    val secondsLeft: Int = 0,
    val withBreaks: Boolean = false,
    val cleanToday: Int = 0,
    val withBreaksToday: Int = 0,
    val isPickerVisible: Boolean = false,
    val pickerQuery: String = "",
    val pickerApps: List<PickerApp> = emptyList(),
    val pickerSelection: Set<String> = emptySet(),
    val isEndDialogVisible: Boolean = false
) {
    val canStart: Boolean
        get() = !isLoading && phase == FocusPhase.IDLE && pausedApps.isNotEmpty()

    val progress: Float
        get() = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 1f

    val filteredPickerApps: List<PickerApp>
        get() = if (pickerQuery.isBlank()) {
            pickerApps
        } else {
            pickerApps.filter { it.label.contains(pickerQuery.trim(), ignoreCase = true) }
        }
}

sealed interface FocusIntent {
    data class DurationSelected(val minutes: Int) : FocusIntent
    data object ChangeAppsClicked : FocusIntent
    data class PickerQueryChanged(val query: String) : FocusIntent
    data class PickerAppToggled(val packageName: String) : FocusIntent
    data object PickerDone : FocusIntent
    data object PickerDismissed : FocusIntent
    data object StartClicked : FocusIntent
    data object EndClicked : FocusIntent
    data object EndConfirmed : FocusIntent
    data object EndDismissed : FocusIntent
    data object FinishedDismissed : FocusIntent
    data object BackClicked : FocusIntent
}

sealed interface FocusEffect {
    data object Close : FocusEffect
    data object RequestNotificationPermission : FocusEffect
}
