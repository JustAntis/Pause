package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.DataWiper
import com.justantis.pause.domain.repository.Notifier
import javax.inject.Inject

/**
 * "Delete all data" (spec §4.5): stops all background work, removes the app's notifications and
 * deletes the database and all settings. Afterwards the app starts again with onboarding. The
 * system's own usage records are not imported again, so history starts fresh from that day.
 */
class DeleteAllDataUseCase @Inject constructor(
    private val dataWiper: DataWiper,
    private val scheduler: BackgroundWorkScheduler,
    private val notifier: Notifier
) {
    suspend operator fun invoke() {
        scheduler.cancelAll()
        notifier.clearAll()
        dataWiper.wipe()
    }
}
