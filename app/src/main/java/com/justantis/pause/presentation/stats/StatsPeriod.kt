package com.justantis.pause.presentation.stats

import androidx.annotation.StringRes
import com.justantis.pause.R

enum class StatsPeriod(@StringRes val labelRes: Int) {
    Day(R.string.stats_period_day),
    Week(R.string.stats_period_week),
    Month(R.string.stats_period_month)
}
