package com.justantis.pause.presentation.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.presentation.components.AppIcon
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme

/**
 * Screen 1 of the break flow. "Close" is the only filled button; the way to access is a text button.
 */
@Composable
fun LimitReachedScreen(
    state: BlockState,
    onIntent: (BlockIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(packageName = state.packageName, size = 64.dp, modifier = Modifier.padding(top = 24.dp))
            Text(
                text = state.appLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle(state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.attemptsToday.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = pluralStringResource(R.plurals.block_attempts_today, state.attemptsToday),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallStatCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = pluralStringResource(R.plurals.block_streak_days, state.streakDays, state.streakDays),
                    label = stringResource(R.string.block_streak_label),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                )
                SmallStatCard(
                    icon = Icons.Rounded.AcUnit,
                    value = stringResource(R.string.block_freezes_value, state.freezes, state.maxFreezes),
                    label = stringResource(R.string.block_freezes_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.block_next_pause_hint, state.pauseSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = { onIntent(BlockIntent.CloseClicked) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = stringResource(R.string.block_close))
        }
        TextButton(
            onClick = { onIntent(BlockIntent.NeedAccessClicked) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = stringResource(R.string.block_need_access))
        }
    }
}

@Composable
private fun subtitle(state: BlockState): String {
    if (state.reason == BlockReason.FOCUS) {
        val left = state.focusMinutesLeft ?: 0
        return stringResource(R.string.block_focus_in_progress, formatDuration(left))
    }
    val limit = state.limitMinutes ?: return ""
    return stringResource(R.string.block_used_of_limit, state.usedMinutes.coerceAtMost(limit), limit)
}

@Composable
private fun SmallStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitReachedScreenPreview() {
    PauseTheme {
        LimitReachedScreen(
            state = BlockState(
                isLoading = false,
                packageName = "com.instagram.android",
                appLabel = "Instagram",
                limitMinutes = 45,
                usedMinutes = 45,
                attemptsToday = 3,
                streakDays = 12,
                freezes = 1,
                pauseSeconds = 30
            ),
            onIntent = {}
        )
    }
}
