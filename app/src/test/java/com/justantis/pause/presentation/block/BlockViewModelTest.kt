package com.justantis.pause.presentation.block

import androidx.lifecycle.SavedStateHandle
import com.justantis.pause.domain.friction.FrictionPolicy
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.usecase.GetAppUseCase
import com.justantis.pause.domain.usecase.GetBypassesTodayUseCase
import com.justantis.pause.domain.usecase.GetLimitUseCase
import com.justantis.pause.domain.usecase.GetRunningFocusSessionUseCase
import com.justantis.pause.domain.usecase.GetUsageOnDayUseCase
import com.justantis.pause.domain.usecase.GrantAccessUseCase
import com.justantis.pause.domain.usecase.ObserveAttemptCountsByAppUseCase
import com.justantis.pause.testing.FakeAppCatalogRepository
import com.justantis.pause.testing.FakeBlockRepository
import com.justantis.pause.testing.FakeFocusRepository
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.FakeUsageRepository
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
class BlockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private inner class Setup(scope: TestScope) {
        val clock = TestClock(scope.testScheduler, Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
        val blocks = FakeBlockRepository(clock)
        val focus = FakeFocusRepository(clock)
        val limits = FakeLimitRepository(
            listOf(AppLimit(INSTAGRAM, 45, DayOfWeek.entries.toSet(), enabled = true, createdAt = Instant.EPOCH))
        )
        val usage = FakeUsageRepository(
            UsageSummary.EMPTY.copy(apps = listOf(AppUsage(INSTAGRAM, 47 * 60_000L, 5)))
        )
        val effects = mutableListOf<BlockEffect>()

        fun viewModel(eventId: Long, reason: BlockReason = BlockReason.LIMIT, focusSessionId: Long? = null) =
            BlockViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        BlockViewModel.ARG_PACKAGE_NAME to INSTAGRAM,
                        BlockViewModel.ARG_EVENT_ID to eventId,
                        BlockViewModel.ARG_REASON to reason.name,
                        BlockViewModel.ARG_FOCUS_SESSION_ID to (focusSessionId ?: BlockViewModel.NO_FOCUS_SESSION)
                    )
                ),
                getApp = GetAppUseCase(FakeAppCatalogRepository(listOf(InstalledApp(INSTAGRAM, "Instagram", null)))),
                getLimit = GetLimitUseCase(limits),
                getUsageOnDay = GetUsageOnDayUseCase(usage),
                observeAttemptCountsByApp = ObserveAttemptCountsByAppUseCase(blocks),
                getBypassesToday = GetBypassesTodayUseCase(blocks, clock),
                getRunningFocusSession = GetRunningFocusSessionUseCase(focus),
                grantAccess = GrantAccessUseCase(blocks, focus, clock),
                frictionPolicy = FrictionPolicy(),
                clock = clock
            )
    }

    private fun TestScope.started(viewModel: BlockViewModel, effects: MutableList<BlockEffect>): BlockViewModel {
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        runCurrent()
        return viewModel
    }

    @Test
    fun `screen shows the app, its limit, attempts today and the next pause`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val earlier = s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)
        s.blocks.recordBypass(earlier, AccessPurpose.OTHER, 5, 10)
        s.blocks.recordAttempt(TELEGRAM, BlockReason.LIMIT, null)
        val current = s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)

        val state = started(s.viewModel(current), s.effects).state.value

        assertFalse(state.isLoading)
        assertEquals("Instagram", state.appLabel)
        assertEquals(45, state.limitMinutes)
        assertEquals(47, state.usedMinutes)
        assertEquals(2, state.attemptsToday)
        assertEquals(2, state.accessOrdinal)
        assertEquals(30, state.pauseSeconds)
    }

    @Test
    fun `pause counts down and allows continuing only at the end`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)

        viewModel.onIntent(BlockIntent.NeedAccessClicked)
        viewModel.onIntent(BlockIntent.PauseStarted)
        runCurrent()
        assertEquals(10, viewModel.state.value.pauseSecondsLeft)

        advanceTimeBy(9_000)
        runCurrent()
        assertEquals(1, viewModel.state.value.pauseSecondsLeft)
        viewModel.onIntent(BlockIntent.NextClicked)
        runCurrent()
        assertFalse(viewModel.state.value.canContinue)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, viewModel.state.value.pauseSecondsLeft)
        assertTrue(viewModel.state.value.canContinue)
        viewModel.onIntent(BlockIntent.NextClicked)
        runCurrent()
        assertEquals(listOf(BlockEffect.OpenPause, BlockEffect.OpenPurpose), s.effects)
    }

    @Test
    fun `starting the pause again after rotation keeps the countdown`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)
        viewModel.onIntent(BlockIntent.PauseStarted)
        advanceTimeBy(6_000)
        runCurrent()

        viewModel.onIntent(BlockIntent.PauseStarted)
        runCurrent()

        assertEquals(4, viewModel.state.value.pauseSecondsLeft)
    }

    @Test
    fun `going back to the first screen restarts the pause next time`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)
        viewModel.onIntent(BlockIntent.PauseStarted)
        advanceTimeBy(6_000)
        runCurrent()

        viewModel.onIntent(BlockIntent.BackToStartClicked)
        viewModel.onIntent(BlockIntent.PauseStarted)
        runCurrent()

        assertEquals(10, viewModel.state.value.pauseSecondsLeft)
        assertEquals(BlockEffect.BackToStart, s.effects.last())
    }

    @Test
    fun `leaving the screen during the pause cancels it`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)
        viewModel.onIntent(BlockIntent.PauseStarted)
        advanceTimeBy(20_000)
        runCurrent()
        assertTrue(viewModel.state.value.canContinue)

        viewModel.onIntent(BlockIntent.FlowInterrupted)
        runCurrent()

        assertFalse(viewModel.state.value.canContinue)
        assertEquals(10, viewModel.state.value.pauseSecondsLeft)
    }

    @Test
    fun `confirm is enabled only after a purpose and a time are chosen`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)

        assertFalse(viewModel.state.value.canConfirm)
        viewModel.onIntent(BlockIntent.PurposeSelected(AccessPurpose.WORK_OR_STUDY))
        assertFalse(viewModel.state.value.canConfirm)
        viewModel.onIntent(BlockIntent.MinutesSelected(5))
        assertTrue(viewModel.state.value.canConfirm)
    }

    @Test
    fun `confirming records the bypass, grants access and returns to the app`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val eventId = s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)
        val viewModel = started(s.viewModel(eventId), s.effects)
        viewModel.onIntent(BlockIntent.PauseStarted)
        advanceTimeBy(10_000)
        runCurrent()
        viewModel.onIntent(BlockIntent.PurposeSelected(AccessPurpose.REPLY_TO_MESSAGE))
        viewModel.onIntent(BlockIntent.MinutesSelected(5))

        viewModel.onIntent(BlockIntent.ConfirmClicked)
        runCurrent()

        val event = s.blocks.getEvent(eventId)!!
        assertEquals(BlockOutcome.BYPASSED, event.outcome)
        assertEquals(10, event.pauseSeconds)
        assertEquals(s.clock.instant().plusSeconds(300), s.blocks.getActiveGrant(INSTAGRAM)!!.expiresAt)
        assertEquals(BlockEffect.ReturnToApp(INSTAGRAM), s.effects.last())
    }

    @Test
    fun `focus variant shows the minutes left and marks the session as interrupted`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val session = s.focus.startSession(plannedMinutes = 25)
        advanceTimeBy(13 * 60_000L)
        val eventId = s.blocks.recordAttempt(INSTAGRAM, BlockReason.FOCUS, session.id)
        val viewModel = started(s.viewModel(eventId, BlockReason.FOCUS, session.id), s.effects)

        assertEquals(12, viewModel.state.value.focusMinutesLeft)

        viewModel.onIntent(BlockIntent.PurposeSelected(AccessPurpose.OTHER))
        viewModel.onIntent(BlockIntent.MinutesSelected(1))
        viewModel.onIntent(BlockIntent.ConfirmClicked)
        runCurrent()

        assertEquals(FocusSessionStatus.INTERRUPTED, s.focus.getRunningSession()!!.status)
    }

    @Test
    fun `close goes to the home screen`() = runTest(mainDispatcherRule.dispatcher) {
        val s = Setup(this)
        val viewModel = started(s.viewModel(s.blocks.recordAttempt(INSTAGRAM, BlockReason.LIMIT, null)), s.effects)

        viewModel.onIntent(BlockIntent.CloseClicked)
        runCurrent()

        assertEquals(listOf(BlockEffect.GoHome), s.effects)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
    }
}
