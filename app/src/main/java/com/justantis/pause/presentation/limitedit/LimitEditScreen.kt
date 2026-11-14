package com.justantis.pause.presentation.limitedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.AppIcon
import com.justantis.pause.presentation.components.AppPickerSheet
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.PickerApp
import com.justantis.pause.presentation.components.ValueStepper
import com.justantis.pause.presentation.components.dayName
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun LimitEditRoute(
    onClose: () -> Unit,
    viewModel: LimitEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            LimitEditEffect.Close -> onClose()
        }
    }
    BackHandler { viewModel.onIntent(LimitEditIntent.BackClicked) }

    LimitEditScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitEditScreen(
    state: LimitEditState,
    onIntent: (LimitEditIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.limit_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LimitEditIntent.BackClicked) }) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppRow(
                app = state.selectedApp,
                isFixed = state.isAppFixed,
                onClick = { onIntent(LimitEditIntent.AppRowClicked) }
            )
            TimePerDayCard(state, onIntent)
            ActiveDaysCard(activeDays = state.activeDays, onDayToggled = { onIntent(LimitEditIntent.DayToggled(it)) })
            Button(
                onClick = { onIntent(LimitEditIntent.SaveClicked) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(R.string.limit_edit_save))
            }
            if (state.isEditMode) {
                TextButton(
                    onClick = { onIntent(LimitEditIntent.DeleteClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.limit_edit_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (state.isPickerVisible) {
        AppPickerSheet(
            apps = state.filteredApps,
            query = state.pickerQuery,
            onQueryChange = { onIntent(LimitEditIntent.PickerQueryChanged(it)) },
            onAppPicked = { onIntent(LimitEditIntent.AppPicked(it)) },
            onDismiss = { onIntent(LimitEditIntent.PickerDismissed) }
        )
    }
    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(LimitEditIntent.DeleteDismissed) },
            title = { Text(text = stringResource(R.string.limit_edit_delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.limit_edit_delete_text,
                        state.selectedApp?.label.orEmpty()
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(LimitEditIntent.DeleteConfirmed) }) {
                    Text(
                        text = stringResource(R.string.limit_edit_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(LimitEditIntent.DeleteDismissed) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AppRow(app: PickerApp?, isFixed: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = !isFixed,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app != null) {
                AppIcon(packageName = app.packageName)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = app?.label ?: stringResource(R.string.limit_edit_choose_app),
                style = MaterialTheme.typography.titleMedium,
                color = if (app == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (!isFixed) {
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TimePerDayCard(state: LimitEditState, onIntent: (LimitEditIntent) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.limit_edit_time_per_day),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            ValueStepper(
                value = formatDuration(state.limitMinutes),
                onDecrease = { onIntent(LimitEditIntent.DecreaseClicked) },
                onIncrease = { onIntent(LimitEditIntent.IncreaseClicked) },
                decreaseEnabled = state.canDecrease,
                increaseEnabled = state.canIncrease
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                LimitEditState.PRESETS_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = state.limitMinutes == minutes,
                        onClick = { onIntent(LimitEditIntent.PresetSelected(minutes)) },
                        label = { Text(text = formatDuration(minutes), maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveDaysCard(activeDays: Set<DayOfWeek>, onDayToggled: (DayOfWeek) -> Unit) {
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
                text = stringResource(R.string.limit_edit_active_days),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            BoxWithConstraints {
                val oneRow = maxWidth >= MIN_TOGGLE_SIZE * DayOfWeek.entries.size + TOGGLE_SPACING * (DayOfWeek.entries.size - 1)
                val rows = if (oneRow) listOf(DayOfWeek.entries) else DayOfWeek.entries.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(TOGGLE_SPACING)) {
                    rows.forEach { days ->
                        Row(horizontalArrangement = Arrangement.spacedBy(TOGGLE_SPACING)) {
                            days.forEach { day ->
                                DayToggle(
                                    label = dayName(day, TextStyle.NARROW),
                                    description = dayName(day, TextStyle.FULL),
                                    selected = day in activeDays,
                                    onToggle = { onDayToggled(day) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (!oneRow) repeat(4 - days.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayToggle(
    label: String,
    description: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(MIN_TOGGLE_SIZE)
            .widthIn(min = MIN_TOGGLE_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The minimum touch target; the seven toggles wrap to two rows when they do not fit. */
private val MIN_TOGGLE_SIZE = 48.dp
private val TOGGLE_SPACING = 4.dp

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitEditScreenPreview() {
    PauseTheme {
        LimitEditScreen(
            state = LimitEditState(
                isLoading = false,
                isEditMode = true,
                isAppFixed = true,
                selectedApp = PickerApp("com.instagram.android", "Instagram"),
                limitMinutes = 45,
                activeDays = DayOfWeek.entries.toSet() - DayOfWeek.SATURDAY - DayOfWeek.SUNDAY
            ),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=360dp,height=780dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitEditScreenCreatePreview() {
    PauseTheme {
        LimitEditScreen(state = LimitEditState(isLoading = false), onIntent = {})
    }
}
