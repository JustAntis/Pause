package com.justantis.pause.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.justantis.pause.domain.model.AppCategory
import com.justantis.pause.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Lists apps that have a launcher icon. Pause itself and the current home launcher are left out,
 * because neither may be limited.
 */
class InstalledAppsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager
) {
    fun launchableApps(): List<InstalledApp> {
        val excluded = setOfNotNull(context.packageName, homeLauncherPackage())
        return queryActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName !in excluded }
            .map { it.toInstalledApp() }
            .sortedBy { it.label.lowercase() }
    }

    fun app(packageName: String): InstalledApp? = try {
        packageManager.getApplicationInfo(packageName, 0).toInstalledApp()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun isLaunchable(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    fun homeLauncherPackage(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                home,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolved?.activityInfo?.packageName
    }

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

    private fun ApplicationInfo.toInstalledApp() = InstalledApp(
        packageName = packageName,
        label = loadLabel(packageManager).toString(),
        category = category.toAppCategory()
    )

    private fun Int.toAppCategory(): AppCategory? = when (this) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.ACCESSIBILITY
        else -> null
    }
}
