package com.justantis.pause.presentation.home

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.model.AppUsage
import com.justantis.pause.domain.model.PermissionStatus
import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class HomeStateMapperTest {

    private val saturday = LocalDate.of(2026, 11, 14)
    private val allGranted = PermissionStatus(usageAccess = true, accessibilityService = true, notifications = true)
    private val labels = mapOf(
        INSTAGRAM to "Instagram",
        TELEGRAM to "Telegram",
        FACEBOOK to "Facebook",
        YOUTUBE to "YouTube"
    )

    private fun usage(vararg apps: Pair<String, Int>, unlocks: Int = 0) = UsageSummary(
        apps = apps.map { (pkg, minutes) -> AppUsage(pkg, minutes * 60_000L, 1) },
        totalScreenTimeMs = apps.sumOf { it.second } * 60_000L,
        unlockCount = unlocks,
        hourlyScreenTimeMs = List(24) { 0L }
    )

    private fun limit(pkg: String, minutes: Int, days: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        AppLimit(pkg, minutes, days, enabled = true, createdAt = Instant.EPOCH)

    private fun map(
        usage: UsageSummary,
        limits: List<AppLimit> = emptyList(),
        settings: UserSettings = UserSettings(),
        hour: Int = 10,
        permissions: PermissionStatus = allGranted,
        attempts: Int = 0
    ) = HomeStateMapper.map(
        settings = settings,
        limits = limits,
        snapshot = TodaySnapshot(saturday, hour, usage, labels, permissions),
        attemptsToday = attempts
    )

    @Test
    fun `top apps are the three with the most time`() {
        val state = map(usage(YOUTUBE to 50, INSTAGRAM to 40, TELEGRAM to 27, FACEBOOK to 11))

        assertEquals(listOf("YouTube", "Instagram", "Telegram"), state.topApps.map { it.label })
    }

    @Test
    fun `apps that are not launchable and apps under a minute are left out`() {
        val state = map(usage(LAUNCHER to 90, INSTAGRAM to 40, TELEGRAM to 0))

        assertEquals(listOf(INSTAGRAM), state.topApps.map { it.packageName })
    }

    @Test
    fun `app with a limit applying today shows the limit and clamps progress`() {
        val state = map(usage(INSTAGRAM to 40), limits = listOf(limit(INSTAGRAM, 35)))

        val item = state.topApps.single()
        assertEquals(35, item.limitMinutes)
        assertTrue(item.isOverLimit)
        assertEquals(1f, item.progress)
    }

    @Test
    fun `limit not applying today is shown as no limit`() {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        val state = map(usage(INSTAGRAM to 40), limits = listOf(limit(INSTAGRAM, 35, weekdays)))

        assertNull(state.topApps.single().limitMinutes)
        assertFalse(state.topApps.single().isOverLimit)
    }

    @Test
    fun `apps without a limit are compared with the longest one`() {
        val state = map(usage(YOUTUBE to 60, INSTAGRAM to 30))

        assertEquals(1f, state.topApps[0].progress)
        assertEquals(0.5f, state.topApps[1].progress)
    }

    @Test
    fun `screen time progress is clamped and over goal is flagged`() {
        val state = map(usage(YOUTUBE to 200), settings = UserSettings(screenTimeGoalMinutes = 180))

        assertEquals(200, state.screenTimeMinutes)
        assertEquals(1f, state.screenTimeProgress)
        assertTrue(state.isOverGoal)
    }

    @Test
    fun `time of day follows the hour`() {
        assertEquals(TimeOfDay.MORNING, map(usage(), hour = 5).timeOfDay)
        assertEquals(TimeOfDay.AFTERNOON, map(usage(), hour = 12).timeOfDay)
        assertEquals(TimeOfDay.EVENING, map(usage(), hour = 18).timeOfDay)
        assertEquals(TimeOfDay.EVENING, map(usage(), hour = 2).timeOfDay)
    }

    @Test
    fun `missing permissions are reported`() {
        val state = map(
            usage(),
            permissions = PermissionStatus(usageAccess = false, accessibilityService = false, notifications = false)
        )

        assertTrue(state.usageAccessMissing)
        assertTrue(state.accessibilityMissing)
    }

    @Test
    fun `unlocks, attempts and name are passed through`() {
        val state = map(usage(unlocks = 46), settings = UserSettings(userName = "Roman"), attempts = 4)

        assertEquals(46, state.unlocks)
        assertEquals(4, state.attemptsToday)
        assertEquals("Roman", state.userName)
        assertEquals(saturday, state.date)
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val TELEGRAM = "org.telegram.messenger"
        const val FACEBOOK = "com.facebook.katana"
        const val YOUTUBE = "com.google.android.youtube"
        const val LAUNCHER = "com.android.launcher"
    }
}
