package com.yifeng.commandcenter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.metrics.CaloriesPoint
import com.yifeng.commandcenter.metrics.WeightPoint
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceElevated
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

internal fun formatEpochDayShort(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(ShortDateFormatter)

internal fun formatWeightKg(value: Double): String =
    String.format(Locale.US, "%.1f", value)

internal fun formatCaloriesCount(value: Int): String =
    NumberFormat.getNumberInstance(Locale.US).format(value)

@Composable
fun WeightLineChart(
    points: List<WeightPoint>,
    movingAvgPoints: List<WeightPoint>,
    showMovingAverage: Boolean,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        EmptyChartPlaceholder(
            message = "No weight entries in this period.",
            modifier = modifier,
        )
        return
    }

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val minValRaw = points.minOf { it.weightKg }
    val maxValRaw = points.maxOf { it.weightKg }
    val spread = (maxValRaw - minValRaw).coerceAtLeast(1.0)
    val yMin = floor((minValRaw - spread * 0.15) * 2.0) / 2.0
    val yMax = ceil((maxValRaw + spread * 0.15) * 2.0) / 2.0
    val yRange = (yMax - yMin).coerceAtLeast(1.0)
    val yMid = (yMin + yMax) / 2.0

    val minDay = points.first().dateEpochDay
    val maxDay = points.last().dateEpochDay
    val daySpan = (maxDay - minDay).coerceAtLeast(1L)

    val selectedPoint = selectedIndex?.let { points.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HevyntSurfaceElevated,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "${formatEpochDayShort(selectedPoint.dateEpochDay)} · ${formatWeightKg(selectedPoint.weightKg)} kg",
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
                    .height(176.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatWeightKg(yMax), color = HevyntTextSecondary, fontSize = 10.5.sp)
                Text(formatWeightKg(yMid), color = HevyntTextSecondary, fontSize = 10.5.sp)
                Text(formatWeightKg(yMin), color = HevyntTextSecondary, fontSize = 10.5.sp)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val idx = nearestWeightIndex(points, minDay, daySpan, offset.x, size.width.toFloat())
                                selectedIndex = if (selectedIndex == idx) null else idx
                                tryAwaitRelease()
                            },
                        )
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onDragEnd = {},
                            onDragCancel = {},
                            onHorizontalDrag = { change, _ ->
                                selectedIndex = nearestWeightIndex(
                                    points,
                                    minDay,
                                    daySpan,
                                    change.position.x,
                                    size.width.toFloat(),
                                )
                            },
                        )
                    },
            ) {
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val padHorizontal = 8.dp.toPx()
                val chartW = (size.width - padHorizontal * 2f).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                fun xForDay(day: Long): Float = if (points.size == 1) {
                    padHorizontal + chartW / 2f
                } else {
                    padHorizontal + ((day - minDay).toFloat() / daySpan.toFloat()) * chartW
                }

                fun yForWeight(w: Double): Float {
                    val ratio = ((w - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    return padTop + chartH * (1f - ratio)
                }

                // Dotted horizontal gridlines
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

                val coords = points.map { Offset(xForDay(it.dateEpochDay), yForWeight(it.weightKg)) }

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
                                HevyntAccent.copy(alpha = 0.28f),
                                HevyntAccent.copy(alpha = 0.0f),
                            ),
                            startY = padTop,
                            endY = padTop + chartH,
                        ),
                    )

                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = HevyntAccent,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                // 7-day moving average dashed overlay
                if (showMovingAverage && movingAvgPoints.size >= 2) {
                    val avgPath = Path().apply {
                        movingAvgPoints.forEachIndexed { i, pt ->
                            val x = xForDay(pt.dateEpochDay)
                            val y = yForWeight(pt.weightKg)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = avgPath,
                        color = HevyntTextPrimary.copy(alpha = 0.65f),
                        style = Stroke(
                            width = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                        ),
                    )
                }

                // Data point dots
                val showAllDots = coords.size <= 35
                coords.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    if (isSelected) {
                        drawLine(
                            color = HevyntTextSecondary.copy(alpha = 0.45f),
                            start = Offset(pt.x, padTop),
                            end = Offset(pt.x, padTop + chartH),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawCircle(
                            color = HevyntAccent.copy(alpha = 0.3f),
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
                            color = HevyntAccent,
                            radius = if (idx == coords.lastIndex) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        DateAxisLabels(minDay = minDay, maxDay = maxDay, modifier = Modifier.padding(start = 36.dp))
    }
}

@Composable
fun CaloriesBarChart(
    points: List<CaloriesPoint>,
    averageCalories: Int?,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        EmptyChartPlaceholder(
            message = "No calorie entries in this period.",
            modifier = modifier,
        )
        return
    }

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val maxRaw = points.maxOf { it.caloriesKcal }
    val yMax = ((maxRaw / 250.0).let { ceil(it) } * 250.0).coerceAtLeast(500.0)
    val yMid = yMax / 2.0

    val minDay = points.first().dateEpochDay
    val maxDay = points.last().dateEpochDay
    val daySpan = (maxDay - minDay).coerceAtLeast(1L)

    val selectedPoint = selectedIndex?.let { points.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HevyntSurfaceElevated,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "${formatEpochDayShort(selectedPoint.dateEpochDay)} · ${formatCaloriesCount(selectedPoint.caloriesKcal)} kcal",
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
                    .height(168.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatCaloriesCount(yMax.toInt()), color = HevyntTextSecondary, fontSize = 10.5.sp)
                Text(formatCaloriesCount(yMid.toInt()), color = HevyntTextSecondary, fontSize = 10.5.sp)
                Text("0", color = HevyntTextSecondary, fontSize = 10.5.sp)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(168.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val idx = nearestCaloriesIndex(points, minDay, daySpan, offset.x, size.width.toFloat())
                                selectedIndex = if (selectedIndex == idx) null else idx
                                tryAwaitRelease()
                            },
                        )
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, _ ->
                                selectedIndex = nearestCaloriesIndex(
                                    points,
                                    minDay,
                                    daySpan,
                                    change.position.x,
                                    size.width.toFloat(),
                                )
                            },
                        )
                    },
            ) {
                val padTop = 10.dp.toPx()
                val padBottom = 6.dp.toPx()
                val padHorizontal = 10.dp.toPx()
                val chartW = (size.width - padHorizontal * 2f).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

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

                val totalSlots = (daySpan + 1L).coerceAtLeast(points.size.toLong()).toFloat()
                val barWidth = (chartW / totalSlots * 0.68f).coerceIn(4.dp.toPx(), 18.dp.toPx())

                points.forEachIndexed { idx, pt ->
                    val centerX = if (points.size == 1) {
                        padHorizontal + chartW / 2f
                    } else {
                        padHorizontal + ((pt.dateEpochDay - minDay).toFloat() / daySpan.toFloat()) * chartW
                    }
                    val ratio = (pt.caloriesKcal.toDouble() / yMax).toFloat().coerceIn(0f, 1f)
                    val barHeight = (chartH * ratio).coerceAtLeast(3.dp.toPx())
                    val topY = padTop + chartH - barHeight
                    val isSelected = idx == selectedIndex
                    val barColor = when {
                        isSelected -> HevyntTextPrimary
                        selectedIndex != null -> HevyntRestBlue.copy(alpha = 0.45f)
                        else -> HevyntRestBlue
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }

                // Average reference dashed line
                if (averageCalories != null) {
                    val avgRatio = (averageCalories.toDouble() / yMax).toFloat().coerceIn(0f, 1f)
                    val avgY = padTop + chartH * (1f - avgRatio)
                    drawLine(
                        color = HevyntTextPrimary.copy(alpha = 0.65f),
                        start = Offset(padHorizontal, avgY),
                        end = Offset(size.width - padHorizontal, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        DateAxisLabels(minDay = minDay, maxDay = maxDay, modifier = Modifier.padding(start = 40.dp))
    }
}

@Composable
fun WeightMiniSparkline(
    points: List<WeightPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    val minVal = points.minOf { it.weightKg }
    val maxVal = points.maxOf { it.weightKg }
    val range = (maxVal - minVal).coerceAtLeast(0.5)
    val minDay = points.first().dateEpochDay
    val daySpan = (points.last().dateEpochDay - minDay).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val pad = 4.dp.toPx()
        val w = (size.width - pad * 2f).coerceAtLeast(1f)
        val h = (size.height - pad * 2f).coerceAtLeast(1f)

        val coords = points.map { pt ->
            val x = pad + ((pt.dateEpochDay - minDay).toFloat() / daySpan.toFloat()) * w
            val y = pad + h * (1f - ((pt.weightKg - minVal) / range).toFloat().coerceIn(0f, 1f))
            Offset(x, y)
        }

        val fillPath = Path().apply {
            moveTo(coords.first().x, pad + h)
            coords.forEach { lineTo(it.x, it.y) }
            lineTo(coords.last().x, pad + h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(HevyntAccent.copy(alpha = 0.25f), Color.Transparent),
                startY = pad,
                endY = pad + h,
            ),
        )

        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) lineTo(coords[i].x, coords[i].y)
        }
        drawPath(
            path = linePath,
            color = HevyntAccent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(
            color = HevyntAccent,
            radius = 3.dp.toPx(),
            center = coords.last(),
        )
    }
}

@Composable
private fun DateAxisLabels(
    minDay: Long,
    maxDay: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatEpochDayShort(minDay),
            color = HevyntTextSecondary,
            fontSize = 11.sp,
        )
        if (maxDay - minDay >= 4L) {
            Text(
                text = formatEpochDayShort((minDay + maxDay) / 2L),
                color = HevyntTextSecondary,
                fontSize = 11.sp,
            )
        }
        if (maxDay > minDay) {
            Text(
                text = formatEpochDayShort(maxDay),
                color = HevyntTextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = HevyntTextSecondary,
            fontSize = 13.5.sp,
        )
    }
}

private fun nearestWeightIndex(
    points: List<WeightPoint>,
    minDay: Long,
    daySpan: Long,
    touchX: Float,
    width: Float,
): Int {
    if (points.size <= 1 || width <= 0f) return 0
    val normalized = (touchX / width).coerceIn(0f, 1f)
    val targetDay = minDay + normalized * daySpan
    var bestIdx = 0
    var bestDist = Float.MAX_VALUE
    points.forEachIndexed { index, pt ->
        val dist = abs((pt.dateEpochDay - targetDay).toFloat())
        if (dist < bestDist) {
            bestDist = dist
            bestIdx = index
        }
    }
    return bestIdx
}

private fun nearestCaloriesIndex(
    points: List<CaloriesPoint>,
    minDay: Long,
    daySpan: Long,
    touchX: Float,
    width: Float,
): Int {
    if (points.size <= 1 || width <= 0f) return 0
    val normalized = (touchX / width).coerceIn(0f, 1f)
    val targetDay = minDay + normalized * daySpan
    var bestIdx = 0
    var bestDist = Float.MAX_VALUE
    points.forEachIndexed { index, pt ->
        val dist = abs((pt.dateEpochDay - targetDay).toFloat())
        if (dist < bestDist) {
            bestDist = dist
            bestIdx = index
        }
    }
    return bestIdx
}
