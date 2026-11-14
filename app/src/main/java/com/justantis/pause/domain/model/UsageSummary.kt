package com.justantis.pause.domain.model

/**
 * Usage within one time window. [apps] is sorted by foreground time, longest first.
 * [hourlyScreenTimeMs] has 24 entries, index = hour of the local day.
 */
data class UsageSummary(
    val apps: List<AppUsage>,
    val totalScreenTimeMs: Long,
    val unlockCount: Int,
    val hourlyScreenTimeMs: List<Long>
) {
    fun usageOf(packageName: String): AppUsage? = apps.firstOrNull { it.packageName == packageName }

    companion object {
        val EMPTY = UsageSummary(
            apps = emptyList(),
            totalScreenTimeMs = 0,
            unlockCount = 0,
            hourlyScreenTimeMs = List(24) { 0L }
        )
    }
}

/**
 * Time one app spent in the foreground and how many separate foreground sessions it had.
 */
data class AppUsage(
    val packageName: String,
    val foregroundMs: Long,
    val openCount: Int
)
