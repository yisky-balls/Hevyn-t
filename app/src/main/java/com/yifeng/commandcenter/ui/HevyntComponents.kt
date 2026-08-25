package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntCanvas
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

internal val HevyntPageHorizontalPadding = 20.dp

@Composable
internal fun HevyntPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displayLarge)
            Text(
                subtitle,
                modifier = Modifier.padding(top = 7.dp),
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.width(20.dp))
        HevyntDotGrid(Modifier.size(32.dp))
    }
}

@Composable
internal fun HevyntSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HevyntTextPrimary,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontFamily = HevyntDisplayFont,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    )
}

@Composable
internal fun HevyntSignalDot(modifier: Modifier = Modifier, color: Color = HevyntAccent) {
    Box(modifier.size(6.dp).background(color, CircleShape))
}

@Composable
internal fun HevyntDottedRule(
    modifier: Modifier = Modifier,
    color: Color = HevyntDivider,
) {
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        val step = 7.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawCircle(color, 0.7.dp.toPx(), Offset(x, size.height / 2))
            x += step
        }
    }
}

@Composable
internal fun HevyntVerticalDottedRule(
    modifier: Modifier = Modifier,
    color: Color = HevyntDivider,
) {
    Canvas(modifier.width(2.dp).fillMaxHeight()) {
        val step = 7.dp.toPx()
        var y = 0f
        while (y <= size.height) {
            drawCircle(color, 0.7.dp.toPx(), Offset(size.width / 2, y))
            y += step
        }
    }
}

@Composable
internal fun HevyntDotGrid(
    modifier: Modifier = Modifier,
    color: Color = HevyntTextSecondary,
) {
    Canvas(modifier.size(30.dp)) {
        val radius = 1.7.dp.toPx()
        val gap = size.minDimension / 4f
        repeat(3) { row ->
            repeat(3) { column ->
                drawCircle(color, radius, Offset(gap * (column + 1), gap * (row + 1)))
            }
        }
    }
}

@Composable
internal fun HevyntPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        shape = MaterialTheme.shapes.medium,
        content = content,
    )
}

@Composable
internal fun HevyntDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.large,
        containerColor = HevyntSurfaceRaised,
        titleContentColor = HevyntTextPrimary,
        textContentColor = HevyntTextSecondary,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

@Composable
internal fun HevyntPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = HevyntAccent,
            contentColor = HevyntCanvas,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
internal fun HevyntOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (accent) HevyntAccent else HevyntOutline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (accent) HevyntAccent else HevyntTextPrimary,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
internal fun hevyntTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = HevyntTextPrimary,
    unfocusedTextColor = HevyntTextPrimary,
    focusedBorderColor = HevyntAccent,
    unfocusedBorderColor = HevyntOutline,
    focusedLabelColor = HevyntAccent,
    unfocusedLabelColor = HevyntTextSecondary,
    cursorColor = HevyntAccent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)