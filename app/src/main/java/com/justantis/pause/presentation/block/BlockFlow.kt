package com.justantis.pause.presentation.block

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.justantis.pause.presentation.components.ObserveEffects
import kotlinx.serialization.Serializable

@Serializable
private data object LimitReachedDestination

@Serializable
private data object PauseDestination

@Serializable
private data object PurposeDestination

/**
 * The three break screens with one shared [BlockViewModel]. System back on the first screen
 * behaves like "Close"; on the other two it returns to the first screen.
 */
@Composable
fun BlockFlow(
    viewModel: BlockViewModel,
    onGoHome: () -> Unit,
    onReturnToApp: (String) -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            BlockEffect.GoHome -> onGoHome()
            BlockEffect.OpenPause -> navController.navigate(PauseDestination)
            BlockEffect.OpenPurpose -> navController.navigate(PurposeDestination)
            BlockEffect.BackToStart -> navController.popBackStack(LimitReachedDestination, inclusive = false)
            is BlockEffect.ReturnToApp -> onReturnToApp(effect.packageName)
        }
    }

    NavHost(navController = navController, startDestination = LimitReachedDestination) {
        composable<LimitReachedDestination> {
            BackHandler { viewModel.onIntent(BlockIntent.CloseClicked) }
            LimitReachedScreen(state = state, onIntent = viewModel::onIntent)
        }
        composable<PauseDestination> {
            BackHandler { viewModel.onIntent(BlockIntent.BackToStartClicked) }
            PauseScreen(state = state, onIntent = viewModel::onIntent)
        }
        composable<PurposeDestination> {
            BackHandler { viewModel.onIntent(BlockIntent.BackToStartClicked) }
            PurposeScreen(state = state, onIntent = viewModel::onIntent)
        }
    }
}
