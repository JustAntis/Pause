package com.justantis.pause.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingDestination

@Serializable
data object MainGraph

@Serializable
data object HomeDestination

@Serializable
data object StatisticsDestination

@Serializable
data object LimitsDestination

@Serializable
data object ProfileDestination

@Serializable
data object FocusDestination

@Serializable
data object SettingsDestination

@Serializable
data class AppDetailsDestination(val packageName: String)

@Serializable
data class LimitEditDestination(val packageName: String? = null)
