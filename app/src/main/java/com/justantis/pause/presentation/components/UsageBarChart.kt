package com.justantis.pause.presentation.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

/** NORMAL bars are muted, HIGHLIGHT marks today, WARNING marks a value over the goal or limit. */
enum class BarStyle { NORMAL, HIGHLIGHT, WARNING }

/** [spokenLabel] is what screen readers say for the bar; the axis shows the short [label]. */
data class ChartBar(val minutes: Int, val label: String, val style: BarStyle, val spokenLabel: String = label)

private val StylesKey = ExtraStore.Key<List<BarStyle>>()
private val LabelsKey = ExtraStore.Key<List<String>>()

/**
 * A column chart of minutes per bar, based on the Vico setup of the original statistics chart.
 * Scrolling and zooming are off and the whole range is fitted to the width, so every bar of the
 * period is visible. Screen readers get one description listing every bar's label and value.
 * Each bar is coloured by its [BarStyle]; [labelSpacing] shows every n-th bottom label and
 * [axisStepMinutes] sets the step of the vertical axis.
 */
@Composable
fun UsageBarChart(
    bars: List<ChartBar>,
    axisStepMinutes: Int,
    modifier: Modifier = Modifier,
    labelSpacing: Int = 1
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(bars) {
        if (bars.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries { series(bars.map { it.minutes }) }
            extras { store ->
                store[StylesKey] = bars.map { it.style }
                store[LabelsKey] = bars.map { it.label }
            }
        }
    }

    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    val thickness = if (bars.size > 12) 6.dp else 16.dp
    val normal = rememberLineComponent(Fill(MaterialTheme.colorScheme.primaryContainer), thickness, shape)
    val highlight = rememberLineComponent(Fill(MaterialTheme.colorScheme.primary), thickness, shape)
    val warning = rememberLineComponent(Fill(MaterialTheme.colorScheme.error), thickness, shape)
    val columnProvider = remember(normal, highlight, warning) {
        StyledColumnProvider(normal, highlight, warning)
    }

    val durations = rememberDurationTexts()
    val description = remember(bars, durations) {
        bars.joinToString(separator = ", ") { "${it.spokenLabel} ${durations.format(it.minutes)}" }
    }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val axisLabel = rememberAxisLabelComponent(style = labelStyle)
    val verticalFormatter = remember(durations) {
        CartesianValueFormatter { _, value, _ -> durations.format(value.toInt()) }
    }
    val bottomFormatter = remember {
        CartesianValueFormatter { context, value, _ ->
            context.model.extraStore.getOrNull(LabelsKey)?.getOrNull(value.toInt()) ?: ""
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(columnProvider = columnProvider),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                valueFormatter = verticalFormatter,
                itemPlacer = remember(axisStepMinutes) { VerticalAxis.ItemPlacer.step({ axisStepMinutes.toDouble() }) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                valueFormatter = bottomFormatter,
                itemPlacer = remember(labelSpacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }) }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .height(200.dp)
            .clearAndSetSemantics { contentDescription = description },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, minZoom = Zoom.Content)
    )
}

private class StyledColumnProvider(
    private val normal: LineComponent,
    private val highlight: LineComponent,
    private val warning: LineComponent
) : ColumnCartesianLayer.ColumnProvider {

    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore
    ): LineComponent = when (extraStore.getOrNull(StylesKey)?.getOrNull(entry.x.toInt())) {
        BarStyle.HIGHLIGHT -> highlight
        BarStyle.WARNING -> warning
        else -> normal
    }

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent = normal
}
