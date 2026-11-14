package com.justantis.pause.presentation.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries


@Composable
fun StatsScreen(){

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        ChooseTimeLimit()
        ScreenTimeChart()
        AppUnlockedLocked()
        AppsScreenTimeCard()
    }
}

@Composable
fun ChooseTimeLimit(){
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StatsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = false,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index, StatsPeriod.entries.size)
            ) {
                Text(stringResource(period.labelRes))
            }
        }
    }
}

@Composable
fun ScreenTimeChart(modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            columnSeries { series(3.2, 2.1, 4.5, 1.8, 2.9, 5.0, 2.2) }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.height(200.dp),
            )
            Text(
                text = stringResource(R.string.stats_chart_above_goal, formatDuration(180)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppUnlockedLocked(modifier: Modifier = Modifier){
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "41",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(text = stringResource(R.string.stats_unlocks_per_day), style = MaterialTheme.typography.titleMedium)
            }
        }

        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(text = stringResource(R.string.stats_limit_bypasses), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AppsScreenTimeCard() {
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
            Text(text = stringResource(R.string.stats_most_used_apps), style = MaterialTheme.typography.titleLarge)
            AppScreenTime(appName = "Instagram", usedMinutes = 312)
            AppScreenTime(appName = "Facebook", usedMinutes = 105)
            AppScreenTime(appName = "Telegram", usedMinutes = 208)
        }
    }
}

@Composable
private fun AppScreenTime(appName: String, usedMinutes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(text = appName, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDuration(usedMinutes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}



@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun StatsScreenPreview(){
    PauseTheme() {
        StatsScreen()
    }
}