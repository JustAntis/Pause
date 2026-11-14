package com.justantis.pause.presentation.block

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.presentation.components.appLocale
import com.justantis.pause.presentation.theme.PauseTheme

/**
 * Screen 2 of the break flow: a breathing pause. "Next" is outlined and stays disabled until the
 * countdown ends.
 */
@Composable
fun PauseScreen(
    state: BlockState,
    onIntent: (BlockIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onIntent(BlockIntent.PauseStarted) }

    BlockStepScaffold(onBack = { onIntent(BlockIntent.BackToStartClicked) }, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = accessOrdinal(state.accessOrdinal).uppercase(appLocale()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.block_breathe_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            BreathingCircles(secondsLeft = state.pauseSecondsLeft)
            Text(
                text = stringResource(R.string.block_breathe_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { onIntent(BlockIntent.NextClicked) },
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (state.canContinue) {
                        stringResource(R.string.block_next)
                    } else {
                        stringResource(R.string.block_next_in, state.pauseSecondsLeft)
                    }
                )
            }
        }
    }
}

@Composable
private fun accessOrdinal(ordinal: Int): String = when (ordinal) {
    1 -> stringResource(R.string.block_access_first)
    2 -> stringResource(R.string.block_access_second)
    3 -> stringResource(R.string.block_access_third)
    else -> stringResource(R.string.block_access_nth, ordinal)
}

@Composable
private fun BreathingCircles(secondsLeft: Int) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    val color = MaterialTheme.colorScheme.primary
    val description = pluralStringResource(R.plurals.block_seconds_left, secondsLeft, secondsLeft)

    Box(
        modifier = Modifier
            .size(240.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(color = color.copy(alpha = 0.12f), radius = radius * scale)
            drawCircle(color = color.copy(alpha = 0.22f), radius = radius * 0.75f * scale)
            drawCircle(color = color.copy(alpha = 0.38f), radius = radius * 0.5f * scale)
        }
        Text(
            text = secondsLeft.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun PauseScreenPreview() {
    PauseTheme {
        PauseScreen(
            state = BlockState(isLoading = false, accessOrdinal = 2, pauseSeconds = 30, pauseSecondsLeft = 17),
            onIntent = {}
        )
    }
}
