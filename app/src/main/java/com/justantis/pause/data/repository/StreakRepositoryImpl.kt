package com.justantis.pause.data.repository

import com.justantis.pause.data.local.dao.BlockEventDao
import com.justantis.pause.data.local.dao.DailyUsageDao
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.StreakRepository
import com.justantis.pause.domain.streak.DayRecord
import com.justantis.pause.domain.streak.StreakCalculator
import com.justantis.pause.domain.streak.StreakState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class StreakRepositoryImpl @Inject constructor(
    private val dailyUsageDao: DailyUsageDao,
    private val blockEventDao: BlockEventDao
) : StreakRepository {

    override fun observeStreak(today: LocalDate): Flow<StreakState> = combine(
        dailyUsageDao.observeAllSummaries(),
        blockEventDao.observeCountsByDate(BlockReason.LIMIT, BlockOutcome.BYPASSED)
    ) { summaries, bypasses ->
        val summaryByDate = summaries.associateBy { it.date }
        val bypassesByDate = bypasses.associate { it.date to it.count }
        val firstDay = (summaryByDate.keys + bypassesByDate.keys).minOrNull()?.coerceAtMost(today) ?: today

        val records = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { date ->
                val summary = summaryByDate[date]
                DayRecord(
                    date = date,
                    closed = summary?.closed ?: false,
                    tracked = summary?.tracked ?: false,
                    limitBypasses = bypassesByDate[date] ?: 0,
                    isToday = date == today
                )
            }
            .toList()
        StreakCalculator.calculate(records)
    }
}
