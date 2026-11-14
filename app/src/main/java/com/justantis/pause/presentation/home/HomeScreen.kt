package com.justantis.pause.presentation.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.appLocale
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.components.openAccessibilitySettings
import com.justantis.pause.presentation.components.openUsageAccessSettings
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onOpenAppDetails: (String) -> Unit,
    onOpenFocus: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(HomeIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.OpenAppDetails -> onOpenAppDetails(effect.packageName)
            HomeEffect.OpenFocus -> onOpenFocus()
            HomeEffect.OpenUsageAccessSettings -> context.openUsageAccessSettings()
            HomeEffect.OpenAccessibilitySettings -> context.openAccessibilitySettings()
        }
    }

    HomeScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = formatDate(state.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = greeting(state.userName, state.timeOfDay),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.usageAccessMissing) {
            PermissionWarningCard(
                title = stringResource(R.string.home_usage_access_off_title),
                text = stringResource(R.string.home_usage_access_off_text),
                onOpenSettings = { onIntent(HomeIntent.FixUsageAccess) }
            )
        }
        if (state.accessibilityMissing) {
            PermissionWarningCard(
                title = stringResource(R.string.home_accessibility_off_title),
                text = stringResource(R.string.home_accessibility_off_text),
                onOpenSettings = { onIntent(HomeIntent.FixAccessibility) }
            )
        }

        StreakCard(streakDays = state.streakDays, freezes = state.freezes, maxFreezes = state.maxFreezes)
        ScreenTimeCard(state)
        AppsScreenTimeCard(apps = state.topApps, onAppClick = { onIntent(HomeIntent.AppClicked(it)) })
        Button(
            onClick = { onIntent(HomeIntent.StartSessionClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.home_start_session))
        }
    }
}

@Composable
private fun formatDate(date: LocalDate): String {
    val locale = appLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    return date.format(formatter).replaceFirstChar { it.titlecase(locale) }
}

@Composable
private fun greeting(userName: String?, timeOfDay: TimeOfDay): String = when {
    userName != null -> stringResource(R.string.home_greeting_name, userName)
    timeOfDay == TimeOfDay.MORNING -> stringResource(R.string.home_greeting_morning)
    timeOfDay == TimeOfDay.AFTERNOON -> stringResource(R.string.home_greeting_afternoon)
    else -> stringResource(R.string.home_greeting_evening)
}

@Composable
private fun PermissionWarningCard(title: String, text: String, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Rounded.Warning, contentDescription = null)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(R.string.home_open_settings))
            }
        }
    }
}

@Composable
private fun StreakCard(streakDays: Int, freezes: Int, maxFreezes: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = stringResource(R.string.home_streak_content_description),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        if (streakDays > 0) R.string.home_streak_caption else R.string.home_streak_caption_start
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.home_freezes_value, freezes, maxFreezes),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.home_freezes_label),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(state: HomeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_screen_time_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.home_screen_time_goal, formatDuration(state.screenTimeGoalMinutes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatDuration(state.screenTimeMinutes),
                style = MaterialTheme.typography.displaySmall,
                color = if (state.isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { state.screenTimeProgress },
                color = if (state.isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(R.plurals.home_unlocks, state.unlocks, state.unlocks),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(
                        R.plurals.home_attempts_after_limits,
                        state.attemptsToday,
                        state.attemptsToday
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AppsScreenTimeCard(apps: List<HomeAppItem>, onAppClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_todays_screen_time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_usage_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            apps.forEach { app ->
                AppScreenTimeWithProgress(app = app, onClick = { onAppClick(app.packageName) })
            }
        }
    }
}

@Composable
private fun AppScreenTimeWithProgress(app: HomeAppItem, onClick: () -> Unit) {
    val accent = when {
        app.limitMinutes == null -> MaterialTheme.colorScheme.outline
        app.isOverLimit -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Row {
            Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (app.limitMinutes != null) {
                    stringResource(R.string.home_app_usage_of_limit, app.usedMinutes, app.limitMinutes)
                } else {
                    stringResource(R.string.home_app_usage_no_limit, formatDuration(app.usedMinutes))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (app.isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { app.progress },
            color = accent,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenPreview() {
    PauseTheme {
        HomeScreen(
            state = HomeState(
                date = LocalDate.of(2026, 11, 14),
                isLoading = false,
                userName = "Roman",
                streakDays = 12,
                freezes = 1,
                screenTimeMinutes = 134,
                unlocks = 46,
                attemptsToday = 4,
                topApps = listOf(
                    HomeAppItem("com.instagram.android", "Instagram", 40, 35, 1f),
                    HomeAppItem("org.telegram.messenger", "Telegram", 27, 45, 0.6f),
                    HomeAppItem("com.facebook.katana", "Facebook", 21, null, 0.5f)
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=360dp,height=780dp", showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenNarrowPreview() {
    PauseTheme {
        HomeScreen(
            state = HomeState(
                date = LocalDate.of(2026, 11, 14),
                isLoading = false,
                screenTimeMinutes = 134,
                unlocks = 46
            ),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenPermissionMissingPreview() {
    PauseTheme {
        HomeScreen(
            state = HomeState(
                date = LocalDate.of(2026, 11, 14),
                isLoading = false,
                timeOfDay = TimeOfDay.EVENING,
                usageAccessMissing = true
            ),
            onIntent = {}
        )
    }
}
