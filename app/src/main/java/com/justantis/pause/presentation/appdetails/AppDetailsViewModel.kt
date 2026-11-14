package com.justantis.pause.presentation.appdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.usecase.GetAppDetailsUseCase
import com.justantis.pause.domain.usecase.SetPausedInFocusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAppDetails: GetAppDetailsUseCase,
    private val setPausedInFocus: SetPausedInFocusUseCase
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle[PACKAGE_NAME_ARG])

    private val _state = MutableStateFlow(AppDetailsState(packageName = packageName))
    val state: StateFlow<AppDetailsState> = _state.asStateFlow()

    private val _effects = Channel<AppDetailsEffect>(Channel.BUFFERED)
    val effects: Flow<AppDetailsEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    fun onIntent(intent: AppDetailsIntent) {
        when (intent) {
            AppDetailsIntent.Refresh -> load()
            AppDetailsIntent.BackClicked -> sendEffect(AppDetailsEffect.Close)
            AppDetailsIntent.LimitClicked -> sendEffect(AppDetailsEffect.OpenLimitEdit(packageName))
            is AppDetailsIntent.PausedInFocusChanged -> {
                _state.update { it.copy(pausedInFocus = intent.paused) }
                viewModelScope.launch { setPausedInFocus(packageName, intent.paused) }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = AppDetailsStateMapper.map(getAppDetails(packageName))
        }
    }

    private fun sendEffect(effect: AppDetailsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        /** Name of the route argument in [com.justantis.pause.presentation.navigation.AppDetailsDestination]. */
        const val PACKAGE_NAME_ARG = "packageName"
    }
}
