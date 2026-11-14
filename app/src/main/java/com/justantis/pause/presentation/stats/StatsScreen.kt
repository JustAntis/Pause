package com.justantis.pause.presentation.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.BarStyle
import com.justantis.pause.presentation.components.ChartBar
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.UsageBarChart
import com.justantis.pause.presentation.components.dayName
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.abs

@Composable
fun StatsRoute(
    onOpenAppDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(StatsIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is StatsEffect.OpenAppDetails -> onOpenAppDetails(effect.packageName)
        }
    }

    StatsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun StatsScreen(
    state: StatsState,
    onIntent: (StatsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        ChooseTimeLimit(selected = state.period, onSelected = { onIntent(StatsIntent.PeriodSelected(it)) })
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        ScreenTimeChart(state)
        AppUnlockedLocked(averageUnlocks = state.averageUnlocks, limitBypasses = state.limitBypasses)
        AppsScreenTimeCard(apps = state.apps, onAppClick = { onIntent(StatsIntent.AppClicked(it)) })
    }
}

@Composable
private fun ChooseTimeLimit(selected: StatsPeriod, onSelected: (StatsPeriod) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StatsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index, StatsPeriod.entries.size)
            ) {
                Text(stringResource(period.labelRes))
            }
        }
    }
}

@Composable
private fun ScreenTimeChart(state: StatsState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (state.period == StatsPeriod.Day) R.string.stats_today else R.string.stats_daily_average
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(state.averageMinutes),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                state.changePercent?.let { ChangeChip(it) }
            }
            UsageBarChart(
                bars = chartBars(state),
                axisStepMinutes = if (state.period == StatsPeriod.Day) 15 else 60,
                labelSpacing = when (state.period) {
                    StatsPeriod.Day -> 6
                    StatsPeriod.Week -> 1
                    StatsPeriod.Month -> 5
                }
            )
            Text(
                text = if (state.period == StatsPeriod.Day) {
                    stringResource(R.string.stats_caption_day)
                } else {
                    stringResource(R.string.stats_caption_days, formatDuration(state.screenTimeGoalMinutes))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ChangeChip(changePercent: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Text(
            text = when {
                changePercent > 0 -> stringResource(R.string.stats_change_up, changePercent)
                changePercent < 0 -> stringResource(R.string.stats_change_down, abs(changePercent))
                else -> stringResource(R.string.stats_change_none)
            },
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun chartBars(state: StatsState): List<ChartBar> = state.bars.map { bar ->
    val label = when {
        bar.hour != null -> bar.hour.toString()
        bar.date != null && state.period == StatsPeriod.Week -> dayName(bar.date.dayOfWeek, TextStyle.NARROW)
        bar.date != null -> bar.date.dayOfMonth.toString()
        else -> ""
    }
    ChartBar(minutes = bar.minutes, label = label, style = bar.style)
}

@Composable
private fun AppUnlockedLocked(averageUnlocks: Int, limitBypasses: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = averageUnlocks.toString(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(text = stringResource(R.string.stats_unlocks_per_day), style = MaterialTheme.typography.titleMedium)
            }
        }

        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = limitBypasses.toString(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(text = stringResource(R.string.stats_limit_bypasses), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AppsScreenTimeCard(apps: List<StatsAppItem>, onAppClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = stringResource(R.string.stats_most_used_apps), style = MaterialTheme.typography.titleLarge)
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            apps.forEach { app ->
                AppScreenTime(appName = app.label, usedMinutes = app.minutes, onClick = { onAppClick(app.packageName) })
            }
        }
    }
}

@Composable
private fun AppScreenTime(appName: String, usedMinutes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = appName, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = formatDuration(usedMinutes),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun StatsScreenPreview() {
    val today = LocalDate.of(2026, 11, 14)
    val minutes = listOf(192, 126, 270, 108, 174, 300, 132)
    PauseTheme {
        StatsScreen(
            state = StatsState(
                isLoading = false,
                period = StatsPeriod.Week,
                averageMinutes = 186,
                changePercent = -18,
                bars = minutes.mapIndexed { index, value ->
                    val date = today.minusDays(5).plusDays(index.toLong())
                    StatsBar(
                        minutes = value,
                        style = when {
                            value > 180 -> BarStyle.WARNING
                            date == today -> BarStyle.HIGHLIGHT
                            else -> BarStyle.NORMAL
                        },
                        date = date
                    )
                },
                averageUnlocks = 41,
                limitBypasses = 3,
                apps = listOf(
                    StatsAppItem("com.instagram.android", "Instagram", 312),
                    StatsAppItem("org.telegram.messenger", "Telegram", 208),
                    StatsAppItem("com.facebook.katana", "Facebook", 105)
                )
            ),
            onIntent = {}
        )
    }
}
