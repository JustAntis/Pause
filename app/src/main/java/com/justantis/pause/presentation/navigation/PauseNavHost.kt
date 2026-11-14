package com.justantis.pause.presentation.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.justantis.pause.R
import com.justantis.pause.presentation.appdetails.AppDetailsRoute
import com.justantis.pause.presentation.focus.FocusRoute
import com.justantis.pause.presentation.home.HomeRoute
import com.justantis.pause.presentation.limitedit.LimitEditRoute
import com.justantis.pause.presentation.limits.LimitsRoute
import com.justantis.pause.presentation.onboarding.OnboardingRoute
import com.justantis.pause.presentation.profile.ProfileRoute
import com.justantis.pause.presentation.stats.StatsRoute

@Composable
fun PauseAppContent(
    openTarget: OpenTarget?,
    onOpenTargetHandled: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    when (startDestination) {
        StartDestination.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
        StartDestination.Onboarding -> PauseNavHost(OnboardingDestination, openTarget, onOpenTargetHandled)
        StartDestination.Main -> PauseNavHost(MainGraph, openTarget, onOpenTargetHandled)
    }
}

@Composable
fun PauseNavHost(
    startDestination: Any,
    openTarget: OpenTarget?,
    onOpenTargetHandled: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = backStackEntry?.destination?.topLevelTab()

    LaunchedEffect(openTarget, backStackEntry != null) {
        val target = openTarget ?: return@LaunchedEffect
        val current = backStackEntry?.destination ?: return@LaunchedEffect
        if (!current.hasRoute(OnboardingDestination::class)) navController.navigateTo(target)
        onOpenTargetHandled()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedTab != null) {
                PauseBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { navController.navigateToTab(it) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable<OnboardingDestination> {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(MainGraph) {
                            popUpTo<OnboardingDestination> { inclusive = true }
                        }
                    }
                )
            }
            navigation<MainGraph>(startDestination = HomeDestination) {
                composable<HomeDestination> {
                    HomeRoute(
                        onOpenAppDetails = { navController.navigate(AppDetailsDestination(it)) },
                        onOpenFocus = { navController.navigate(FocusDestination) }
                    )
                }
                composable<StatisticsDestination> {
                    StatsRoute(onOpenAppDetails = { navController.navigate(AppDetailsDestination(it)) })
                }
                composable<LimitsDestination> {
                    LimitsRoute(onOpenLimitEdit = { navController.navigate(LimitEditDestination(it)) })
                }
                composable<LimitEditDestination> {
                    LimitEditRoute(onClose = { navController.popBackStack() })
                }
                composable<ProfileDestination> {
                    ProfileRoute(onOpenSettings = { navController.navigate(SettingsDestination) })
                }
                composable<SettingsDestination> {
                    PlaceholderScreen(title = stringResource(R.string.profile_settings))
                }
                composable<FocusDestination> {
                    FocusRoute(onClose = { navController.popBackStack() })
                }
                composable<AppDetailsDestination> {
                    AppDetailsRoute(
                        onClose = { navController.popBackStack() },
                        onOpenLimitEdit = { navController.navigate(LimitEditDestination(it)) }
                    )
                }
            }
        }
    }
}

private fun NavDestination.topLevelTab(): TopLevelTab? =
    TopLevelTab.entries.firstOrNull { tab -> hierarchy.any { it.hasRoute(tab.destination::class) } }

private fun NavHostController.navigateTo(target: OpenTarget) = when (target) {
    OpenTarget.Focus -> navigate(FocusDestination) { launchSingleTop = true }
    OpenTarget.Statistics -> navigateToTab(TopLevelTab.Statistics)
    is OpenTarget.AppDetails -> navigate(AppDetailsDestination(target.packageName))
}

private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.destination) {
        popUpTo<HomeDestination> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
