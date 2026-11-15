package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.history.ProgressionTrend
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun SetHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetColumnLabel("Set", Modifier.width(34.dp), TextAlign.Center)
        SetColumnLabel("Previous · tap to fill", Modifier.weight(1f).padding(start = 6.dp))
        SetColumnLabel("kg", Modifier.width(54.dp), TextAlign.Center)
        SetColumnLabel("Reps", Modifier.width(54.dp), TextAlign.Center)
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = HevyntTextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun SetColumnLabel(
    label: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = label,
        modifier = modifier,
        color = HevyntTextSecondary,
        style = MaterialTheme.typography.labelSmall,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
internal fun SetRow(
    index: Int,
    set: WorkoutSet,
    previous: PreviousSetPerformance?,
    precedingSet: WorkoutSet?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onChanged: (WorkoutSet) -> Unit,
    onCompleted: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCopy: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val progression = remember(set.weight, set.reps, previous) {
        PreviousSetResolver.evaluateProgression(set.weight, set.reps, previous)
    }

    val isActive = !set.completed && isSelected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = { menuExpanded = true },
                color = Color.Transparent,
                shape = CircleShape,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "Set ${index + 1} options" },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (isActive) HevyntAccent else if (set.completed) HevyntTextPrimary else HevyntTextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = HevyntSurfaceInteractive,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "DUPLICATE SET",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDuplicate()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "COPY PREVIOUS",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    enabled = previous != null || precedingSet != null,
                    onClick = {
                        menuExpanded = false
                        onCopy()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "REMOVE SET",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCopy()
            },
            enabled = previous != null || precedingSet != null,
            color = if (previous == null && precedingSet == null) Color.Transparent else HevyntSurfaceInteractive,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .padding(end = 6.dp)
                .semantics {
                    if (previous != null) {
                        contentDescription =
                            "Copy previous ${PreviousSetResolver.format(previous)} to set ${index + 1}"
                    }
                },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    PreviousSetResolver.format(previous),
                    color = if (previous == null) HevyntTextSecondary else HevyntTextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progression == ProgressionTrend.IMPROVED) {
                    Text(
                        text = " ↑",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        CompactNumberField(
            value = set.weight,
            allowDecimal = true,
            imeAction = ImeAction.Next,
            contentDescription = "Set ${index + 1} weight in kilograms",
            isActive = isActive,
            completed = set.completed,
            onValueChange = { onChanged(set.copy(weight = it)) },
        )
        Spacer(Modifier.width(4.dp))
        CompactNumberField(
            value = set.reps,
            imeAction = ImeAction.Done,
            contentDescription = "Set ${index + 1} repetitions",
            isActive = isActive,
            completed = set.completed,
            onValueChange = { onChanged(set.copy(reps = it)) },
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = if (set.completed) {
                        "Mark set ${index + 1} incomplete"
                    } else {
                        "Complete set ${index + 1}"
                    }
                }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCompleted()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (set.completed) {
                // Completed: solid white circle with black checkmark
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else if (isActive) {
                // Active: red outline ring
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, HevyntAccent),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {}
            } else {
                // Upcoming: subtle grey outline ring
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, HevyntOutline),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                ) {}
            }
        }
    }
}

@Composable
internal fun CompactNumberField(
    value: String,
    allowDecimal: Boolean = false,
    imeAction: ImeAction,
    contentDescription: String,
    isActive: Boolean = false,
    completed: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    BasicTextField(
        value = fieldValue,
        onValueChange = { input ->
            val sanitized = sanitizeWorkoutNumber(input.text, allowDecimal)
            val newSelection = if (sanitized == input.text) {
                input.selection
            } else {
                TextRange(sanitized.length.coerceAtMost(input.selection.end))
            }
            fieldValue = TextFieldValue(
                text = sanitized,
                selection = newSelection,
            )
            onValueChange(sanitized)
        },
        modifier = Modifier
            .width(54.dp)
            .height(42.dp)
            .padding(horizontal = 2.dp)
            .background(
                if (completed) HevyntSurfaceRaised else HevyntSurfaceInteractive,
                RoundedCornerShape(12.dp),
            )
            .border(
                if (isActive) 1.5.dp else 1.dp,
                if (isActive) HevyntAccent else if (completed) HevyntOutline.copy(alpha = 0.5f) else HevyntOutline,
                RoundedCornerShape(12.dp),
            )
            .semantics { this.contentDescription = contentDescription }
            .onFocusChanged { focusState ->
                if (focusState.isFocused && fieldValue.text.isNotEmpty()) {
                    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (completed) HevyntTextSecondary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() },
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

internal fun sanitizeWorkoutNumber(
    input: String,
    allowDecimal: Boolean,
    maxLength: Int = 6,
): String = buildString {
    input.forEach { character ->
        if (length >= maxLength) return@forEach
        when {
            character.isDigit() -> append(character)
            allowDecimal && character in ".," && '.' !in this -> append('.')
        }
    }
}
