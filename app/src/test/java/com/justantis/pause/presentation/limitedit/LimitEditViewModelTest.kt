package com.justantis.pause.presentation.limitedit

import androidx.lifecycle.SavedStateHandle
import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.usecase.DeleteLimitUseCase
import com.justantis.pause.domain.usecase.GetAppUseCase
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetLimitUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.SaveLimitUseCase
import com.justantis.pause.presentation.components.PickerApp
import com.justantis.pause.testing.FakeAppCatalogRepository
import com.justantis.pause.testing.FakeLimitRepository
import com.justantis.pause.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class LimitEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
    private val createdAt = Instant.parse("2026-11-01T08:00:00Z")
    private val limitRepository = FakeLimitRepository(
        listOf(AppLimit(INSTAGRAM, 45, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), enabled = true, createdAt = createdAt))
    )
    private val catalog = FakeAppCatalogRepository(
        listOf(
            InstalledApp(INSTAGRAM, "Instagram", null),
            InstalledApp(TELEGRAM, "Telegram", null),
            InstalledApp(YOUTUBE, "YouTube", null)
        )
    )

    private fun viewModel(packageName: String? = null) = LimitEditViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LimitEditViewModel.PACKAGE_NAME_ARG to packageName)),
        getLimit = GetLimitUseCase(limitRepository),
        getApp = GetAppUseCase(catalog),
        getLaunchableApps = GetLaunchableAppsUseCase(catalog),
        observeLimits = ObserveLimitsUseCase(limitRepository),
        saveLimit = SaveLimitUseCase(limitRepository, clock),
        deleteLimit = DeleteLimitUseCase(limitRepository)
    )

    @Test
    fun `create mode starts with no app, 30 minutes and every day`() {
        val state = viewModel().state.value

        assertFalse(state.isEditMode)
        assertFalse(state.isAppFixed)
        assertNull(state.selectedApp)
        assertEquals(30, state.limitMinutes)
        assertEquals(DayOfWeek.entries.toSet(), state.activeDays)
        assertFalse(state.canSave)
    }

    @Test
    fun `picker lists apps without a limit and filters by the query`() {
        val viewModel = viewModel()

        viewModel.onIntent(LimitEditIntent.AppRowClicked)
        assertTrue(viewModel.state.value.isPickerVisible)
        assertEquals(listOf("Telegram", "YouTube"), viewModel.state.value.filteredApps.map { it.label })

        viewModel.onIntent(LimitEditIntent.PickerQueryChanged("tube"))
        assertEquals(listOf("YouTube"), viewModel.state.value.filteredApps.map { it.label })
    }

    @Test
    fun `picking an app selects it and closes the picker`() {
        val viewModel = viewModel()
        viewModel.onIntent(LimitEditIntent.AppRowClicked)

        viewModel.onIntent(LimitEditIntent.AppPicked(TELEGRAM))

        val state = viewModel.state.value
        assertEquals(PickerApp(TELEGRAM, "Telegram"), state.selectedApp)
        assertFalse(state.isPickerVisible)
        assertTrue(state.canSave)
    }

    @Test
    fun `stepper moves in 5 minute steps and stays within 5 minutes and 8 hours`() {
        val viewModel = viewModel()

        viewModel.onIntent(LimitEditIntent.IncreaseClicked)
        assertEquals(35, viewModel.state.value.limitMinutes)

        repeat(10) { viewModel.onIntent(LimitEditIntent.DecreaseClicked) }
        assertEquals(5, viewModel.state.value.limitMinutes)
        assertFalse(viewModel.state.value.canDecrease)

        repeat(200) { viewModel.onIntent(LimitEditIntent.IncreaseClicked) }
        assertEquals(480, viewModel.state.value.limitMinutes)
        assertFalse(viewModel.state.value.canIncrease)
    }

    @Test
    fun `preset sets the limit`() {
        val viewModel = viewModel()

        viewModel.onIntent(LimitEditIntent.PresetSelected(45))

        assertEquals(45, viewModel.state.value.limitMinutes)
    }

    @Test
    fun `days can be toggled but the last one stays on`() {
        val viewModel = viewModel()

        DayOfWeek.entries.forEach { viewModel.onIntent(LimitEditIntent.DayToggled(it)) }

        assertEquals(setOf(DayOfWeek.SUNDAY), viewModel.state.value.activeDays)

        viewModel.onIntent(LimitEditIntent.DayToggled(DayOfWeek.MONDAY))
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), viewModel.state.value.activeDays)
    }

    @Test
    fun `save stores the limit and closes`() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LimitEditIntent.AppRowClicked)
        viewModel.onIntent(LimitEditIntent.AppPicked(TELEGRAM))
        viewModel.onIntent(LimitEditIntent.PresetSelected(15))
        viewModel.onIntent(LimitEditIntent.DayToggled(DayOfWeek.SUNDAY))

        viewModel.onIntent(LimitEditIntent.SaveClicked)

        val saved = limitRepository.getLimit(TELEGRAM)!!
        assertEquals(15, saved.limitMinutes)
        assertEquals(DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY, saved.activeDays)
        assertEquals(clock.instant(), saved.createdAt)
        assertEquals(LimitEditEffect.Close, viewModel.effects.first())
    }

    @Test
    fun `edit mode loads the limit and keeps the app fixed`() {
        val state = viewModel(INSTAGRAM).state.value

        assertTrue(state.isEditMode)
        assertTrue(state.isAppFixed)
        assertEquals(PickerApp(INSTAGRAM, "Instagram"), state.selectedApp)
        assertEquals(45, state.limitMinutes)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), state.activeDays)
    }

    @Test
    fun `saving an edited limit keeps its creation time`() = runTest {
        val viewModel = viewModel(INSTAGRAM)

        viewModel.onIntent(LimitEditIntent.IncreaseClicked)
        viewModel.onIntent(LimitEditIntent.SaveClicked)

        val saved = limitRepository.getLimit(INSTAGRAM)!!
        assertEquals(50, saved.limitMinutes)
        assertEquals(createdAt, saved.createdAt)
    }

    @Test
    fun `app picker does not open for a fixed app`() {
        val viewModel = viewModel(INSTAGRAM)

        viewModel.onIntent(LimitEditIntent.AppRowClicked)

        assertFalse(viewModel.state.value.isPickerVisible)
    }

    @Test
    fun `delete asks for confirmation before removing the limit`() = runTest {
        val viewModel = viewModel(INSTAGRAM)

        viewModel.onIntent(LimitEditIntent.DeleteClicked)
        assertTrue(viewModel.state.value.isDeleteDialogVisible)
        assertEquals(1, limitRepository.getLimits().size)

        viewModel.onIntent(LimitEditIntent.DeleteConfirmed)

        assertNull(limitRepository.getLimit(INSTAGRAM))
        assertEquals(LimitEditEffect.Close, viewModel.effects.first())
    }

    @Test
    fun `dismissing the delete dialog keeps the limit`() = runTest {
        val viewModel = viewModel(INSTAGRAM)

        viewModel.onIntent(LimitEditIntent.DeleteClicked)
        viewModel.onIntent(LimitEditIntent.DeleteDismissed)

        assertFalse(viewModel.state.value.isDeleteDialogVisible)
        assertEquals(1, limitRepository.getLimits().size)
    }

    @Test
    fun `app without a limit opened directly is created with the app preselected`() {
        val state = viewModel(TELEGRAM).state.value

        assertFalse(state.isEditMode)
        assertTrue(state.isAppFixed)
        assertEquals(PickerApp(TELEGRAM, "Telegram"), state.selectedApp)
        assertTrue(state.canSave)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
        const val YOUTUBE = "com.google.android.youtube"
    }
}
