package com.justantis.pause.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.openAccessibilitySettings
import com.justantis.pause.presentation.components.openUsageAccessSettings
import com.justantis.pause.presentation.components.rememberNotificationPermissionRequest
import com.justantis.pause.presentation.theme.PauseTheme

@Composable
fun SettingsRoute(
    onClose: () -> Unit,
    onDataDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequest {
        viewModel.onIntent(SettingsIntent.Refresh)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(SettingsIntent.Refresh)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.Close -> onClose()
            SettingsEffect.OpenUsageAccessSettings -> context.openUsageAccessSettings()
            SettingsEffect.OpenAccessibilitySettings -> context.openAccessibilitySettings()
            SettingsEffect.RequestNotificationPermission -> requestNotificationPermission()
            SettingsEffect.DataDeleted -> onDataDeleted()
        }
    }

    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.profile_settings)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingsIntent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section(title = stringResource(R.string.settings_break_screen)) {
                ValueRow(
                    title = stringResource(R.string.settings_pauses),
                    value = stringResource(R.string.settings_pauses_value, state.pauseSeconds.joinToString(" · "))
                )
                ValueRow(
                    title = stringResource(R.string.settings_access_time),
                    value = stringResource(R.string.settings_access_time_value, state.accessMinutes.joinToString(" · "))
                )
                ValueRow(
                    title = stringResource(R.string.settings_streak_freeze),
                    value = pluralStringResource(
                        R.plurals.settings_streak_freeze_value,
                        state.freezeEveryDays,
                        state.freezeEveryDays,
                        state.maxFreezes
                    )
                )
            }
            Section(title = stringResource(R.string.settings_notifications)) {
                SwitchRow(
                    title = stringResource(R.string.settings_notify_before_limit),
                    checked = state.notifyBeforeLimit,
                    onChange = { onIntent(SettingsIntent.LimitWarningToggled(it)) }
                )
                SwitchRow(
                    title = stringResource(R.string.settings_weekly_summary),
                    checked = state.notifyWeeklySummary,
                    onChange = { onIntent(SettingsIntent.WeeklySummaryToggled(it)) }
                )
                if (!state.notificationsAllowed) {
                    Text(
                        text = stringResource(R.string.settings_notifications_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            Section(title = stringResource(R.string.settings_permissions)) {
                ValueRow(
                    title = stringResource(R.string.permission_usage_access_title),
                    value = onOff(state.usageAccessOn),
                    onClick = { onIntent(SettingsIntent.UsageAccessClicked) }
                )
                ValueRow(
                    title = stringResource(R.string.permission_accessibility_title),
                    value = onOff(state.accessibilityOn),
                    onClick = { onIntent(SettingsIntent.AccessibilityClicked) }
                )
            }
            Section(title = stringResource(R.string.nav_profile)) {
                ValueRow(
                    title = stringResource(R.string.settings_name),
                    value = state.userName ?: stringResource(R.string.settings_name_not_set),
                    onClick = { onIntent(SettingsIntent.EditNameClicked) }
                )
            }
            Section(title = stringResource(R.string.settings_data)) {
                Text(
                    text = stringResource(R.string.settings_data_local),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                TextButton(
                    onClick = { onIntent(SettingsIntent.DeleteClicked) },
                    enabled = !state.isDeleting,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(text = stringResource(R.string.settings_delete_all), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    state.nameDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.NameDismissed) },
            title = { Text(text = stringResource(R.string.settings_name)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { onIntent(SettingsIntent.NameChanged(it)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.NameConfirmed) }) { Text(text = stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.NameDismissed) }) { Text(text = stringResource(R.string.cancel)) }
            }
        )
    }
    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DeleteDismissed) },
            title = { Text(text = stringResource(R.string.settings_delete_title)) },
            text = { Text(text = stringResource(R.string.settings_delete_text)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DeleteConfirmed) }) {
                    Text(text = stringResource(R.string.settings_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DeleteDismissed) }) { Text(text = stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun onOff(on: Boolean): String = stringResource(if (on) R.string.settings_on else R.string.settings_off)

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PauseTheme {
        SettingsScreen(
            state = SettingsState(
                isLoading = false,
                usageAccessOn = true,
                accessibilityOn = true,
                userName = "Roman",
                notificationsAllowed = false
            ),
            onIntent = {}
        )
    }
}
