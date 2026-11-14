package com.justantis.pause.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.openUsageAccessSettings() = openSystemSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)

fun Context.openAccessibilitySettings() = openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)

private fun Context.openSystemSettings(action: String) {
    try {
        startActivity(Intent(action))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
