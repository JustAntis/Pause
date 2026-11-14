package com.justantis.pause.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.data.local.PauseDatabase
import com.justantis.pause.data.repository.LimitRepositoryImpl
import com.justantis.pause.domain.model.InstalledApp
import com.justantis.pause.domain.repository.AppCatalogRepository
import com.justantis.pause.domain.usecase.DeleteLimitUseCase
import com.justantis.pause.domain.usecase.GetAppUseCase
import com.justantis.pause.domain.usecase.GetLaunchableAppsUseCase
import com.justantis.pause.domain.usecase.GetLimitUseCase
import com.justantis.pause.domain.usecase.ObserveLimitsUseCase
import com.justantis.pause.domain.usecase.SaveLimitUseCase
import com.justantis.pause.presentation.limitedit.LimitEditScreen
import com.justantis.pause.presentation.limitedit.LimitEditViewModel
import com.justantis.pause.presentation.theme.PauseTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class LimitEditScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: PauseDatabase
    private lateinit var viewModel: LimitEditViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PauseDatabase::class.java).build()
        val limits = LimitRepositoryImpl(database.limitDao())
        val catalog = object : AppCatalogRepository {
            private val apps = listOf(InstalledApp("app.chat", "Chat", null))
            override suspend fun launchableApps() = apps
            override suspend fun app(packageName: String) = apps.firstOrNull { it.packageName == packageName }
        }
        viewModel = LimitEditViewModel(
            savedStateHandle = SavedStateHandle(),
            getLimit = GetLimitUseCase(limits),
            getApp = GetAppUseCase(catalog),
            getLaunchableApps = GetLaunchableAppsUseCase(catalog),
            observeLimits = ObserveLimitsUseCase(limits),
            saveLimit = SaveLimitUseCase(limits, Clock.systemDefaultZone()),
            deleteLimit = DeleteLimitUseCase(limits)
        )
        composeRule.setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PauseTheme { LimitEditScreen(state = state, onIntent = viewModel::onIntent) }
        }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun stepperStopsAtFiveMinutesAndEightHours() {
        repeat(10) { composeRule.onNodeWithContentDescription("Decrease").performClick() }
        assertEquals(5, viewModel.state.value.limitMinutes)
        composeRule.onNodeWithContentDescription("Decrease").assertIsNotEnabled()

        repeat(100) { composeRule.onNodeWithContentDescription("Increase").performClick() }
        assertEquals(480, viewModel.state.value.limitMinutes)
        composeRule.onNodeWithContentDescription("Increase").assertIsNotEnabled()
    }

    @Test
    fun presetSetsTheLimit() {
        composeRule.onAllNodesWithText("45 min")[0].performClick()

        assertEquals(45, viewModel.state.value.limitMinutes)
    }

    @Test
    fun theLastActiveDayCannotBeTurnedOff() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { composeRule.onNodeWithContentDescription(it).performClick() }

        days.dropLast(1).forEach { composeRule.onNodeWithContentDescription(it).assertIsOff() }
        composeRule.onNodeWithContentDescription("Sunday").assertIsOn()
    }
}
