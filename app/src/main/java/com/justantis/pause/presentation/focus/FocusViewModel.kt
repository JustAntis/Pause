package com.justantis.pause.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.usecase.EndFocusSessionEarlyUseCase
import com.justantis.pause.domain.usecase.FinishExpiredFocusSessionUseCase
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetPermissionStatusUseCase
import com.justantis.pause.domain.usecase.ObserveFocusAppsUseCase
import com.justantis.pause.domain.usecase.ObserveFocusSessionsUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.ObserveRunningFocusSessionUseCase
import com.justantis.pause.domain.usecase.StartFocusSessionUseCase
import com.justantis.pause.presentation.components.PickerApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * The countdown is always computed from the stored start and planned end of the running session,
 * never from an in-memory counter, so it is correct after the app process was killed. When the
 * planned end passes while the screen is open, the session is finished here. Starting a session
 * asks for the notification permission when it is missing, for the ongoing countdown notification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusViewModel @Inject constructor(
    observeRunningFocusSession: ObserveRunningFocusSessionUseCase,
    observeFocusSessions: ObserveFocusSessionsUseCase,
    observeFocusApps: ObserveFocusAppsUseCase,
    observeLimits: ObserveLimitsUseCase,
    private val getLaunchableApps: GetLaunchableAppsUseCase,
    private val startFocusSession: StartFocusSessionUseCase,
    private val finishExpiredFocusSession: FinishExpiredFocusSessionUseCase,
    private val endFocusSessionEarly: EndFocusSessionEarlyUseCase,
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val clock: Clock
) : ViewModel() {

    /** What exists only on this screen: choices not saved yet, dialogs, the shown result. */
    private data class UiState(
        val selectedMinutes: Int,
        val selection: Set<String>? = null,
        val isPickerVisible: Boolean = false,
        val pickerQuery: String = "",
        val pickerSelection: Set<String> = emptySet(),
        val isEndDialogVisible: Boolean = false,
        val finishedSessionId: Long? = null
    )

    private val ui = MutableStateFlow(UiState(selectedMinutes = FocusState().selectedMinutes))
    private var lastRunningId: Long? = null

    private val labels: Flow<List<PickerApp>> = flow {
        emit(getLaunchableApps().map { PickerApp(it.packageName, it.label) })
    }

    private val running: Flow<FocusSession?> = observeRunningFocusSession().onEach { session ->
        val previous = lastRunningId
        if (previous != null && session?.id != previous) ui.update { it.copy(finishedSessionId = previous) }
        lastRunningId = session?.id
    }

    private val now: Flow<Instant> = running.flatMapLatest { session ->
        if (session == null) {
            flowOf(clock.instant())
        } else {
            flow {
                while (true) {
                    val now = clock.instant()
                    emit(now)
                    if (!now.isBefore(session.plannedEnd)) {
                        finishExpiredFocusSession()
                        break
                    }
                    delay(MILLIS_PER_SECOND - now.toEpochMilli() % MILLIS_PER_SECOND)
                }
            }
        }
    }

    private val data = combine(
        running,
        observeFocusSessions(LocalDate.now(clock)),
        observeFocusApps(),
        observeLimits(),
        labels
    ) { session, sessionsToday, focusApps, limits, apps ->
        Data(session, sessionsToday, focusApps, limits, apps)
    }

    val state: StateFlow<FocusState> = combine(data, ui, now) { data, ui, now -> toState(data, ui, now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusState())

    private val _effects = Channel<FocusEffect>(Channel.BUFFERED)
    val effects: Flow<FocusEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: FocusIntent) {
        when (intent) {
            is FocusIntent.DurationSelected -> ui.update { it.copy(selectedMinutes = intent.minutes) }
            FocusIntent.ChangeAppsClicked -> ui.update {
                it.copy(
                    isPickerVisible = true,
                    pickerQuery = "",
                    pickerSelection = state.value.pausedApps.map { app -> app.packageName }.toSet()
                )
            }
            is FocusIntent.PickerQueryChanged -> ui.update { it.copy(pickerQuery = intent.query) }
            is FocusIntent.PickerAppToggled -> ui.update {
                val selection = it.pickerSelection
                it.copy(
                    pickerSelection = if (intent.packageName in selection) {
                        selection - intent.packageName
                    } else {
                        selection + intent.packageName
                    }
                )
            }
            FocusIntent.PickerDone -> ui.update { it.copy(selection = it.pickerSelection, isPickerVisible = false) }
            FocusIntent.PickerDismissed -> ui.update { it.copy(isPickerVisible = false) }
            FocusIntent.StartClicked -> start()
            FocusIntent.EndClicked -> ui.update { it.copy(isEndDialogVisible = true) }
            FocusIntent.EndDismissed -> ui.update { it.copy(isEndDialogVisible = false) }
            FocusIntent.EndConfirmed -> {
                ui.update { it.copy(isEndDialogVisible = false) }
                viewModelScope.launch { endFocusSessionEarly() }
            }
            FocusIntent.FinishedDismissed -> ui.update { it.copy(finishedSessionId = null) }
            FocusIntent.BackClicked -> viewModelScope.launch { _effects.send(FocusEffect.Close) }
        }
    }

    private fun start() {
        val current = state.value
        if (current.phase != FocusPhase.IDLE || current.isLoading) return
        val minutes = ui.value.selectedMinutes
        val apps = ui.value.selection ?: current.pausedApps.map { it.packageName }.toSet()
        if (apps.isEmpty()) return
        ui.update { it.copy(selection = null, finishedSessionId = null) }
        viewModelScope.launch {
            startFocusSession(minutes, apps)
            if (!getPermissionStatus().notifications) _effects.send(FocusEffect.RequestNotificationPermission)
        }
    }

    private data class Data(
        val running: FocusSession?,
        val sessionsToday: List<FocusSession>,
        val focusApps: Set<String>,
        val limits: List<AppLimit>,
        val apps: List<PickerApp>
    )

    private fun toState(data: Data, ui: UiState, now: Instant): FocusState {
        val labelByPackage = data.apps.associate { it.packageName to it.label }
        val defaultSelection = data.focusApps.ifEmpty { data.limits.map { it.packageName }.toSet() }
        val selection = ui.selection ?: defaultSelection
        val finished = data.sessionsToday.firstOrNull { it.id == ui.finishedSessionId && it.status != FocusSessionStatus.CANCELLED }
        val session = data.running

        val phase = when {
            session != null -> FocusPhase.RUNNING
            finished != null -> FocusPhase.FINISHED
            else -> FocusPhase.IDLE
        }
        val shownApps = if (session != null) data.focusApps else selection
        val totalSeconds = (session?.plannedMinutes ?: ui.selectedMinutes) * SECONDS_PER_MINUTE

        return FocusState(
            isLoading = false,
            phase = phase,
            selectedMinutes = session?.plannedMinutes ?: finished?.plannedMinutes ?: ui.selectedMinutes,
            pausedApps = shownApps
                .map { PickerApp(it, labelByPackage[it] ?: it) }
                .sortedBy { it.label.lowercase() },
            totalSeconds = totalSeconds,
            secondsLeft = when (phase) {
                FocusPhase.RUNNING -> secondsUntil(now, session!!.plannedEnd)
                FocusPhase.FINISHED -> 0
                FocusPhase.IDLE -> totalSeconds
            },
            withBreaks = (session ?: finished)?.status == FocusSessionStatus.INTERRUPTED,
            cleanToday = data.sessionsToday.count { !it.isRunning && it.status == FocusSessionStatus.CLEAN },
            withBreaksToday = data.sessionsToday.count { !it.isRunning && it.status == FocusSessionStatus.INTERRUPTED },
            isPickerVisible = ui.isPickerVisible,
            pickerQuery = ui.pickerQuery,
            pickerApps = data.apps,
            pickerSelection = ui.pickerSelection,
            isEndDialogVisible = ui.isEndDialogVisible
        )
    }

    private fun secondsUntil(now: Instant, end: Instant): Int {
        val leftMs = Duration.between(now, end).toMillis().coerceAtLeast(0)
        return ((leftMs + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val SECONDS_PER_MINUTE = 60
    }
}
