package com.justantis.pause.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : BackgroundWorkScheduler {

    override fun scheduleUsageSync() {
        val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UsageSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val SYNC_INTERVAL_MINUTES = 15L
    }
}
