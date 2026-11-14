package com.justantis.pause.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : BackgroundWorkScheduler {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    override fun scheduleUsageSync() {
        val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(UsageSyncWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun scheduleFocusSessionEnd(at: Instant) {
        val delayMs = Duration.between(clock.instant(), at).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<FocusSessionEndWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(FocusSessionEndWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelFocusSessionEnd() {
        workManager.cancelUniqueWork(FocusSessionEndWorker.UNIQUE_NAME)
    }

    override fun scheduleWeeklySummary() {
        val now = ZonedDateTime.now(clock)
        var firstRun = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .withHour(SUMMARY_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusWeeks(1)
        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, firstRun).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WeeklySummaryWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelWeeklySummary() {
        workManager.cancelUniqueWork(WeeklySummaryWorker.UNIQUE_NAME)
    }

    override fun cancelAll() {
        workManager.cancelAllWork()
    }

    private companion object {
        const val SYNC_INTERVAL_MINUTES = 15L
        const val SUMMARY_HOUR = 9
    }
}
