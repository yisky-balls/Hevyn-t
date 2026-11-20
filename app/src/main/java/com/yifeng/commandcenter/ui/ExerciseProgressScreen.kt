package com.yifeng.commandcenter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.ExerciseProgressCalculator
import com.yifeng.commandcenter.history.ExerciseProgressMetric
import com.yifeng.commandcenter.history.ExerciseProgressOption
import com.yifeng.commandcenter.history.ExerciseProgressPeriod
import com.yifeng.commandcenter.history.ExerciseProgressSummary
import com.yifeng.commandcenter.history.ExerciseSessionPoint
import com.yifeng.commandcenter.history.HistoricalExerciseSetRow
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceElevated
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun ExerciseProgressHomeCard(
    rows: List<HistoricalExerciseSetRow>,
    options: List<ExerciseProgressOption>,
    selectedExerciseKey: String?,
    metric: ExerciseProgressMetric,
    onSelectMetric: (ExerciseProgressMetric) -> Unit,
    onOpenPicker: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val summary = remember(rows, selectedExerciseKey, metric) {
        val key = selectedExerciseKey ?: return@remember null
        val recent = ExerciseProgressCalculator.summarize(
            rows = rows,
            exerciseKey = key,
            metric = metric,
            period = ExerciseProgressPeriod.DAYS_90,
        )
        if (recent != null && recent.points.size >= 2) {
            recent
        } else {
            ExerciseProgressCalculator.summarize(
                rows = rows,
                exerciseKey = key,
                metric = metric,
                period = ExerciseProgressPeriod.ALL,
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = options.isNotEmpty(), onClick = onOpenDetail),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(color = HevyntRestBlue)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Exercise progress",
                        color = HevyntTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                if (options.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    val label = summary?.exerciseName
                        ?: options.firstOrNull { it.normalizedKey == selectedExerciseKey }?.displayName
                        ?: "Select"
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = HevyntSurfaceInteractive,
                        border = BorderStroke(1.dp, HevyntBorderSubtle),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenPicker),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 11.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                color = HevyntTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(end = 2.dp),
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Switch exercise",
                                tint = HevyntTextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (options.isEmpty() || summary == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Complete workouts with logged weights to track exercise progression.",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
                return@Column
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = summary.latestValue?.let { formatMetricValue(it, metric) } ?: "—",
                    fontFamily = HevyntDisplayFont,
                    fontSize = 28.sp,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                summary.deltaValue?.let { delta ->
                    val formattedDelta = formatMetricDelta(delta, metric)
                    val sign = if (delta > 0.0 && !formattedDelta.startsWith("0")) "+" else ""
                    Text(
                        text = "$sign$formattedDelta",
                        color = if (delta >= 0) HevyntRestBlue else HevyntAccent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Compact metric switcher
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExerciseProgressMetric.entries.forEach { item ->
                    val selected = item == metric
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) HevyntRestBlue else HevyntSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectMetric(item) },
                    ) {
                        Text(
                            text = item.shortLabel,
                            color = if (selected) Color.Black else HevyntTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ExerciseProgressLineChart(
                points = summary.points,
                metric = metric,
                interactive = false,
                chartHeight = 124.dp,
            )
        }
    }
}

@Composable
fun ExerciseProgressScreen(
    rows: List<HistoricalExerciseSetRow>,
    options: List<ExerciseProgressOption>,
    selectedExerciseKey: String?,
    metric: ExerciseProgressMetric,
    period: ExerciseProgressPeriod,
    onSelectMetric: (ExerciseProgressMetric) -> Unit,
    onSelectPeriod: (ExerciseProgressPeriod) -> Unit,
    onOpenPicker: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val summary = remember(rows, selectedExerciseKey, metric, period) {
        selectedExerciseKey?.let { key ->
            ExerciseProgressCalculator.summarize(
                rows = rows,
                exerciseKey = key,
                metric = metric,
                period = period,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HevyntPageHorizontalPadding)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = HevyntTextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Exercise progress",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                )
                Text(
                    text = "${options.size} tracked exercises",
                    fontSize = 12.sp,
                    color = HevyntTextSecondary,
                )
            }
            if (options.isNotEmpty()) {
                val label = summary?.exerciseName ?: "Select"
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HevyntSurfaceInteractive,
                    border = BorderStroke(1.dp, HevyntBorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenPicker),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            color = HevyntTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Switch exercise",
                            tint = HevyntTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Period selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExerciseProgressPeriod.entries.forEach { item ->
                val isSelected = item == period
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) HevyntAccent else HevyntSurfaceRaised,
                    border = if (isSelected) null else BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectPeriod(item) },
                ) {
                    Text(
                        text = item.label,
                        color = if (isSelected) Color.White else HevyntTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Metric selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExerciseProgressMetric.entries.forEach { item ->
                val isSelected = item == metric
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) HevyntRestBlue else HevyntSurfaceRaised,
                    border = if (isSelected) null else BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectMetric(item) },
                ) {
                    Text(
                        text = item.fullLabel,
                        color = if (isSelected) Color.Black else HevyntTextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (summary == null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = HevyntSurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No logged sessions found for this exercise.",
                        color = HevyntTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            return
        }

        // Main Chart Card
        ExerciseProgressDetailChartCard(summary = summary)

        Spacer(Modifier.height(14.dp))

        // Session breakdown list
        if (summary.points.isNotEmpty()) {
            Text(
                text = "Sessions (${summary.points.size})",
                color = HevyntTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = HevyntSurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val descending = summary.points.asReversed()
                    descending.forEachIndexed { idx, point ->
                        if (idx > 0) {
                            HevyntDottedRule(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                color = HevyntOutline.copy(alpha = 0.7f),
                            )
                        }
                        ExerciseSessionHistoryRow(point = point, metric = metric)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressDetailChartCard(
    summary: ExerciseProgressSummary,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = summary.exerciseName,
                color = HevyntTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = summary.latestValue?.let { formatMetricValue(it, summary.metric) } ?: "—",
                    fontFamily = HevyntDisplayFont,
                    fontSize = 32.sp,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                summary.deltaValue?.let { delta ->
                    val formattedDelta = formatMetricDelta(delta, summary.metric)
                    val sign = if (delta > 0.0 && !formattedDelta.startsWith("0")) "+" else ""
                    Text(
                        text = "$sign$formattedDelta",
                        color = if (delta >= 0) HevyntRestBlue else HevyntAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }

            summary.points.lastOrNull()?.let { latest ->
                Text(
                    text = "Latest · ${formatEpochDayShort(latest.dateEpochDay)}",
                    color = HevyntTextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            ExerciseProgressLineChart(
                points = summary.points,
                metric = summary.metric,
                interactive = true,
                chartHeight = 184.dp,
            )

            Spacer(Modifier.height(14.dp))
            HevyntDottedRule(color = HevyntOutline.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ProgressStatColumn(
                    label = "Period best",
                    value = summary.periodBest?.let { formatMetricValue(it, summary.metric) } ?: "—",
                )
                ProgressStatColumn(
                    label = "All-time PR",
                    value = summary.allTimePr?.let { formatMetricValue(it, summary.metric) } ?: "—",
                )
                ProgressStatColumn(
                    label = "Sessions",
                    value = "${summary.sessionCount}",
                )
            }
        }
    }
}

@Composable
private fun ProgressStatColumn(
    label: String,
    value: String,
) {
    Column {
        Text(text = label, color = HevyntTextSecondary, fontSize = 11.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = HevyntTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ExerciseSessionHistoryRow(
    point: ExerciseSessionPoint,
    metric: ExerciseProgressMetric,
) {
    val setsText = remember(point.sets) {
        point.sets.mapNotNull { set ->
            formatCompactSet(set)
        }.joinToString("  ·  ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = formatEpochDayShort(point.dateEpochDay),
                color = HevyntTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (setsText.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = setsText,
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = point.valueFor(metric)?.let { formatMetricValue(it, metric) } ?: "—",
            color = HevyntRestBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun ExerciseProgressLineChart(
    points: List<ExerciseSessionPoint>,
    metric: ExerciseProgressMetric,
    interactive: Boolean,
    chartHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val plotted = remember(points, metric) {
        points.mapNotNull { pt -> pt.valueFor(metric)?.let { pt to it } }
    }
    if (plotted.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No entries in this period.",
                color = HevyntTextSecondary,
                fontSize = 13.sp,
            )
        }
        return
    }

    var selectedIndex by remember(plotted) { mutableStateOf<Int?>(null) }

    val minValRaw = plotted.minOf { it.second }
    val maxValRaw = plotted.maxOf { it.second }
    val step = if (metric == ExerciseProgressMetric.SESSION_VOLUME) 50.0 else 2.5
    val spread = (maxValRaw - minValRaw).coerceAtLeast(step)
    val yMin = (floor((minValRaw - spread * 0.15) / step) * step).coerceAtLeast(0.0)
    val yMax = ceil((maxValRaw + spread * 0.15) / step) * step
    val yRange = (yMax - yMin).coerceAtLeast(step)
    val yMid = (yMin + yMax) / 2.0

    val minDay = plotted.first().first.dateEpochDay
    val maxDay = plotted.last().first.dateEpochDay
    val daySpan = (maxDay - minDay).coerceAtLeast(1L)

    val selected = selectedIndex?.let { plotted.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (interactive && selected != null) {
            val (pt, value) = selected
            val topSetSuffix = if (metric == ExerciseProgressMetric.TOP_WEIGHT &&
                pt.topWeightKg != null && pt.topSetReps != null
            ) {
                " (${formatWeight(pt.topWeightKg)} × ${pt.topSetReps})"
            } else {
                ""
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HevyntSurfaceElevated,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "${formatEpochDayShort(pt.dateEpochDay)} · ${formatMetricValue(value, metric)}$topSetSuffix",
                    color = HevyntTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .height(chartHeight)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatAxisValue(yMax, metric), color = HevyntTextSecondary, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                Text(formatAxisValue(yMid, metric), color = HevyntTextSecondary, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                Text(formatAxisValue(yMin, metric), color = HevyntTextSecondary, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
            }

            val canvasModifier = Modifier
                .weight(1f)
                .height(chartHeight)
                .let { base ->
                    if (!interactive) base
                    else base
                        .pointerInput(plotted) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val idx = nearestSessionIndex(plotted, minDay, daySpan, offset.x, size.width.toFloat())
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                    tryAwaitRelease()
                                },
                            )
                        }
                        .pointerInput(plotted) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, _ ->
                                    selectedIndex = nearestSessionIndex(
                                        plotted,
                                        minDay,
                                        daySpan,
                                        change.position.x,
                                        size.width.toFloat(),
                                    )
                                },
                            )
                        }
                }

            Canvas(modifier = canvasModifier) {
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val padHorizontal = 8.dp.toPx()
                val chartW = (size.width - padHorizontal * 2f).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                fun xForIndex(index: Int, day: Long): Float = when {
                    plotted.size == 1 -> padHorizontal + chartW / 2f
                    maxDay == minDay -> padHorizontal + (index.toFloat() / (plotted.size - 1).toFloat()) * chartW
                    else -> padHorizontal + ((day - minDay).toFloat() / daySpan.toFloat()) * chartW
                }

                fun yForValue(v: Double): Float {
                    val ratio = ((v - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    return padTop + chartH * (1f - ratio)
                }

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = padTop + chartH * frac
                    drawLine(
                        color = HevyntOutline.copy(alpha = 0.7f),
                        start = Offset(padHorizontal, y),
                        end = Offset(size.width - padHorizontal, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                }

                val coords = plotted.mapIndexed { idx, (pt, value) ->
                    Offset(xForIndex(idx, pt.dateEpochDay), yForValue(value))
                }

                if (coords.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, padTop + chartH)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, padTop + chartH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HevyntRestBlue.copy(alpha = 0.28f),
                                HevyntRestBlue.copy(alpha = 0.0f),
                            ),
                            startY = padTop,
                            endY = padTop + chartH,
                        ),
                    )

                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) lineTo(coords[i].x, coords[i].y)
                    }
                    drawPath(
                        path = linePath,
                        color = HevyntRestBlue,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                val showAllDots = coords.size <= 35
                coords.forEachIndexed { idx, pt ->
                    val isSelected = interactive && idx == selectedIndex
                    if (isSelected) {
                        drawLine(
                            color = HevyntTextSecondary.copy(alpha = 0.45f),
                            start = Offset(pt.x, padTop),
                            end = Offset(pt.x, padTop + chartH),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawCircle(
                            color = HevyntRestBlue.copy(alpha = 0.3f),
                            radius = 9.dp.toPx(),
                            center = pt,
                        )
                        drawCircle(
                            color = HevyntTextPrimary,
                            radius = 4.5.dp.toPx(),
                            center = pt,
                        )
                    } else if (showAllDots || idx == coords.lastIndex) {
                        drawCircle(
                            color = HevyntRestBlue,
                            radius = if (idx == coords.lastIndex) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatEpochDayShort(minDay), color = HevyntTextSecondary, fontSize = 11.sp)
            if (maxDay - minDay >= 4L) {
                Text(formatEpochDayShort((minDay + maxDay) / 2L), color = HevyntTextSecondary, fontSize = 11.sp)
            }
            if (maxDay > minDay) {
                Text(formatEpochDayShort(maxDay), color = HevyntTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ExercisePickerSheet(
    options: List<ExerciseProgressOption>,
    selectedExerciseKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(options, query) {
        val q = query.trim()
        if (q.isEmpty()) options
        else options.filter { it.displayName.contains(q, ignoreCase = true) }
    }

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Choose exercise",
        subtitle = "Track progression on your Home screen",
    ) {
        HevyntTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search exercise…",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No matching exercises",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
            }
        } else {
            HevyntSheetSection {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.normalizedKey }) { index, option ->
                        if (index > 0) {
                            HevyntDottedRule(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = HevyntOutline.copy(alpha = 0.7f),
                            )
                        }
                        val isSelected = option.normalizedKey == selectedExerciseKey
                        val bestSuffix = option.bestWeightKg?.let { " · best ${formatWeight(it)} kg" }.orEmpty()
                        HevyntSheetRow(
                            label = option.displayName,
                            subtitle = "${option.sessionCount} session${if (option.sessionCount == 1) "" else "s"}$bestSuffix",
                            onClick = { onSelect(option.normalizedKey) },
                            trailing = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = HevyntRestBlue,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun nearestSessionIndex(
    plotted: List<Pair<ExerciseSessionPoint, Double>>,
    minDay: Long,
    daySpan: Long,
    touchX: Float,
    width: Float,
): Int {
    if (plotted.size <= 1 || width <= 0f) return 0
    val normalized = (touchX / width).coerceIn(0f, 1f)
    if (plotted.last().first.dateEpochDay == minDay) {
        return (normalized * (plotted.size - 1)).roundToInt().coerceIn(0, plotted.lastIndex)
    }
    val targetDay = minDay + normalized * daySpan
    var bestIdx = 0
    var bestDist = Float.MAX_VALUE
    plotted.forEachIndexed { index, (pt, _) ->
        val dist = abs((pt.dateEpochDay - targetDay).toFloat())
        if (dist < bestDist) {
            bestDist = dist
            bestIdx = index
        }
    }
    return bestIdx
}

private fun formatMetricValue(value: Double, metric: ExerciseProgressMetric): String = when (metric) {
    ExerciseProgressMetric.TOP_WEIGHT -> "${formatWeight(value, maxDecimals = 2)} kg"
    ExerciseProgressMetric.ESTIMATED_1RM -> "${formatWeight(value, maxDecimals = 1)} kg"
    ExerciseProgressMetric.SESSION_VOLUME -> "${NumberFormat.getNumberInstance(Locale.US).format(value.roundToInt())} kg"
}

private fun formatMetricDelta(delta: Double, metric: ExerciseProgressMetric): String = when (metric) {
    ExerciseProgressMetric.TOP_WEIGHT,
    ExerciseProgressMetric.ESTIMATED_1RM -> "${formatWeight(delta, maxDecimals = 1)} kg"
    ExerciseProgressMetric.SESSION_VOLUME -> "${NumberFormat.getNumberInstance(Locale.US).format(delta.roundToInt())} kg"
}

private fun formatAxisValue(value: Double, metric: ExerciseProgressMetric): String = when (metric) {
    ExerciseProgressMetric.TOP_WEIGHT,
    ExerciseProgressMetric.ESTIMATED_1RM -> formatWeight(value, maxDecimals = 1)
    ExerciseProgressMetric.SESSION_VOLUME -> NumberFormat.getNumberInstance(Locale.US).format(value.roundToInt())
}

private fun formatCompactSet(set: PreviousSetPerformance): String? {
    val w = set.weightKg
    val r = set.repetitions
    return when {
        w != null && r != null -> "${formatWeight(w)}×$r"
        w != null -> "${formatWeight(w)} kg"
        r != null -> "$r reps"
        else -> null
    }
}
