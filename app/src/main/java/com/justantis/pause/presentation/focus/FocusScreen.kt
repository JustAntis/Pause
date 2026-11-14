package com.justantis.pause.presentation.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.AppPickerSheet
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.PickerApp
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.components.rememberNotificationPermissionRequest
import com.justantis.pause.presentation.theme.PauseTheme

@Composable
fun FocusRoute(
    onClose: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestNotificationPermission = rememberNotificationPermissionRequest()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            FocusEffect.Close -> onClose()
            FocusEffect.RequestNotificationPermission -> requestNotificationPermission()
        }
    }

    FocusScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    state: FocusState,
    onIntent: (FocusIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.focus_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(FocusIntent.BackClicked) }) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FocusRing(state)
            when (state.phase) {
                FocusPhase.IDLE -> IdleContent(state, onIntent)
                FocusPhase.RUNNING -> RunningContent(state, onIntent)
                FocusPhase.FINISHED -> FinishedContent(state, onIntent)
            }
        }
    }

    if (state.isPickerVisible) {
        AppPickerSheet(
            apps = state.filteredPickerApps,
            query = state.pickerQuery,
            onQueryChange = { onIntent(FocusIntent.PickerQueryChanged(it)) },
            onAppPicked = { onIntent(FocusIntent.PickerAppToggled(it)) },
            onDismiss = { onIntent(FocusIntent.PickerDismissed) },
            selectedPackages = state.pickerSelection,
            onDone = { onIntent(FocusIntent.PickerDone) }
        )
    }
    if (state.isEndDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(FocusIntent.EndDismissed) },
            title = { Text(text = stringResource(R.string.focus_end_title)) },
            text = { Text(text = stringResource(R.string.focus_end_text)) },
            confirmButton = {
                TextButton(onClick = { onIntent(FocusIntent.EndConfirmed) }) {
                    Text(text = stringResource(R.string.focus_end_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(FocusIntent.EndDismissed) }) {
                    Text(text = stringResource(R.string.focus_end_keep_going))
                }
            }
        )
    }
}

@Composable
private fun FocusRing(state: FocusState) {
    val time = formatClock(state.secondsLeft)
    val caption = stringResource(
        when (state.phase) {
            FocusPhase.IDLE -> R.string.focus_ring_idle
            FocusPhase.RUNNING -> R.string.focus_ring_running
            FocusPhase.FINISHED -> R.string.focus_ring_finished
        }
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(240.dp)
            .clearAndSetSemantics { contentDescription = "$time, $caption" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * state.progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = time, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(text = caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun todaySummary(state: FocusState): String = stringResource(
    R.string.focus_today_summary,
    pluralStringResource(R.plurals.focus_today_clean, state.cleanToday, state.cleanToday),
    pluralStringResource(R.plurals.focus_today_with_breaks, state.withBreaksToday, state.withBreaksToday)
)

@Composable
private fun formatClock(totalSeconds: Int): String =
    stringResource(R.string.focus_clock, totalSeconds / 60, totalSeconds % 60)

@Composable
private fun IdleContent(state: FocusState, onIntent: (FocusIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.durations.forEach { minutes ->
            FilterChip(
                selected = minutes == state.selectedMinutes,
                onClick = { onIntent(FocusIntent.DurationSelected(minutes)) },
                label = { Text(text = formatDuration(minutes), maxLines = 1) }
            )
        }
    }
    PausedAppsCard(apps = state.pausedApps, onChange = { onIntent(FocusIntent.ChangeAppsClicked) })
    Text(
        text = todaySummary(state),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = { onIntent(FocusIntent.StartClicked) },
        enabled = state.canStart,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = stringResource(R.string.home_start_session))
    }
}

@Composable
private fun RunningContent(state: FocusState, onIntent: (FocusIntent) -> Unit) {
    Text(
        text = stringResource(if (state.withBreaks) R.string.focus_status_with_breaks else R.string.focus_status_clean),
        style = MaterialTheme.typography.titleMedium,
        color = if (state.withBreaks) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    )
    PausedAppsCard(apps = state.pausedApps, onChange = null)
    OutlinedButton(
        onClick = { onIntent(FocusIntent.EndClicked) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = stringResource(R.string.focus_end_session))
    }
}

@Composable
private fun FinishedContent(state: FocusState, onIntent: (FocusIntent) -> Unit) {
    Text(
        text = stringResource(if (state.withBreaks) R.string.focus_result_with_breaks else R.string.focus_result_clean),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Text(
        text = todaySummary(state),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = { onIntent(FocusIntent.FinishedDismissed) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = stringResource(R.string.focus_done))
    }
}

@Composable
private fun PausedAppsCard(apps: List<PickerApp>, onChange: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.focus_paused_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onChange != null) {
                    TextButton(onClick = onChange) { Text(text = stringResource(R.string.focus_change_apps)) }
                }
            }
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.focus_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.take(VISIBLE_APP_CHIPS).forEach { app ->
                        AssistChip(onClick = {}, enabled = false, label = { Text(text = app.label) })
                    }
                    if (apps.size > VISIBLE_APP_CHIPS) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(text = stringResource(R.string.focus_more_apps, apps.size - VISIBLE_APP_CHIPS)) }
                        )
                    }
                }
            }
        }
    }
}

private const val VISIBLE_APP_CHIPS = 3

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun FocusScreenIdlePreview() {
    PauseTheme {
        FocusScreen(
            state = FocusState(
                isLoading = false,
                totalSeconds = 25 * 60,
                secondsLeft = 25 * 60,
                pausedApps = listOf(
                    PickerApp("a", "Instagram"),
                    PickerApp("b", "TikTok"),
                    PickerApp("c", "YouTube"),
                    PickerApp("d", "Facebook"),
                    PickerApp("e", "Reddit")
                ),
                cleanToday = 2,
                withBreaksToday = 1
            ),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun FocusScreenRunningPreview() {
    PauseTheme {
        FocusScreen(
            state = FocusState(
                isLoading = false,
                phase = FocusPhase.RUNNING,
                totalSeconds = 25 * 60,
                secondsLeft = 17 * 60 + 42,
                withBreaks = true,
                pausedApps = listOf(PickerApp("a", "Instagram"), PickerApp("b", "TikTok"))
            ),
            onIntent = {}
        )
    }
}
