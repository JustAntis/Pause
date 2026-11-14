package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.AppLimit
import com.justantis.pause.domain.repository.LimitRepository
import java.time.Clock
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Creates or replaces the daily limit of one app (assumption A8): a whole number of 5-minute
 * steps between 5 minutes and 8 hours, applying on at least one weekday. An edited limit keeps
 * its creation time so the list order stays the same.
 */
class SaveLimitUseCase @Inject constructor(
    private val limitRepository: LimitRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(packageName: String, limitMinutes: Int, activeDays: Set<DayOfWeek>) {
        require(limitMinutes in AppLimit.MIN_MINUTES..AppLimit.MAX_MINUTES) { "Limit out of range: $limitMinutes" }
        require(limitMinutes % AppLimit.STEP_MINUTES == 0) { "Limit not in 5-minute steps: $limitMinutes" }
        require(activeDays.isNotEmpty()) { "A limit needs at least one active day" }

        val existing = limitRepository.getLimit(packageName)
        limitRepository.saveLimit(
            AppLimit(
                packageName = packageName,
                limitMinutes = limitMinutes,
                activeDays = activeDays,
                enabled = true,
                createdAt = existing?.createdAt ?: clock.instant()
            )
        )
    }
}
