package com.justantis.pause.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.usecase.CloseFinishedDaysUseCase
import com.justantis.pause.domain.usecase.FinishExpiredFocusSessionUseCase
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import com.justantis.pause.domain.usecase.ScheduleBackgroundWorkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination { Loading, Onboarding, Main }

/**
 * Decides once, at app start, whether the user still has to finish onboarding. For onboarded
 * users it makes sure the history sync is scheduled, finishes a Focus Mode session whose time ran
 * out while the app was not running, and closes the days that ended since the app was last opened.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase,
    private val closeFinishedDays: CloseFinishedDaysUseCase,
    private val scheduleBackgroundWork: ScheduleBackgroundWorkUseCase,
    private val finishExpiredFocusSession: FinishExpiredFocusSessionUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow(StartDestination.Loading)
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = observeSettings().first()
            _startDestination.value =
                if (settings.onboardingCompleted) StartDestination.Main else StartDestination.Onboarding
            if (settings.onboardingCompleted) {
                scheduleBackgroundWork()
                finishExpiredFocusSession()
                closeFinishedDays()
            }
        }
    }
}
