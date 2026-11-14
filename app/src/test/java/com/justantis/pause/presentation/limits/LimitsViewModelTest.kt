package com.justantis.pause.presentation.limits

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetUsageOnDayUseCase
import com.justantis.pause.domain.usecase.ObserveAttemptCountsByAppUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.testing.FakeAppCatalogRepository
import com.justantis.pause.testing.FakeBlockRepository
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.FakeUsageRepository
import com.justantis.pause.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class LimitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
    private val limitRepository = FakeLimitRepository(
        listOf(AppLimit(INSTAGRAM, 45, DayOfWeek.entries.toSet(), enabled = true, createdAt = Instant.EPOCH))
    )
    private val usageRepository = FakeUsageRepository(
        UsageSummary.EMPTY.copy(apps = listOf(AppUsage(INSTAGRAM, 17 * 60_000L, 3)))
    )

    private fun viewModel() = LimitsViewModel(
        observeLimits = ObserveLimitsUseCase(limitRepository),
        observeAttemptCountsByApp = ObserveAttemptCountsByAppUseCase(FakeBlockRepository(clock)),
        getUsageOnDay = GetUsageOnDayUseCase(usageRepository),
        getLaunchableApps = GetLaunchableAppsUseCase(
            FakeAppCatalogRepository(listOf(InstalledApp(INSTAGRAM, "Instagram", null)))
        ),
        clock = clock
    )

    @Test
    fun `refresh shows limits with today's usage`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        viewModel.onIntent(LimitsIntent.Refresh)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Instagram", state.items.single().label)
        assertEquals(LimitStatus.Remaining(28), state.items.single().status)
    }

    @Test
    fun `deleted limit disappears from the list`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onIntent(LimitsIntent.Refresh)

        limitRepository.deleteLimit(INSTAGRAM)

        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `add limit opens limit edit in create mode`() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LimitsIntent.AddLimitClicked)

        assertEquals(LimitsEffect.OpenLimitEdit(packageName = null), viewModel.effects.first())
    }

    @Test
    fun `tapping a limit opens it for editing`() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LimitsIntent.LimitClicked(INSTAGRAM))

        assertEquals(LimitsEffect.OpenLimitEdit(packageName = INSTAGRAM), viewModel.effects.first())
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
    }
}
