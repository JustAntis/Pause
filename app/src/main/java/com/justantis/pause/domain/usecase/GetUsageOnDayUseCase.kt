package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.model.UsageSummary
import com.justantis.pause.domain.repository.UsageRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Usage of one local calendar day computed live from system events. For today the result covers
 * the time from midnight until now.
 */
class GetUsageOnDayUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(date: LocalDate): UsageSummary = usageRepository.usageOn(date)
}
