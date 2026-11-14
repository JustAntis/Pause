package com.justantis.pause.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.justantis.pause.domain.usecase.SyncUsageHistoryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs every 15 minutes (the WorkManager minimum) and stores today's and yesterday's usage.
 */
@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncUsageHistory: SyncUsageHistoryUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncUsageHistory()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "usage_sync"
    }
}
