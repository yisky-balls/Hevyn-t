package com.yifeng.commandcenter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftDivider
import com.yifeng.commandcenter.ui.theme.LiftMuted
import com.yifeng.commandcenter.ui.theme.LiftRed

internal val LiftPageHorizontalPadding = 24.dp

@Composable
internal fun LiftPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 92.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                letterSpacing = 2.sp,
            )
            Text(
                subtitle,
                modifier = Modifier.padding(top = 5.dp),
                color = LiftMuted,
                fontSize = 15.sp,
                letterSpacing = 0.45.sp,
            )
        }
        Spacer(Modifier.width(20.dp))
        DotGridMark(Modifier.size(30.dp))
    }
}

@Composable
internal fun LiftSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = DisplayFont,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp,
    )
}

@Composable
internal fun SignalDot(modifier: Modifier = Modifier, color: Color = LiftRed) {
    Box(modifier.size(6.dp).background(color, CircleShape))
}

@Composable
internal fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = LiftDivider,
) {
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        val step = 7.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawCircle(color = color, radius = 0.7.dp.toPx(), center = Offset(x, size.height / 2))
            x += step
        }
    }
}

@Composable
internal fun DottedVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = LiftDivider,
) {
    Canvas(modifier.width(2.dp).fillMaxHeight()) {
        val step = 7.dp.toPx()
        var y = 0f
        while (y <= size.height) {
            drawCircle(color = color, radius = 0.7.dp.toPx(), center = Offset(size.width / 2, y))
            y += step
        }
    }
}

@Composable
internal fun DotGridMark(
    modifier: Modifier = Modifier,
    color: Color = LiftMuted,
) {
    Canvas(modifier.size(30.dp)) {
        val radius = 1.8.dp.toPx()
        val gap = size.minDimension / 4f
        repeat(3) { row ->
            repeat(3) { column ->
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(gap * (column + 1), gap * (row + 1)),
                )
            }
        }
    }
}

@Composable
internal fun IndustrialDumbbell(
    modifier: Modifier = Modifier,
    color: Color = LiftRed,
) {
    Canvas(modifier.size(width = 48.dp, height = 28.dp)) {
        val centerY = size.height / 2f
        val stroke = 1.6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.22f, centerY),
            end = Offset(size.width * 0.78f, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
        listOf(0.16f, 0.22f, 0.78f, 0.84f).forEachIndexed { index, xFraction ->
            val tall = index == 0 || index == 3
            val plateHeight = size.height * if (tall) 0.72f else 0.5f
            drawLine(
                color = color,
                start = Offset(size.width * xFraction, centerY - plateHeight / 2f),
                end = Offset(size.width * xFraction, centerY + plateHeight / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
        }
        drawArc(
            color = color.copy(alpha = 0.8f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.84f, size.height * 0.76f),
            style = Stroke(width = 0.6.dp.toPx()),
        )
    }
}