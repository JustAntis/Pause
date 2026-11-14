package com.justantis.pause.presentation.stats

import androidx.annotation.StringRes
import com.justantis.pause.R
import com.justantis.pause.domain.stats.StatisticsPeriod

enum class StatsPeriod(@StringRes val labelRes: Int, val domain: StatisticsPeriod) {
    Day(R.string.stats_period_day, StatisticsPeriod.DAY),
    Week(R.string.stats_period_week, StatisticsPeriod.WEEK),
    Month(R.string.stats_period_month, StatisticsPeriod.MONTH)
}
