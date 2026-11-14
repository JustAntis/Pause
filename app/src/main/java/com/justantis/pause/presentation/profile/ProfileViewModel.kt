package com.justantis.pause.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import com.justantis.pause.domain.usecase.ObserveStreakUseCase
import com.justantis.pause.domain.usecase.SetScreenTimeGoalUseCase
import com.justantis.pause.domain.usecase.SetUnlockGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeStreak: ObserveStreakUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val setScreenTimeGoal: SetScreenTimeGoalUseCase,
    private val setUnlockGoal: SetUnlockGoalUseCase,
    private val clock: Clock
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))
    private val goalDialog = MutableStateFlow<GoalDialog?>(null)

    val state: StateFlow<ProfileState> = combine(
        today.flatMapLatest { date -> observeStreak(date) },
        observeSettings(),
        today,
        goalDialog
    ) { streak, settings, date, dialog ->
        ProfileStateMapper.map(streak, settings, date).copy(goalDialog = dialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileState()
    )

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh -> today.value = LocalDate.now(clock)
            ProfileIntent.SettingsClicked -> viewModelScope.launch { _effects.send(ProfileEffect.OpenSettings) }
            is ProfileIntent.EditGoal -> goalDialog.value = GoalDialog(
                type = intent.type,
                value = when (intent.type) {
                    GoalType.SCREEN_TIME -> state.value.screenTimeGoalMinutes
                    GoalType.UNLOCKS -> state.value.unlockGoal
                }
            )
            ProfileIntent.GoalDecreased -> changeGoal(-1)
            ProfileIntent.GoalIncreased -> changeGoal(1)
            ProfileIntent.GoalConfirmed -> confirmGoal()
            ProfileIntent.GoalDismissed -> goalDialog.value = null
        }
    }

    private fun changeGoal(direction: Int) = goalDialog.update { dialog ->
        dialog ?: return@update null
        when (dialog.type) {
            GoalType.SCREEN_TIME -> dialog.copy(
                value = (dialog.value + direction * UserSettings.SCREEN_TIME_GOAL_STEP_MINUTES).coerceIn(
                    UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES,
                    UserSettings.SCREEN_TIME_GOAL_MAX_MINUTES
                )
            )
            GoalType.UNLOCKS -> dialog.copy(
                value = (dialog.value + direction * UserSettings.UNLOCK_GOAL_STEP).coerceIn(
                    UserSettings.UNLOCK_GOAL_MIN,
                    UserSettings.UNLOCK_GOAL_MAX
                )
            )
        }
    }

    private fun confirmGoal() {
        val dialog = goalDialog.value ?: return
        goalDialog.value = null
        viewModelScope.launch {
            when (dialog.type) {
                GoalType.SCREEN_TIME -> setScreenTimeGoal(dialog.value)
                GoalType.UNLOCKS -> setUnlockGoal(dialog.value)
            }
        }
    }
}
