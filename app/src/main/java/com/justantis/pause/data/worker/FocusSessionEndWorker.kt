package com.justantis.pause.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.justantis.pause.domain.usecase.FinishExpiredFocusSessionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs once at the planned end of a Focus Mode session and finishes it, also when the app process
 * is not running.
 */
@HiltWorker
class FocusSessionEndWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val finishExpiredFocusSession: FinishExpiredFocusSessionUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        finishExpiredFocusSession()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "focus_session_end"
    }
}
