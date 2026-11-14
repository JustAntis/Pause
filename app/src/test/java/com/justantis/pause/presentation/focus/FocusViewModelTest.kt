package com.justantis.pause.presentation.focus

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.usecase.EndFocusSessionEarlyUseCase
import com.justantis.pause.domain.usecase.FinishExpiredFocusSessionUseCase
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.ObserveFocusAppsUseCase
import com.justantis.pause.domain.usecase.ObserveFocusSessionsUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.ObserveRunningFocusSessionUseCase
import com.justantis.pause.domain.usecase.StartFocusSessionUseCase
import com.justantis.pause.testing.FakeAppCatalogRepository
import com.justantis.pause.testing.FakeFocusRepository
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.FakeWorkScheduler
import com.justantis.pause.testing.MainDispatcherRule
import com.justantis.pause.testing.TestClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private inner class Setup(scope: TestScope, limited: List<String> = listOf(INSTAGRAM)) {
        val clock = TestClock(scope.testScheduler, Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
        val focus = FakeFocusRepository(clock)
        val scheduler = FakeWorkScheduler()
        val limits = FakeLimitRepository(
            limited.map { AppLimit(it, 30, DayOfWeek.entries.toSet(), enabled = true, createdAt = Instant.EPOCH) }
        )
        val catalog = FakeAppCatalogRepository(
            listOf(
                InstalledApp(INSTAGRAM, "Instagram", null),
                InstalledApp(TELEGRAM, "Telegram", null),
                InstalledApp(YOUTUBE, "YouTube", null)
            )
        )

        fun viewModel() = FocusViewModel(
            observeRunningFocusSession = ObserveRunningFocusSessionUseCase(focus),
            observeFocusSessions = ObserveFocusSessionsUseCase(focus),
            observeFocusApps = ObserveFocusAppsUseCase(focus),
            observeLimits = ObserveLimitsUseCase(limits),
            getLaunchableApps = GetLaunchableAppsUseCase(catalog),
            startFocusSession = StartFocusSessionUseCase(focus, scheduler),
            finishExpiredFocusSession = FinishExpiredFocusSessionUseCase(focus, scheduler, clock),
            endFocusSessionEarly = EndFocusSessionEarlyUseCase(focus, scheduler, clock),
            clock = clock
        )
    }

    private fun TestScope.started(viewModel: FocusViewModel): FocusViewModel {
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun `idle screen starts with 25 minutes and the limited apps as paused apps`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val state = started(s.viewModel()).state.value

        assertEquals(FocusPhase.IDLE, state.phase)
        assertEquals(25, state.selectedMinutes)
        assertEquals(listOf("Instagram"), state.pausedApps.map { it.label })
        assertTrue(state.canStart)
    }

    @Test
    fun `start is disabled without any paused app`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this, limited = emptyList())

        assertFalse(started(s.viewModel()).state.value.canStart)
    }

    @Test
    fun `choosing a duration and apps updates the idle screen`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel())

        viewModel.onIntent(FocusIntent.DurationSelected(45))
        viewModel.onIntent(FocusIntent.ChangeAppsClicked)
        viewModel.onIntent(FocusIntent.PickerAppToggled(TELEGRAM))
        viewModel.onIntent(FocusIntent.PickerAppToggled(INSTAGRAM))
        viewModel.onIntent(FocusIntent.PickerDone)
        runCurrent()

        val state = viewModel.state.value
        assertEquals(45, state.selectedMinutes)
        assertEquals(listOf("Telegram"), state.pausedApps.map { it.label })
        assertFalse(state.isPickerVisible)
    }

    @Test
    fun `starting persists the session and counts down from its stored times`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel())

        viewModel.onIntent(FocusIntent.StartClicked)
        runCurrent()
        assertEquals(FocusPhase.RUNNING, viewModel.state.value.phase)
        assertEquals(25 * 60, viewModel.state.value.secondsLeft)
        assertEquals(setOf(INSTAGRAM), s.focus.focusApps.value)

        advanceTimeBy(10 * 60_000L)
        runCurrent()
        assertEquals(15 * 60, viewModel.state.value.secondsLeft)
    }

    @Test
    fun `a new screen after process death shows the running session from storage`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        s.focus.setFocusApps(setOf(INSTAGRAM))
        s.focus.startSession(plannedMinutes = 25)
        advanceTimeBy(5 * 60_000L)

        val state = started(s.viewModel()).state.value

        assertEquals(FocusPhase.RUNNING, state.phase)
        assertEquals(20 * 60, state.secondsLeft)
        assertFalse(state.withBreaks)
    }

    @Test
    fun `a bypass during the session shows it as with breaks`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel())
        viewModel.onIntent(FocusIntent.StartClicked)
        runCurrent()

        s.focus.markInterrupted(s.focus.getRunningSession()!!.id)
        runCurrent()

        assertEquals(FocusPhase.RUNNING, viewModel.state.value.phase)
        assertTrue(viewModel.state.value.withBreaks)
    }

    @Test
    fun `ending early asks first and then cancels the session`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel())
        viewModel.onIntent(FocusIntent.StartClicked)
        runCurrent()

        viewModel.onIntent(FocusIntent.EndClicked)
        runCurrent()
        assertTrue(viewModel.state.value.isEndDialogVisible)
        assertEquals(FocusSessionStatus.RUNNING, s.focus.getRunningSession()!!.status)

        viewModel.onIntent(FocusIntent.EndConfirmed)
        runCurrent()
        assertEquals(FocusPhase.IDLE, viewModel.state.value.phase)
        assertEquals(FocusSessionStatus.CANCELLED, s.focus.sessions.value.single().status)
        assertEquals(0, viewModel.state.value.cleanToday)
    }

    @Test
    fun `reaching the planned end shows a clean result and counts it today`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel())
        viewModel.onIntent(FocusIntent.DurationSelected(15))
        viewModel.onIntent(FocusIntent.StartClicked)
        runCurrent()

        advanceTimeBy(15 * 60_000L + 1_000)
        runCurrent()

        val state = viewModel.state.value
        assertEquals(FocusPhase.FINISHED, state.phase)
        assertFalse(state.withBreaks)
        assertEquals(1, state.cleanToday)

        viewModel.onIntent(FocusIntent.FinishedDismissed)
        runCurrent()
        assertEquals(FocusPhase.IDLE, viewModel.state.value.phase)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
        const val YOUTUBE = "com.google.android.youtube"
    }
}
