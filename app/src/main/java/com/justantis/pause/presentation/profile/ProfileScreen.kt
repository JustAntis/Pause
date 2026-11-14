package com.justantis.pause.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.domain.streak.DayStatus
import com.justantis.pause.domain.streak.DayStreak
import com.justantis.pause.domain.streak.StreakState
import com.justantis.pause.domain.model.UserSettings
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.ValueStepper
import com.justantis.pause.presentation.components.appLocale
import com.justantis.pause.presentation.components.dayName
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun ProfileRoute(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ProfileIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            ProfileEffect.OpenSettings -> onOpenSettings()
        }
    }

    ProfileScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onIntent(ProfileIntent.SettingsClicked) }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.profile_settings)
                )
            }
        }
        StreakCard(state)
        CalendarCard(state.weeks)
        GoalsCard(
            screenTimeGoalMinutes = state.screenTimeGoalMinutes,
            unlockGoal = state.unlockGoal,
            onEdit = { onIntent(ProfileIntent.EditGoal(it)) }
        )
    }

    state.goalDialog?.let { dialog -> GoalEditDialog(dialog, onIntent) }
}

@Composable
private fun StreakCard(state: ProfileState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = pluralStringResource(R.plurals.profile_days, state.currentStreak, state.currentStreak),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = stringResource(R.string.profile_current_streak), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreakTile(
                    value = pluralStringResource(R.plurals.profile_days, state.bestStreak, state.bestStreak),
                    label = stringResource(R.string.profile_record),
                    modifier = Modifier.weight(1f)
                )
                StreakTile(
                    value = stringResource(R.string.profile_freezes_value, state.freezes, state.maxFreezes),
                    label = state.daysUntilNextFreeze?.let {
                        pluralStringResource(R.plurals.profile_next_freeze, it, it)
                    } ?: stringResource(R.string.profile_freezes_full),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StreakTile(value: String, label: String, modifier: Modifier = Modifier) {
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
private fun CalendarCard(weeks: List<List<CalendarCell>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_last_four_weeks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = dayName(day, TextStyle.NARROW),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            weeks.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { cell -> CalendarDay(cell, modifier = Modifier.weight(1f)) }
                }
            }
            CalendarLegend()
        }
    }
}

@Composable
private fun CalendarDay(cell: CalendarCell, modifier: Modifier = Modifier) {
    val description = calendarDescription(cell)
    val todayBorder = if (cell.isToday) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(todayBorder)
            .padding(if (cell.isToday) 3.dp else 0.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(statusColor(if (cell.isFuture) null else cell.status))
            .semantics { contentDescription = description }
    )
}

@Composable
private fun statusColor(status: DayStatus?): Color = when (status) {
    DayStatus.CLEAN -> MaterialTheme.colorScheme.primary
    DayStatus.FROZEN -> MaterialTheme.colorScheme.tertiaryContainer
    DayStatus.BROKEN -> MaterialTheme.colorScheme.errorContainer
    DayStatus.UNTRACKED, DayStatus.IN_PROGRESS, null -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun calendarDescription(cell: CalendarCell): String {
    val locale = appLocale()
    val date = cell.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    val status = when {
        cell.isFuture -> stringResource(R.string.profile_status_future)
        else -> when (cell.status) {
            DayStatus.CLEAN -> stringResource(R.string.profile_status_clean)
            DayStatus.FROZEN -> stringResource(R.string.profile_status_frozen)
            DayStatus.BROKEN -> stringResource(R.string.profile_status_broken)
            DayStatus.UNTRACKED -> stringResource(R.string.profile_status_untracked)
            DayStatus.IN_PROGRESS -> stringResource(R.string.profile_status_in_progress)
        }
    }
    return stringResource(R.string.profile_day_description, date, status)
}

@Composable
private fun CalendarLegend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        LegendItem(DayStatus.CLEAN, stringResource(R.string.profile_status_clean))
        LegendItem(DayStatus.FROZEN, stringResource(R.string.profile_status_frozen))
        LegendItem(DayStatus.BROKEN, stringResource(R.string.profile_status_broken))
        LegendItem(DayStatus.UNTRACKED, stringResource(R.string.profile_status_untracked))
    }
}

@Composable
private fun LegendItem(status: DayStatus, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor(status))
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GoalsCard(screenTimeGoalMinutes: Int, unlockGoal: Int, onEdit: (GoalType) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.profile_my_goals),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            GoalRow(
                title = stringResource(R.string.goal_screen_time_title),
                value = stringResource(R.string.profile_goal_max, formatDuration(screenTimeGoalMinutes)),
                onClick = { onEdit(GoalType.SCREEN_TIME) }
            )
            GoalRow(
                title = stringResource(R.string.goal_unlocks_title),
                value = stringResource(R.string.profile_goal_max, unlockGoal.toString()),
                onClick = { onEdit(GoalType.UNLOCKS) }
            )
        }
    }
}

@Composable
private fun GoalRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.size(8.dp))
        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun GoalEditDialog(dialog: GoalDialog, onIntent: (ProfileIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(ProfileIntent.GoalDismissed) },
        title = {
            Text(
                text = stringResource(
                    when (dialog.type) {
                        GoalType.SCREEN_TIME -> R.string.goal_screen_time_title
                        GoalType.UNLOCKS -> R.string.goal_unlocks_title
                    }
                )
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ValueStepper(
                    value = when (dialog.type) {
                        GoalType.SCREEN_TIME -> formatDuration(dialog.value)
                        GoalType.UNLOCKS -> dialog.value.toString()
                    },
                    onDecrease = { onIntent(ProfileIntent.GoalDecreased) },
                    onIncrease = { onIntent(ProfileIntent.GoalIncreased) },
                    decreaseEnabled = dialog.canDecrease,
                    increaseEnabled = dialog.canIncrease
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onIntent(ProfileIntent.GoalConfirmed) }) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ProfileIntent.GoalDismissed) }) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val today = LocalDate.of(2026, 11, 14)
    val days = (0L until 26L).map { daysAgo ->
        val date = today.minusDays(daysAgo)
        val status = when (daysAgo) {
            0L -> DayStatus.IN_PROGRESS
            13L -> DayStatus.BROKEN
            18L -> DayStatus.FROZEN
            in 23L..25L -> DayStatus.UNTRACKED
            else -> DayStatus.CLEAN
        }
        DayStreak(date, status)
    }
    PauseTheme {
        ProfileScreen(
            state = ProfileStateMapper.map(
                streak = StreakState.EMPTY.copy(current = 12, best = 13, freezes = 1, daysUntilNextFreeze = 2, days = days),
                settings = UserSettings(),
                today = today
            ),
            onIntent = {}
        )
    }
}
