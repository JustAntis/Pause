package com.justantis.pause.presentation.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetUsageOnDayUseCase
import com.justantis.pause.domain.usecase.ObserveAttemptCountsByAppUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LimitsViewModel @Inject constructor(
    observeLimits: ObserveLimitsUseCase,
    private val observeAttemptCountsByApp: ObserveAttemptCountsByAppUseCase,
    private val getUsageOnDay: GetUsageOnDayUseCase,
    private val getLaunchableApps: GetLaunchableAppsUseCase,
    private val clock: Clock
) : ViewModel() {

    private val snapshot = MutableStateFlow<LimitsSnapshot?>(null)
    private var appLabels: Map<String, String>? = null
    private var refreshJob: Job? = null

    private val attemptsByApp: Flow<Map<String, Int>> = snapshot
        .filterNotNull()
        .map { it.date }
        .distinctUntilChanged()
        .flatMapLatest { observeAttemptCountsByApp(it) }

    val state: StateFlow<LimitsState> = combine(
        observeLimits(),
        snapshot.filterNotNull(),
        attemptsByApp
    ) { limits, today, attempts ->
        LimitsStateMapper.map(limits, today, attempts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LimitsState()
    )

    private val _effects = Channel<LimitsEffect>(Channel.BUFFERED)
    val effects: Flow<LimitsEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: LimitsIntent) {
        when (intent) {
            LimitsIntent.Refresh -> refresh()
            LimitsIntent.AddLimitClicked -> sendEffect(LimitsEffect.OpenLimitEdit(packageName = null))
            is LimitsIntent.LimitClicked -> sendEffect(LimitsEffect.OpenLimitEdit(intent.packageName))
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val today = LocalDate.now(clock)
            val usage = getUsageOnDay(today)
            val labels = appLabels
                ?: getLaunchableApps().associate { it.packageName to it.label }.also { appLabels = it }
            snapshot.value = LimitsSnapshot(today, usage, labels)
        }
    }

    private fun sendEffect(effect: LimitsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
