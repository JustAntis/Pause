package com.justantis.pause.presentation.limitedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.usecase.DeleteLimitUseCase
import com.justantis.pause.domain.usecase.GetAppUseCase
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetLimitUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.SaveLimitUseCase
import com.justantis.pause.presentation.components.PickerApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LimitEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLimit: GetLimitUseCase,
    private val getApp: GetAppUseCase,
    private val getLaunchableApps: GetLaunchableAppsUseCase,
    private val observeLimits: ObserveLimitsUseCase,
    private val saveLimit: SaveLimitUseCase,
    private val deleteLimit: DeleteLimitUseCase
) : ViewModel() {

    private val packageName: String? = savedStateHandle[PACKAGE_NAME_ARG]

    private val _state = MutableStateFlow(LimitEditState())
    val state: StateFlow<LimitEditState> = _state.asStateFlow()

    private val _effects = Channel<LimitEditEffect>(Channel.BUFFERED)
    val effects: Flow<LimitEditEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun onIntent(intent: LimitEditIntent) {
        when (intent) {
            LimitEditIntent.AppRowClicked -> _state.update {
                if (it.isAppFixed) it else it.copy(isPickerVisible = true, pickerQuery = "")
            }
            is LimitEditIntent.PickerQueryChanged -> _state.update { it.copy(pickerQuery = intent.query) }
            is LimitEditIntent.AppPicked -> _state.update { state ->
                state.copy(
                    selectedApp = state.availableApps.firstOrNull { it.packageName == intent.packageName },
                    isPickerVisible = false
                )
            }
            LimitEditIntent.PickerDismissed -> _state.update { it.copy(isPickerVisible = false) }
            LimitEditIntent.DecreaseClicked -> changeLimit(-AppLimit.STEP_MINUTES)
            LimitEditIntent.IncreaseClicked -> changeLimit(AppLimit.STEP_MINUTES)
            is LimitEditIntent.PresetSelected -> _state.update { it.copy(limitMinutes = intent.minutes) }
            is LimitEditIntent.DayToggled -> toggleDay(intent)
            LimitEditIntent.SaveClicked -> save()
            LimitEditIntent.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            LimitEditIntent.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            LimitEditIntent.DeleteConfirmed -> delete()
            LimitEditIntent.BackClicked -> close()
        }
    }

    private suspend fun load() {
        if (packageName == null) {
            val limited = observeLimits().first().map { it.packageName }.toSet()
            val apps = getLaunchableApps()
                .filter { it.packageName !in limited }
                .map { PickerApp(it.packageName, it.label) }
            _state.update { it.copy(isLoading = false, availableApps = apps) }
            return
        }

        val app = PickerApp(packageName, getApp(packageName)?.label ?: packageName)
        val limit = getLimit(packageName)
        _state.update {
            it.copy(
                isLoading = false,
                isEditMode = limit != null,
                isAppFixed = true,
                selectedApp = app,
                limitMinutes = limit?.limitMinutes ?: it.limitMinutes,
                activeDays = limit?.activeDays ?: it.activeDays
            )
        }
    }

    private fun changeLimit(delta: Int) = _state.update {
        it.copy(limitMinutes = (it.limitMinutes + delta).coerceIn(AppLimit.MIN_MINUTES, AppLimit.MAX_MINUTES))
    }

    private fun toggleDay(intent: LimitEditIntent.DayToggled) = _state.update { state ->
        val days = if (intent.day in state.activeDays) state.activeDays - intent.day else state.activeDays + intent.day
        if (days.isEmpty()) state else state.copy(activeDays = days)
    }

    private fun save() {
        val current = _state.value
        val app = current.selectedApp ?: return
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveLimit(app.packageName, current.limitMinutes, current.activeDays)
            _effects.send(LimitEditEffect.Close)
        }
    }

    private fun delete() {
        val app = _state.value.selectedApp ?: return
        _state.update { it.copy(isDeleteDialogVisible = false, isSaving = true) }
        viewModelScope.launch {
            deleteLimit(app.packageName)
            _effects.send(LimitEditEffect.Close)
        }
    }

    private fun close() {
        viewModelScope.launch { _effects.send(LimitEditEffect.Close) }
    }

    companion object {
        /** Name of the route argument in [com.justantis.pause.presentation.navigation.LimitEditDestination]. */
        const val PACKAGE_NAME_ARG = "packageName"
    }
}
