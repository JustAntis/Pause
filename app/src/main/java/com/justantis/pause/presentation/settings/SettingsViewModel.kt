package com.justantis.pause.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.usecase.DeleteAllDataUseCase
import com.justantis.pause.domain.usecase.GetPermissionStatusUseCase
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import com.justantis.pause.domain.usecase.SetLimitWarningEnabledUseCase
import com.justantis.pause.domain.usecase.SetUserNameUseCase
import com.justantis.pause.domain.usecase.SetWeeklySummaryEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Turning a notification switch on asks for the notification permission when it is missing
 * (spec §8: in context, not during onboarding). The switches work either way.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val setLimitWarningEnabled: SetLimitWarningEnabledUseCase,
    private val setWeeklySummaryEnabled: SetWeeklySummaryEnabledUseCase,
    private val setUserName: SetUserNameUseCase,
    private val deleteAllData: DeleteAllDataUseCase
) : ViewModel() {

    private data class UiState(
        val permissions: PermissionStatus? = null,
        val nameDraft: String? = null,
        val isDeleteDialogVisible: Boolean = false,
        val isDeleting: Boolean = false
    )

    private val ui = MutableStateFlow(UiState())

    val state: StateFlow<SettingsState> = combine(observeSettings(), ui) { settings, ui ->
        SettingsState(
            isLoading = ui.permissions == null,
            notifyBeforeLimit = settings.notifyBeforeLimit,
            notifyWeeklySummary = settings.notifyWeeklySummary,
            notificationsAllowed = ui.permissions?.notifications ?: true,
            usageAccessOn = ui.permissions?.usageAccess ?: false,
            accessibilityOn = ui.permissions?.accessibilityService ?: false,
            userName = settings.userName,
            nameDraft = ui.nameDraft,
            isDeleteDialogVisible = ui.isDeleteDialogVisible,
            isDeleting = ui.isDeleting
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Refresh -> ui.update { it.copy(permissions = getPermissionStatus()) }
            is SettingsIntent.LimitWarningToggled -> viewModelScope.launch {
                setLimitWarningEnabled(intent.enabled)
                askForNotificationsIfNeeded(intent.enabled)
            }
            is SettingsIntent.WeeklySummaryToggled -> viewModelScope.launch {
                setWeeklySummaryEnabled(intent.enabled)
                askForNotificationsIfNeeded(intent.enabled)
            }
            SettingsIntent.UsageAccessClicked -> send(SettingsEffect.OpenUsageAccessSettings)
            SettingsIntent.AccessibilityClicked -> send(SettingsEffect.OpenAccessibilitySettings)
            SettingsIntent.EditNameClicked -> ui.update { it.copy(nameDraft = state.value.userName.orEmpty()) }
            is SettingsIntent.NameChanged -> ui.update { it.copy(nameDraft = intent.name) }
            SettingsIntent.NameConfirmed -> {
                val name = ui.value.nameDraft
                ui.update { it.copy(nameDraft = null) }
                viewModelScope.launch { setUserName(name) }
            }
            SettingsIntent.NameDismissed -> ui.update { it.copy(nameDraft = null) }
            SettingsIntent.DeleteClicked -> ui.update { it.copy(isDeleteDialogVisible = true) }
            SettingsIntent.DeleteDismissed -> ui.update { it.copy(isDeleteDialogVisible = false) }
            SettingsIntent.DeleteConfirmed -> {
                ui.update { it.copy(isDeleteDialogVisible = false, isDeleting = true) }
                viewModelScope.launch {
                    deleteAllData()
                    _effects.send(SettingsEffect.DataDeleted)
                }
            }
            SettingsIntent.BackClicked -> send(SettingsEffect.Close)
        }
    }

    private suspend fun askForNotificationsIfNeeded(enabled: Boolean) {
        if (enabled && !getPermissionStatus().notifications) _effects.send(SettingsEffect.RequestNotificationPermission)
    }

    private fun send(effect: SettingsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
