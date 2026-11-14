package com.justantis.pause.presentation.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.AppIcon
import com.justantis.pause.presentation.components.InfoBanner
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.dashedBorder
import com.justantis.pause.presentation.components.dayName
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.DayOfWeek

@Composable
fun LimitsRoute(
    onOpenLimitEdit: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LimitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(LimitsIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is LimitsEffect.OpenLimitEdit -> onOpenLimitEdit(effect.packageName)
        }
    }

    LimitsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun LimitsScreen(
    state: LimitsState,
    onIntent: (LimitsIntent) -> Unit,
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
            text = stringResource(R.string.limits_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.limits_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.isEmpty -> EmptyLimits()
            else -> state.items.forEach { item ->
                AppLimitsCard(item = item, onClick = { onIntent(LimitsIntent.LimitClicked(item.packageName)) })
            }
        }

        AddLimitButton(onClick = { onIntent(LimitsIntent.AddLimitClicked) })
        InfoBanner(text = stringResource(R.string.limits_info))
    }
}

@Composable
private fun EmptyLimits() {
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
            Text(
                text = stringResource(R.string.limits_empty_title),    
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.limits_empty_text),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AddLimitButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(color = MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.padding(end = ButtonDefaults.IconSpacing)
        )
        Text(text = stringResource(R.string.limits_add_limit), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AppLimitsCard(item: LimitItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = item.packageName)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = statusLine(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.status is LimitStatus.UsedUp) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = formatDuration(item.limitMinutes))
            }
            if (item.status != LimitStatus.NotActiveToday) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = if (item.status is LimitStatus.UsedUp) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun statusLine(item: LimitItem): String {
    val status = when (val status = item.status) {
        is LimitStatus.Remaining -> stringResource(R.string.limits_status_left, formatDuration(status.minutes))
        is LimitStatus.UsedUp -> pluralStringResource(R.plurals.limits_status_used_up, status.attempts, status.attempts)
        LimitStatus.NotActiveToday -> stringResource(R.string.limits_status_not_active_today)
    }
    val days = when (val days = item.activeDays) {
        ActiveDays.EveryDay -> null
        ActiveDays.Weekdays -> stringResource(R.string.limits_days_weekdays)
        ActiveDays.Weekends -> stringResource(R.string.limits_days_weekends)
        is ActiveDays.Custom -> stringResource(R.string.limits_days_custom, days.days.map { dayName(it) }.joinToString(", "))
        is ActiveDays.Except -> stringResource(R.string.limits_days_except, days.days.map { dayName(it) }.joinToString(", "))
    }
    return if (days == null) status else stringResource(R.string.limits_status_with_days, days, status)
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitsScreenPreview() {
    PauseTheme {
        LimitsScreen(
            state = LimitsState(
                isLoading = false,
                items = listOf(
                    LimitItem("com.instagram.android", "Instagram", 45, 17, 0.38f, ActiveDays.EveryDay, LimitStatus.Remaining(28)),
                    LimitItem("com.facebook.katana", "Facebook", 30, 34, 1f, ActiveDays.EveryDay, LimitStatus.UsedUp(4)),
                    LimitItem("org.telegram.messenger", "Telegram", 60, 12, 0.2f, ActiveDays.Weekdays, LimitStatus.Remaining(48)),
                    LimitItem(
                        "com.google.android.youtube",
                        "YouTube",
                        90,
                        0,
                        0f,
                        ActiveDays.Custom(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
                        LimitStatus.NotActiveToday
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitsScreenEmptyPreview() {
    PauseTheme {
        LimitsScreen(state = LimitsState(isLoading = false), onIntent = {})
    }
}
