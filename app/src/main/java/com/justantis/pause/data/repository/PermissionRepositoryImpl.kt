package com.justantis.pause.data.repository

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {

    override fun currentStatus() = PermissionStatus(
        usageAccess = hasUsageAccess(),
        accessibilityService = isAccessibilityServiceEnabled(),
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
    )

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = ComponentName(context.packageName, ACCESSIBILITY_SERVICE_CLASS)
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private companion object {
        const val ACCESSIBILITY_SERVICE_CLASS = "com.justantis.pause.service.PauseAccessibilityService"
    }
}
