package com.justantis.pause.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination { Loading, Onboarding, Main }

/**
 * Decides once, at app start, whether the user still has to finish onboarding.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow(StartDestination.Loading)
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = observeSettings().first()
            _startDestination.value =
                if (settings.onboardingCompleted) StartDestination.Main else StartDestination.Onboarding
        }
    }
}
