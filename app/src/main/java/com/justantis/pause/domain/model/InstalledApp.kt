package com.justantis.pause.domain.model

/**
 * A launchable app on the device. The icon is not part of the model because it is an Android
 * type; the UI loads it by [packageName].
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: AppCategory?
)

/** The app's own category declared in its manifest. Apps without one have no category. */
enum class AppCategory { GAME, AUDIO, VIDEO, IMAGE, SOCIAL, NEWS, MAPS, PRODUCTIVITY, ACCESSIBILITY }
