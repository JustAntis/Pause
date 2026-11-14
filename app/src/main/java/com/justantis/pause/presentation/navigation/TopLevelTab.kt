package com.justantis.pause.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.justantis.pause.R

enum class TopLevelTab(
    val destination: Any,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Home(HomeDestination, Icons.Rounded.Home, R.string.nav_home),
    Statistics(StatisticsDestination, Icons.Rounded.BarChart, R.string.nav_statistics),
    Limits(LimitsDestination, Icons.Rounded.HourglassBottom, R.string.nav_limits),
    Profile(ProfileDestination, Icons.Rounded.Person, R.string.nav_profile)
}
