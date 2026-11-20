package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntError
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
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = HevyntTextPrimary,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                color = HevyntTextSecondary,
                fontSize = 14.sp,
            )
        }
    }
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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = HevyntSurfaceRaised,
            contentColor = HevyntTextPrimary,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
            ) {
                if (title != null) {
                    Surface(color = Color.Transparent, contentColor = HevyntTextPrimary) {
                        title()
                    }
                    if (text != null) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (text != null) {
                    Surface(color = Color.Transparent, contentColor = HevyntTextSecondary) {
                        text()
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissButton != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            dismissButton()
                        }
                    }
                    Box(modifier = if (dismissButton != null) Modifier.weight(1f) else Modifier.fillMaxWidth()) {
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
internal fun HevyntPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = HevyntAccent,
            contentColor = Color.White,
            disabledContainerColor = HevyntAccent.copy(alpha = 0.32f),
            disabledContentColor = Color.White.copy(alpha = 0.45f),
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
internal fun HevyntTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor = if (destructive) HevyntAccentContainer else HevyntSurfaceInteractive
    val textColor = when {
        !enabled -> HevyntTextSecondary.copy(alpha = 0.5f)
        destructive -> HevyntError
        else -> HevyntTextPrimary
    }
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = textColor,
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
internal fun HevyntOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (accent) HevyntAccentContainer else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (accent) HevyntAccent.copy(alpha = 0.7f) else HevyntBorderSubtle,
        ),
        contentColor = when {
            !enabled -> HevyntTextSecondary.copy(alpha = 0.5f)
            accent -> HevyntAccent
            else -> HevyntTextPrimary
        },
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
internal fun HevyntTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 50.dp,
    maxLines: Int = if (singleLine) 1 else 5,
    containerColor: Color = HevyntSurfaceInteractive,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = HevyntTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .then(
                    if (isFocused) {
                        Modifier.border(1.5.dp, HevyntAccent, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 10.dp)) {
                    leadingIcon()
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = HevyntTextPrimary,
                    fontSize = 15.5.sp,
                ),
                cursorBrush = SolidColor(HevyntAccent),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                color = HevyntTextSecondary,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (trailingIcon != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
internal fun hevyntSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = HevyntAccent,
    checkedBorderColor = HevyntAccent,
    uncheckedThumbColor = HevyntTextSecondary,
    uncheckedTrackColor = HevyntSurfaceRaised,
    uncheckedBorderColor = HevyntBorderSubtle,
    disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
    disabledCheckedTrackColor = HevyntAccent.copy(alpha = 0.35f),
    disabledUncheckedThumbColor = HevyntTextSecondary.copy(alpha = 0.4f),
    disabledUncheckedTrackColor = HevyntSurfaceRaised.copy(alpha = 0.5f),
)

@Composable
internal fun HevyntChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
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
        border = if (active) BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.7f)) else null,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingDotColor?.let { dotColor ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape),
                )
            }
            Text(
                text = text,
                color = if (active) HevyntAccent else HevyntTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun HevyntMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    highlight: Boolean = false,
    valueFontSize: TextUnit = 28.sp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val valueColor = if (highlight) HevyntAccent else HevyntTextPrimary
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        Row {
            Text(
                text = value,
                fontFamily = HevyntDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = valueFontSize,
                color = valueColor,
                modifier = Modifier.alignByBaseline(),
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontFamily = HevyntDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = (valueFontSize.value * 0.62f).sp,
                    color = valueColor,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = HevyntTextSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun HevyntConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                color = HevyntTextSecondary,
            )
        },
        confirmButton = {
            HevyntPrimaryButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = confirmText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        dismissButton = {
            HevyntTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = dismissText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
internal fun HevyntInfoDialog(
    title: String,
    message: String,
    buttonText: String = "OK",
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                color = HevyntTextSecondary,
            )
        },
        confirmButton = {
            HevyntTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

internal data class HevyntSheetAction(
    val label: String,
    val icon: ImageVector? = null,
    val subtitle: String? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HevyntSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = HevyntSurfaceRaised,
        contentColor = HevyntTextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(HevyntBorderSubtle, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 20.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.5.sp,
                        color = HevyntTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

@Composable
internal fun HevyntSheetSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = HevyntSurfaceInteractive,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
internal fun HevyntSheetRow(
    label: String,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    subtitle: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f
    val labelColor = when {
        destructive -> HevyntError.copy(alpha = alpha)
        else -> HevyntTextPrimary.copy(alpha = alpha)
    }
    val iconTint = when {
        destructive -> HevyntError.copy(alpha = alpha)
        else -> HevyntTextPrimary.copy(alpha = alpha)
    }
    val iconBg = if (destructive) HevyntAccentContainer else HevyntSurfaceRaised

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = HevyntTextSecondary.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
internal fun HevyntActionSheet(
    actions: List<HevyntSheetAction>,
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
) {
    val regularActions = actions.filterNot { it.destructive }
    val destructiveActions = actions.filter { it.destructive }

    HevyntSheet(
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle,
    ) {
        if (regularActions.isNotEmpty()) {
            HevyntSheetSection {
                regularActions.forEachIndexed { index, action ->
                    if (index > 0) {
                        HevyntDottedRule(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = HevyntOutline.copy(alpha = 0.7f),
                        )
                    }
                    HevyntSheetRow(
                        label = action.label,
                        icon = action.icon,
                        subtitle = action.subtitle,
                        destructive = false,
                        enabled = action.enabled,
                        onClick = {
                            onDismiss()
                            action.onClick()
                        },
                    )
                }
            }
        }
        if (destructiveActions.isNotEmpty()) {
            if (regularActions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
            }
            HevyntSheetSection {
                destructiveActions.forEachIndexed { index, action ->
                    if (index > 0) {
                        HevyntDottedRule(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = HevyntOutline.copy(alpha = 0.7f),
                        )
                    }
                    HevyntSheetRow(
                        label = action.label,
                        icon = action.icon,
                        subtitle = action.subtitle,
                        destructive = true,
                        enabled = action.enabled,
                        onClick = {
                            onDismiss()
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
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
            color = HevyntSurfaceRaised.copy(alpha = 0.76f),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, HevyntOutline.copy(alpha = 0.55f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        painter = painterResource(R.drawable.ic_nav_dumbbell),
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
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (selected) HevyntSurfaceInteractive.copy(alpha = 0.65f) else Color.Transparent,
            )
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
