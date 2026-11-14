package com.justantis.pause.presentation.block

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.domain.friction.FrictionConfig
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.streak.BypassConsequence
import com.justantis.pause.presentation.components.InfoBanner
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme

/**
 * Screen 3 of the break flow: purpose and time. The confirm button is outlined, never filled.
 */
@Composable
fun PurposeScreen(
    state: BlockState,
    onIntent: (BlockIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    BlockStepScaffold(onBack = { onIntent(BlockIntent.BackToStartClicked) }, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.block_purpose_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(modifier = Modifier.selectableGroup()) {
                    state.purposes.forEach { purpose ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = purpose == state.selectedPurpose,
                                    onClick = { onIntent(BlockIntent.PurposeSelected(purpose)) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = purpose == state.selectedPurpose, onClick = null)
                            Text(
                                text = stringResource(purposeLabel(purpose)),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.block_duration_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accessMinutesOptions.forEach { minutes ->
                        FilterChip(
                            selected = minutes == state.selectedMinutes,
                            onClick = { onIntent(BlockIntent.MinutesSelected(minutes)) },
                            label = { Text(text = formatDuration(minutes)) }
                        )
                    }
                }
                consequenceText(state)?.let { InfoBanner(text = it) }
            }
            OutlinedButton(
                onClick = { onIntent(BlockIntent.ConfirmClicked) },
                enabled = state.canConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                val minutes = state.selectedMinutes
                Text(
                    text = if (minutes != null) {
                        stringResource(R.string.block_open_for, formatDuration(minutes))
                    } else {
                        stringResource(R.string.block_open)
                    }
                )
            }
        }
    }
}

@StringRes
private fun purposeLabel(purpose: AccessPurpose): Int = when (purpose) {
    AccessPurpose.REPLY_TO_MESSAGE -> R.string.block_purpose_reply
    AccessPurpose.CHECK_SOMETHING -> R.string.block_purpose_check
    AccessPurpose.WORK_OR_STUDY -> R.string.block_purpose_work
    AccessPurpose.OTHER -> R.string.block_purpose_other
}

@Composable
private fun consequenceText(state: BlockState): String? = when (state.consequence) {
    BypassConsequence.NONE -> null
    BypassConsequence.USES_FREEZE ->
        pluralStringResource(R.plurals.block_consequence_freeze, state.streakDays, state.streakDays)
    BypassConsequence.BREAKS_STREAK ->
        pluralStringResource(R.plurals.block_consequence_break, state.streakDays, state.streakDays)
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun PurposeScreenPreview() {
    PauseTheme {
        PurposeScreen(
            state = BlockState(
                isLoading = false,
                purposes = FrictionConfig.DEFAULT.purposes,
                accessMinutesOptions = FrictionConfig.DEFAULT.accessMinutes,
                selectedPurpose = AccessPurpose.REPLY_TO_MESSAGE,
                selectedMinutes = 5,
                streakDays = 12,
                freezes = 1,
                consequence = BypassConsequence.USES_FREEZE
            ),
            onIntent = {}
        )
    }
}
