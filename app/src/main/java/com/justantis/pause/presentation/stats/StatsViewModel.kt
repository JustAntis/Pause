package com.justantis.pause.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetStatisticsUseCase
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getStatistics: GetStatisticsUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val getLaunchableApps: GetLaunchableAppsUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    private val _effects = Channel<StatsEffect>(Channel.BUFFERED)
    val effects: Flow<StatsEffect> = _effects.receiveAsFlow()

    private var appLabels: Map<String, String>? = null
    private var loadJob: Job? = null

    fun onIntent(intent: StatsIntent) {
        when (intent) {
            StatsIntent.Refresh -> load(_state.value.period)
            is StatsIntent.PeriodSelected -> {
                _state.update { it.copy(period = intent.period) }
                load(intent.period)
            }
            is StatsIntent.AppClicked -> viewModelScope.launch {
                _effects.send(StatsEffect.OpenAppDetails(intent.packageName))
            }
        }
    }

    private fun load(period: StatsPeriod) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val now = ZonedDateTime.now(clock)
            val labels = appLabels
                ?: getLaunchableApps().associate { it.packageName to it.label }.also { appLabels = it }
            val stats = getStatistics(period.domain)
            val goal = observeSettings().first().screenTimeGoalMinutes
            _state.value = StatsStateMapper.map(stats, goal, labels, now.toLocalDate(), now.hour)
        }
    }
}
