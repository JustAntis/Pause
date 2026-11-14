package com.justantis.pause.presentation.block

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.friction.FrictionPolicy
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.streak.BypassConsequence
import com.justantis.pause.domain.usecase.GetAppUseCase
import com.justantis.pause.domain.usecase.GetBypassesTodayUseCase
import com.justantis.pause.domain.usecase.GetLimitUseCase
import com.justantis.pause.domain.usecase.GetRunningFocusSessionUseCase
import com.justantis.pause.domain.usecase.GetUsageOnDayUseCase
import com.justantis.pause.domain.usecase.GrantAccessUseCase
import com.justantis.pause.domain.usecase.ObserveAttemptCountsByAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/**
 * Shared by the three break screens. The pause countdown is computed from the clock and the moment
 * the pause started, so it keeps running across rotation; leaving the flow resets it.
 */
@HiltViewModel
class BlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getApp: GetAppUseCase,
    private val getLimit: GetLimitUseCase,
    private val getUsageOnDay: GetUsageOnDayUseCase,
    private val observeAttemptCountsByApp: ObserveAttemptCountsByAppUseCase,
    private val getBypassesToday: GetBypassesTodayUseCase,
    private val getRunningFocusSession: GetRunningFocusSessionUseCase,
    private val grantAccess: GrantAccessUseCase,
    private val frictionPolicy: FrictionPolicy,
    private val clock: Clock
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle[ARG_PACKAGE_NAME])
    private val eventId: Long = checkNotNull(savedStateHandle[ARG_EVENT_ID])
    private val reason: BlockReason = BlockReason.valueOf(checkNotNull(savedStateHandle[ARG_REASON]))
    private val focusSessionId: Long? =
        savedStateHandle.get<Long>(ARG_FOCUS_SESSION_ID)?.takeIf { it != NO_FOCUS_SESSION }

    private val _state = MutableStateFlow(
        BlockState(
            packageName = packageName,
            reason = reason,
            purposes = frictionPolicy.purposes,
            accessMinutesOptions = frictionPolicy.allowedAccessMinutes
        )
    )
    val state: StateFlow<BlockState> = _state.asStateFlow()

    private val _effects = Channel<BlockEffect>(Channel.BUFFERED)
    val effects: Flow<BlockEffect> = _effects.receiveAsFlow()

    private var pauseJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    fun onIntent(intent: BlockIntent) {
        when (intent) {
            BlockIntent.CloseClicked -> sendEffect(BlockEffect.GoHome)
            BlockIntent.NeedAccessClicked -> {
                resetPause()
                sendEffect(BlockEffect.OpenPause)
            }
            BlockIntent.PauseStarted -> startPause()
            BlockIntent.NextClicked -> if (_state.value.canContinue) sendEffect(BlockEffect.OpenPurpose)
            is BlockIntent.PurposeSelected -> _state.update { it.copy(selectedPurpose = intent.purpose) }
            is BlockIntent.MinutesSelected -> _state.update { it.copy(selectedMinutes = intent.minutes) }
            BlockIntent.ConfirmClicked -> confirm()
            BlockIntent.BackToStartClicked,
            BlockIntent.FlowInterrupted -> {
                resetPause()
                sendEffect(BlockEffect.BackToStart)
            }
        }
    }

    private suspend fun load() {
        val today = LocalDate.now(clock)
        val bypasses = getBypassesToday()
        val pauseSeconds = frictionPolicy.pauseSecondsFor(bypasses.total)
        val focusMinutesLeft = if (reason == BlockReason.FOCUS) {
            getRunningFocusSession()?.let { session ->
                val leftSeconds = Duration.between(clock.instant(), session.plannedEnd).seconds
                ((leftSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE).toInt().coerceAtLeast(0)
            }
        } else {
            null
        }
        val usedMs = getUsageOnDay(today).usageOf(packageName)?.foregroundMs ?: 0L

        _state.update {
            it.copy(
                isLoading = false,
                appLabel = getApp(packageName)?.label ?: packageName,
                limitMinutes = getLimit(packageName)?.limitMinutes,
                usedMinutes = Duration.ofMillis(usedMs).toMinutes().toInt(),
                focusMinutesLeft = focusMinutesLeft,
                attemptsToday = observeAttemptCountsByApp(today).first()[packageName] ?: 0,
                accessOrdinal = bypasses.total + 1,
                pauseSeconds = pauseSeconds,
                pauseSecondsLeft = pauseSeconds,
                consequence = BypassConsequence.of(
                    reason = reason,
                    limitBypassesToday = bypasses.limit,
                    currentStreak = it.streakDays,
                    freezes = it.freezes
                )
            )
        }
    }

    private fun startPause() {
        if (pauseJob?.isActive == true || _state.value.isPauseFinished) return
        pauseJob = viewModelScope.launch {
            val startedAt = clock.instant()
            val totalMs = Duration.ofSeconds(_state.value.pauseSeconds.toLong()).toMillis()
            while (true) {
                val leftMs = totalMs - Duration.between(startedAt, clock.instant()).toMillis()
                if (leftMs <= 0) {
                    _state.update { it.copy(pauseSecondsLeft = 0, isPauseFinished = true) }
                    break
                }
                val secondsLeft = (leftMs + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
                _state.update { it.copy(pauseSecondsLeft = secondsLeft.toInt()) }
                val untilNextSecond = (leftMs - 1) % MILLIS_PER_SECOND + 1
                delay(untilNextSecond)
            }
        }
    }

    private fun resetPause() {
        pauseJob?.cancel()
        pauseJob = null
        _state.update {
            it.copy(
                pauseSecondsLeft = it.pauseSeconds,
                isPauseFinished = false,
                selectedPurpose = null,
                selectedMinutes = null
            )
        }
    }

    private fun confirm() {
        val current = _state.value
        val purpose = current.selectedPurpose ?: return
        val minutes = current.selectedMinutes ?: return
        if (!current.canConfirm) return
        _state.update { it.copy(isGranting = true) }
        viewModelScope.launch {
            grantAccess(
                eventId = eventId,
                packageName = packageName,
                reason = reason,
                focusSessionId = focusSessionId,
                purpose = purpose,
                minutes = minutes,
                pauseSeconds = current.pauseSeconds
            )
            _effects.send(BlockEffect.ReturnToApp(packageName))
        }
    }

    private fun sendEffect(effect: BlockEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        const val ARG_PACKAGE_NAME = "packageName"
        const val ARG_EVENT_ID = "eventId"
        const val ARG_REASON = "reason"
        const val ARG_FOCUS_SESSION_ID = "focusSessionId"
        const val NO_FOCUS_SESSION = -1L

        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60L
    }
}
