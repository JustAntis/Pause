package com.justantis.pause.debug

import androidx.room.withTransaction
import com.justantis.pause.data.local.PauseDatabase
import com.justantis.pause.data.local.dao.BlockEventDao
import com.justantis.pause.data.local.dao.DailyUsageDao
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.data.local.entity.DailySummaryEntity
import com.justantis.pause.data.usage.InstalledAppsDataSource
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Debug builds only: writes 35 closed past days of demo history for checking the streak, the
 * calendar and statistics, and for screenshots. Today is not touched. Running it again replaces
 * the same days.
 *
 * Pattern, oldest first: 8 untracked, 9 clean (a freeze at day 7), 1 frozen, 4 clean, 1 broken,
 * 12 clean. Result: current streak 12, record 13, 1 of 2 freezes, next one in 2 days.
 */
class DemoDataSeeder @Inject constructor(
    private val database: PauseDatabase,
    private val dailyUsageDao: DailyUsageDao,
    private val blockEventDao: BlockEventDao,
    private val installedApps: InstalledAppsDataSource,
    private val clock: Clock
) {
    suspend fun seed(): Int {
        val today = LocalDate.now(clock)
        val firstDay = today.minusDays(PATTERN.length.toLong())
        val apps = demoApps()
        val random = Random(SEED)

        database.withTransaction {
            blockEventDao.deleteBetween(firstDay, today.minusDays(1))
            PATTERN.forEachIndexed { index, symbol ->
                val date = firstDay.plusDays(index.toLong())
                seedDay(date, symbol, apps, random)
            }
        }
        return PATTERN.length
    }

    private suspend fun seedDay(date: LocalDate, symbol: Char, apps: List<String>, random: Random) {
        val overGoal = symbol == FROZEN || symbol == BROKEN || random.nextInt(6) == 0
        val screenTimeMinutes = if (overGoal) random.nextInt(200, 260) else random.nextInt(95, 175)
        val appShares = listOf(0.30, 0.20, 0.15, 0.10)
        val appUsage = apps.zip(appShares).map { (packageName, share) ->
            val minutes = (screenTimeMinutes * share * random.nextDouble(0.8, 1.2)).toLong()
            DailyAppUsageEntity(
                date = date,
                packageName = packageName,
                foregroundMs = Duration.ofMinutes(minutes).toMillis(),
                openCount = random.nextInt(4, 25)
            )
        }

        dailyUsageDao.upsertSummary(
            DailySummaryEntity(
                date = date,
                screenTimeMs = Duration.ofMinutes(screenTimeMinutes.toLong()).toMillis(),
                unlockCount = random.nextInt(28, 58),
                closed = true,
                tracked = symbol != UNTRACKED
            )
        )
        dailyUsageDao.deleteAppUsage(date)
        dailyUsageDao.upsertAppUsage(appUsage)

        val limitedApp = apps.firstOrNull() ?: return
        val time = date.atTime(20, 15).atZone(clock.zone).toInstant().toEpochMilli()
        if (symbol == FROZEN || symbol == BROKEN) {
            blockEventDao.insert(
                BlockEventEntity(
                    timestamp = time,
                    date = date,
                    packageName = limitedApp,
                    reason = BlockReason.LIMIT,
                    outcome = BlockOutcome.BYPASSED,
                    purpose = AccessPurpose.REPLY_TO_MESSAGE,
                    grantedMinutes = 5,
                    pauseSeconds = 10
                )
            )
        }
        if (symbol != UNTRACKED && random.nextInt(3) == 0) {
            blockEventDao.insert(
                BlockEventEntity(
                    timestamp = time - Duration.ofHours(2).toMillis(),
                    date = date,
                    packageName = limitedApp,
                    reason = BlockReason.LIMIT,
                    outcome = BlockOutcome.CLOSED
                )
            )
        }
    }

    /** Real apps on this phone so that names and icons appear; well-known ones first. */
    private fun demoApps(): List<String> {
        val launchable = installedApps.launchableApps().map { it.packageName }
        val preferred = PREFERRED_APPS.filter { it in launchable }
        return (preferred + launchable).distinct().take(DEMO_APP_COUNT)
    }

    private companion object {
        const val UNTRACKED = 'U'
        const val FROZEN = 'F'
        const val BROKEN = 'B'
        val PATTERN = "UUUUUUUU" + "CCCCCCCCC" + "F" + "CCCC" + "B" + "CCCCCCCCCCCC"
        const val SEED = 42
        const val DEMO_APP_COUNT = 4
        val PREFERRED_APPS = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "org.telegram.messenger",
            "com.android.chrome"
        )
    }
}
