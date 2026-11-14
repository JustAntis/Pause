package com.justantis.pause.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.usecase.CompleteOnboardingUseCase
import com.justantis.pause.domain.usecase.GetPermissionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class OnboardingViewModel @Inject constructor(
    private val getPermissionStatus: GetPermissionStatusUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.NameChanged -> _state.update { it.copy(userName = intent.name) }
            OnboardingIntent.Next -> next()
            OnboardingIntent.Back -> back()
            OnboardingIntent.RefreshPermissions -> refreshPermissions()
            OnboardingIntent.GrantUsageAccess -> sendEffect(OnboardingEffect.OpenUsageAccessSettings)
            OnboardingIntent.GrantAccessibility -> sendEffect(OnboardingEffect.OpenAccessibilitySettings)
            OnboardingIntent.DecreaseScreenTimeGoal -> changeScreenTimeGoal(-UserSettings.SCREEN_TIME_GOAL_STEP_MINUTES)
            OnboardingIntent.IncreaseScreenTimeGoal -> changeScreenTimeGoal(UserSettings.SCREEN_TIME_GOAL_STEP_MINUTES)
            OnboardingIntent.DecreaseUnlockGoal -> changeUnlockGoal(-UserSettings.UNLOCK_GOAL_STEP)
            OnboardingIntent.IncreaseUnlockGoal -> changeUnlockGoal(UserSettings.UNLOCK_GOAL_STEP)
        }
    }

    private fun next() {
        val current = _state.value
        if (!current.canContinue) return
        if (current.isLastStep) {
            finish(current)
        } else {
            _state.update { it.copy(step = OnboardingStep.entries[it.step.ordinal + 1]) }
        }
    }

    private fun back() {
        _state.update { state ->
            if (state.step.ordinal == 0) state
            else state.copy(step = OnboardingStep.entries[state.step.ordinal - 1])
        }
    }

    private fun refreshPermissions() {
        val status = getPermissionStatus()
        _state.update {
            it.copy(
                usageAccessGranted = status.usageAccess,
                accessibilityGranted = status.accessibilityService
            )
        }
    }

    private fun changeScreenTimeGoal(delta: Int) = _state.update {
        it.copy(
            screenTimeGoalMinutes = (it.screenTimeGoalMinutes + delta).coerceIn(
                UserSettings.SCREEN_TIME_GOAL_MIN_MINUTES,
                UserSettings.SCREEN_TIME_GOAL_MAX_MINUTES
            )
        )
    }

    private fun changeUnlockGoal(delta: Int) = _state.update {
        it.copy(unlockGoal = (it.unlockGoal + delta).coerceIn(UserSettings.UNLOCK_GOAL_MIN, UserSettings.UNLOCK_GOAL_MAX))
    }

    private fun finish(state: OnboardingState) {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            completeOnboarding(
                userName = state.userName,
                screenTimeGoalMinutes = state.screenTimeGoalMinutes,
                unlockGoal = state.unlockGoal
            )
            _effects.send(OnboardingEffect.Finished)
        }
    }

    private fun sendEffect(effect: OnboardingEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
