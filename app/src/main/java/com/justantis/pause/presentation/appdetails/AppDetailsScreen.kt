package com.justantis.pause.presentation.appdetails

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.domain.model.AppCategory
import com.justantis.pause.presentation.components.AppIcon
import com.justantis.pause.presentation.components.BarStyle
import com.justantis.pause.presentation.components.ChartBar
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.UsageBarChart
import com.justantis.pause.presentation.components.dayName
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.limits.ActiveDays
import com.justantis.pause.presentation.limits.activeDaysText
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun AppDetailsRoute(
    onClose: () -> Unit,
    onOpenLimitEdit: (String) -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(AppDetailsIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            AppDetailsEffect.Close -> onClose()
            is AppDetailsEffect.OpenLimitEdit -> onOpenLimitEdit(effect.packageName)
        }
    }

    AppDetailsScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    state: AppDetailsState,
    onIntent: (AppDetailsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onIntent(AppDetailsIntent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(state)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(formatDuration(state.todayMinutes), stringResource(R.string.app_details_time_today), Modifier.weight(1f))
                StatTile(state.opensToday.toString(), stringResource(R.string.app_details_opens_today), Modifier.weight(1f))
                StatTile(state.bypassesToday.toString(), stringResource(R.string.app_details_bypasses_today), Modifier.weight(1f))
            }
            LastDaysCard(state)
            LimitCard(state, onClick = { onIntent(AppDetailsIntent.LimitClicked) })
            FocusCard(
                paused = state.pausedInFocus,
                onChange = { onIntent(AppDetailsIntent.PausedInFocusChanged(it)) }
            )
        }
    }
}

@Composable
private fun Header(state: AppDetailsState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = state.packageName, size = 56.dp)
        Column {
            Text(text = state.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            state.category?.let {
                Text(
                    text = stringResource(categoryLabel(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@StringRes
private fun categoryLabel(category: AppCategory): Int = when (category) {
    AppCategory.GAME -> R.string.app_category_game
    AppCategory.AUDIO -> R.string.app_category_audio
    AppCategory.VIDEO -> R.string.app_category_video
    AppCategory.IMAGE -> R.string.app_category_image
    AppCategory.SOCIAL -> R.string.app_category_social
    AppCategory.NEWS -> R.string.app_category_news
    AppCategory.MAPS -> R.string.app_category_maps
    AppCategory.PRODUCTIVITY -> R.string.app_category_productivity
    AppCategory.ACCESSIBILITY -> R.string.app_category_accessibility
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LastDaysCard(state: AppDetailsState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.app_details_last_7_days),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            UsageBarChart(
                bars = state.bars.map { ChartBar(it.minutes, dayName(it.date.dayOfWeek, TextStyle.NARROW), it.style) },
                axisStepMinutes = if ((state.bars.maxOfOrNull { it.minutes } ?: 0) > 90) 60 else 15
            )
            Text(
                text = stringResource(
                    if (state.limitMinutes != null) R.string.app_details_caption_limit else R.string.app_details_caption
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LimitCard(state: AppDetailsState, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.app_details_daily_limit), style = MaterialTheme.typography.titleMedium)
                val limit = state.limitMinutes
                val days = state.limitDays
                Text(
                    text = if (limit != null && days != null) {
                        stringResource(R.string.limits_status_with_days, formatDuration(limit), activeDaysText(days))
                    } else {
                        stringResource(R.string.app_details_no_limit)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClick) {
                Text(
                    text = stringResource(
                        if (state.limitMinutes != null) R.string.app_details_edit_limit else R.string.app_details_set_limit
                    )
                )
            }
        }
    }
}

@Composable
private fun FocusCard(paused: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = paused, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_details_pause_in_focus),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = paused, onCheckedChange = null)
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun AppDetailsScreenPreview() {
    val today = LocalDate.of(2026, 11, 14)
    val minutes = listOf(32, 50, 28, 45, 12, 38, 27)
    PauseTheme {
        AppDetailsScreen(
            state = AppDetailsState(
                isLoading = false,
                packageName = "com.instagram.android",
                label = "Instagram",
                category = AppCategory.SOCIAL,
                todayMinutes = 27,
                opensToday = 9,
                bypassesToday = 1,
                bars = minutes.mapIndexed { index, value ->
                    AppDayBar(
                        date = today.minusDays(6L - index),
                        minutes = value,
                        style = when {
                            value >= 45 -> BarStyle.WARNING
                            index == 6 -> BarStyle.HIGHLIGHT
                            else -> BarStyle.NORMAL
                        }
                    )
                },
                limitMinutes = 45,
                limitDays = ActiveDays.EveryDay,
                pausedInFocus = true
            ),
            onIntent = {}
        )
    }
}
