package com.justantis.pause.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetPermissionStatusUseCase
import com.justantis.pause.domain.usecase.GetUsageOnDayUseCase
import com.justantis.pause.domain.usecase.ObserveAttemptCountUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import com.justantis.pause.domain.usecase.ObserveStreakUseCase
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
import java.time.ZonedDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    observeLimits: ObserveLimitsUseCase,
    private val observeAttemptCount: ObserveAttemptCountUseCase,
    private val observeStreak: ObserveStreakUseCase,
    private val getUsageOnDay: GetUsageOnDayUseCase,
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val getLaunchableApps: GetLaunchableAppsUseCase,
    private val clock: Clock
) : ViewModel() {

    private val snapshot = MutableStateFlow<TodaySnapshot?>(null)
    private var appLabels: Map<String, String>? = null
    private var refreshJob: Job? = null

    private val today: Flow<LocalDate> = snapshot
        .filterNotNull()
        .map { it.date }
        .distinctUntilChanged()

    val state: StateFlow<HomeState> = combine(
        observeSettings(),
        observeLimits(),
        snapshot.filterNotNull(),
        today.flatMapLatest { observeAttemptCount(it) },
        today.flatMapLatest { observeStreak(it) }
    ) { settings, limits, todaySnapshot, attempts, streak ->
        HomeStateMapper.map(settings, limits, todaySnapshot, attempts, streak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeState(date = LocalDate.now(clock))
    )

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh()
            is HomeIntent.AppClicked -> sendEffect(HomeEffect.OpenAppDetails(intent.packageName))
            HomeIntent.StartSessionClicked -> sendEffect(HomeEffect.OpenFocus)
            HomeIntent.FixUsageAccess -> sendEffect(HomeEffect.OpenUsageAccessSettings)
            HomeIntent.FixAccessibility -> sendEffect(HomeEffect.OpenAccessibilitySettings)
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val now = ZonedDateTime.now(clock)
            val permissions = getPermissionStatus()
            val usage = if (permissions.usageAccess) getUsageOnDay(now.toLocalDate()) else UsageSummary.EMPTY
            val labels = appLabels
                ?: getLaunchableApps().associate { it.packageName to it.label }.also { appLabels = it }
            snapshot.value = TodaySnapshot(
                date = now.toLocalDate(),
                hour = now.hour,
                usage = usage,
                appLabels = labels,
                permissions = permissions
            )
        }
    }

    private fun sendEffect(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
