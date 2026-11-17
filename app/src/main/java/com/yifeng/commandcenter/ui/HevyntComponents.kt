package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntCanvas
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

internal val HevyntPageHorizontalPadding = 12.dp

@Composable
internal fun HevyntPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = HevyntTextPrimary,
            letterSpacing = 0.5.sp,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun HevyntSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HevyntTextPrimary,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.6.sp,
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
internal fun HevyntVerticalDottedRule(
    modifier: Modifier = Modifier,
    color: Color = HevyntDivider,
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(color),
    )
}

@Composable
internal fun HevyntDotGrid(
    modifier: Modifier = Modifier,
    color: Color = HevyntTextSecondary,
) {
    Canvas(modifier.size(24.dp)) {
        val radius = 1.2.dp.toPx()
        val gap = size.minDimension / 4f
        repeat(3) { row ->
            repeat(3) { column ->
                drawCircle(color.copy(alpha = 0.4f), radius, Offset(gap * (column + 1), gap * (row + 1)))
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
        shape = RoundedCornerShape(24.dp),
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
        modifier = modifier.defaultMinSize(minHeight = 50.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = HevyntAccent,
            contentColor = Color.White,
            disabledContainerColor = HevyntAccent.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.5f),
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (accent) HevyntAccentContainer else HevyntSurfaceInteractive,
        border = BorderStroke(1.dp, if (accent) HevyntAccent.copy(alpha = 0.7f) else HevyntOutline),
        contentColor = if (accent) HevyntAccent else HevyntTextPrimary,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
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

@Composable
internal fun HevyntChip(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    leadingDotColor: Color? = null,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            onClick?.let {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                it()
            }
        },
        enabled = onClick != null,
        color = if (active) HevyntAccentContainer else HevyntSurfaceInteractive,
        border = BorderStroke(1.dp, if (active) HevyntAccent.copy(alpha = 0.7f) else HevyntOutline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            leadingDotColor?.let { dotColor ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor, CircleShape),
                )
            }
            Text(
                text = text,
                color = if (active) HevyntAccent else HevyntTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
internal fun HevyntMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = HevyntTextSecondary,
            letterSpacing = 0.8.sp,
            fontSize = 9.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) HevyntAccent else HevyntTextPrimary,
            fontFamily = HevyntDisplayFont,
        )
    }
}

@Composable
internal fun HevyntConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "CONFIRM",
    dismissText: String = "CANCEL",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = HevyntTextSecondary)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else HevyntAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = HevyntTextSecondary)
            }
        },
    )
}

@Composable
internal fun HevyntInputDialog(
    title: String,
    initialValue: String = "",
    label: String = "Name",
    confirmText: String = "SAVE",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    colors = hevyntTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) onConfirm(trimmed)
                },
                enabled = text.isNotBlank(),
            ) {
                Text(confirmText, color = HevyntAccent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = HevyntTextSecondary)
            }
        },
    )
}

@Composable
fun HevyntBottomBar(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = HevyntSurfaceRaised,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HevyntBottomDestination(
                    label = "Home",
                    selected = selectedPage == AppPage.Home,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPageSelected(AppPage.Home)
                    },
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(22.dp))
                }
                HevyntBottomDestination(
                    label = "Workout",
                    selected = selectedPage == AppPage.Workout,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPageSelected(AppPage.Workout)
                    },
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.yifeng.commandcenter.R.drawable.ic_nav_dumbbell),
                        contentDescription = "Workout",
                        modifier = Modifier.size(22.dp),
                    )
                }
                HevyntBottomDestination(
                    label = "Journal",
                    selected = selectedPage == AppPage.Journal,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPageSelected(AppPage.Journal)
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = "Journal",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.HevyntBottomDestination(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val contentColor = if (selected) HevyntTextPrimary else HevyntTextSecondary
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(HevyntAccent, CircleShape),
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(3.dp))
            Surface(color = Color.Transparent, contentColor = contentColor) { icon() }
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}