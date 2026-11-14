package com.justantis.pause.presentation.settings

import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.domain.repository.DataWiper
import com.justantis.pause.domain.usecase.DeleteAllDataUseCase
import com.justantis.pause.domain.usecase.GetPermissionStatusUseCase
import com.justantis.pause.domain.usecase.ObserveSettingsUseCase
import com.justantis.pause.domain.usecase.SetLimitWarningEnabledUseCase
import com.justantis.pause.domain.usecase.SetUserNameUseCase
import com.justantis.pause.domain.usecase.SetWeeklySummaryEnabledUseCase
import com.justantis.pause.testing.FakeNotifier
import com.justantis.pause.testing.FakePermissionRepository
import com.justantis.pause.testing.FakeSettingsRepository
import com.justantis.pause.testing.FakeWorkScheduler
import com.justantis.pause.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(onboardingCompleted = true, userName = "Roman"))
    private val permissions = FakePermissionRepository(
        PermissionStatus(usageAccess = true, accessibilityService = false, notifications = false)
    )
    private val scheduler = FakeWorkScheduler()
    private var wiped = false
    private val wiper = object : DataWiper {
        override suspend fun wipe() {
            wiped = true
        }
    }

    private fun viewModel() = SettingsViewModel(
        observeSettings = ObserveSettingsUseCase(settings),
        getPermissionStatus = GetPermissionStatusUseCase(permissions),
        setLimitWarningEnabled = SetLimitWarningEnabledUseCase(settings),
        setWeeklySummaryEnabled = SetWeeklySummaryEnabledUseCase(settings, scheduler),
        setUserName = SetUserNameUseCase(settings),
        deleteAllData = DeleteAllDataUseCase(wiper, scheduler, FakeNotifier())
    )

    @Test
    fun `state shows rules, settings and permissions`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onIntent(SettingsIntent.Refresh)

        val state = viewModel.state.value
        assertEquals(listOf(10, 30, 60), state.pauseSeconds)
        assertEquals(listOf(1, 5, 10), state.accessMinutes)
        assertEquals(7, state.freezeEveryDays)
        assertEquals(2, state.maxFreezes)
        assertTrue(state.notifyBeforeLimit)
        assertTrue(state.usageAccessOn)
        assertFalse(state.accessibilityOn)
        assertEquals("Roman", state.userName)
    }

    @Test
    fun `turning on the weekly summary saves it, schedules it and asks for notifications`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects += it } }
        viewModel.onIntent(SettingsIntent.Refresh)

        viewModel.onIntent(SettingsIntent.WeeklySummaryToggled(true))

        assertTrue(settings.settings.value.notifyWeeklySummary)
        assertTrue(scheduler.weeklySummaryScheduled)
        assertEquals(listOf(SettingsEffect.RequestNotificationPermission), effects)
    }

    @Test
    fun `turning a switch off does not ask for notifications`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects += it } }
        viewModel.onIntent(SettingsIntent.Refresh)

        viewModel.onIntent(SettingsIntent.LimitWarningToggled(false))

        assertFalse(settings.settings.value.notifyBeforeLimit)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `the name is saved from the dialog`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        viewModel.onIntent(SettingsIntent.EditNameClicked)
        viewModel.onIntent(SettingsIntent.NameChanged("Anna"))
        viewModel.onIntent(SettingsIntent.NameConfirmed)

        assertEquals("Anna", settings.settings.value.userName)
        assertEquals(null, viewModel.state.value.nameDraft)
    }

    @Test
    fun `delete all data asks first, wipes and returns to onboarding`() = runTest {
        val viewModel = viewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects += it } }

        viewModel.onIntent(SettingsIntent.DeleteClicked)
        assertTrue(viewModel.state.value.isDeleteDialogVisible)
        assertFalse(wiped)

        viewModel.onIntent(SettingsIntent.DeleteConfirmed)

        assertTrue(wiped)
        assertTrue(scheduler.cancelledAll)
        assertEquals(listOf(SettingsEffect.DataDeleted), effects)
    }
}
