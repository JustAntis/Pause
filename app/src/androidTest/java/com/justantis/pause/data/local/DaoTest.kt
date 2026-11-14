package com.justantis.pause.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.justantis.pause.data.local.entity.AppLimitEntity
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var database: PauseDatabase
    private val today = LocalDate.of(2026, 11, 14)

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PauseDatabase::class.java).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun limitsAreCreatedUpdatedAndDeleted() = runTest {
        val dao = database.limitDao()
        dao.upsert(AppLimitEntity("app.a", 30, DaysMask.EVERY_DAY, enabled = true, createdAt = 1))
        dao.upsert(AppLimitEntity("app.b", 45, 0b0011111, enabled = true, createdAt = 2))

        dao.upsert(AppLimitEntity("app.a", 60, DaysMask.EVERY_DAY, enabled = true, createdAt = 1))
        assertEquals(listOf(60, 45), dao.observeAll().first().map { it.limitMinutes })

        dao.delete("app.a")
        assertNull(dao.get("app.a"))
        assertEquals(listOf("app.b"), dao.getAll().map { it.packageName })
    }

    @Test
    fun attemptsAndBypassesAreCountedPerDate() = runTest {
        val dao = database.blockEventDao()
        fun attempt(date: LocalDate, pkg: String) =
            BlockEventEntity(timestamp = 0, date = date, packageName = pkg, reason = BlockReason.LIMIT, outcome = BlockOutcome.CLOSED)
        val first = dao.insert(attempt(today, "app.a"))
        dao.insert(attempt(today, "app.a"))
        dao.insert(attempt(today, "app.b"))
        dao.insert(attempt(today.minusDays(1), "app.a"))

        dao.updateOutcome(first, BlockOutcome.BYPASSED, AccessPurpose.WORK_OR_STUDY, grantedMinutes = 5, pauseSeconds = 10)

        assertEquals(2, dao.observeAttemptCount(today, "app.a").first())
        assertEquals(1, dao.countWithOutcome(today, BlockOutcome.BYPASSED))
        assertEquals(0, dao.countWithOutcome(today.minusDays(1), BlockOutcome.BYPASSED))
        val bypassed = dao.get(first)!!
        assertEquals(AccessPurpose.WORK_OR_STUDY, bypassed.purpose)
        assertEquals(5, bypassed.grantedMinutes)
        assertEquals(listOf(today to 1), dao.observeCountsByDate(BlockReason.LIMIT, BlockOutcome.BYPASSED).first().map { it.date to it.count })
    }

    @Test
    fun replacingADayKeepsItsFlagsAndReplacesItsApps() = runTest {
        val dao = database.dailyUsageDao()
        dao.replaceDay(today, 100, 10, listOf(DailyAppUsageEntity(today, "app.a", 60, 2), DailyAppUsageEntity(today, "app.b", 40, 1)))
        dao.closeDay(today, tracked = true)

        dao.replaceDay(today, 200, 12, listOf(DailyAppUsageEntity(today, "app.a", 200, 3)))

        val summary = dao.getSummary(today)!!
        assertEquals(200L, summary.screenTimeMs)
        assertTrue(summary.closed && summary.tracked)
        assertEquals(listOf("app.a"), dao.observeAppUsage(today, today).first().map { it.packageName })
        assertEquals(today, dao.getLastClosedDate())
    }

    @Test
    fun closingADayWithoutUsageCreatesAnEmptyRow() = runTest {
        val dao = database.dailyUsageDao()

        dao.closeDay(today, tracked = false)

        val summary = dao.getSummary(today)!!
        assertEquals(0L, summary.screenTimeMs)
        assertTrue(summary.closed)
    }
}
