package com.justantis.pause.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.justantis.pause.domain.usecase.SendWeeklySummaryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs once a week, first on Monday morning, and posts the weekly summary.
 */
@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendWeeklySummary: SendWeeklySummaryUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        sendWeeklySummary()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "weekly_summary"
    }
}
