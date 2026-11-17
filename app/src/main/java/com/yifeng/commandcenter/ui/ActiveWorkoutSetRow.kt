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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
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
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

private val PreviousPillBorder = Color(0xFF2C2C30)
private val UncompletedInputText = Color(0xFFA2A1A7)

@Composable
internal fun SetHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetColumnLabel("Set", Modifier.width(26.dp), TextAlign.Start)
        SetColumnLabel("Previous · tap to fill", Modifier.weight(1f).padding(start = 6.dp))
        SetColumnLabel("kg", Modifier.width(60.dp), TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        SetColumnLabel("Reps", Modifier.width(60.dp), TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        Spacer(Modifier.width(42.dp))
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
        fontSize = 12.5.sp,
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
            .padding(vertical = 5.dp)
            .height(46.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(44.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Surface(
                onClick = { menuExpanded = true },
                color = Color.Transparent,
                shape = CircleShape,
                modifier = Modifier
                    .size(26.dp)
                    .semantics { contentDescription = "Set ${index + 1} options" },
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (isActive) HevyntAccent else HevyntTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
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

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val canCopy = previous != null || precedingSet != null
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopy()
                },
                enabled = canCopy,
                color = Color.Transparent,
                border = if (previous != null || precedingSet != null) {
                    BorderStroke(1.dp, PreviousPillBorder)
                } else {
                    BorderStroke(1.dp, PreviousPillBorder.copy(alpha = 0.4f))
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .widthIn(min = 78.dp)
                    .height(44.dp)
                    .semantics {
                        if (previous != null) {
                            contentDescription =
                                "Copy previous ${PreviousSetResolver.format(previous)} to set ${index + 1}"
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = PreviousSetResolver.format(previous),
                        color = if (previous == null) {
                            HevyntTextSecondary
                        } else if (isActive) {
                            HevyntTextPrimary
                        } else {
                            HevyntTextSecondary
                        },
                        fontSize = 14.5.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (progression == ProgressionTrend.IMPROVED) {
                        Text(
                            text = " ↑",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
        Spacer(Modifier.width(8.dp))
        CompactNumberField(
            value = set.reps,
            imeAction = ImeAction.Done,
            contentDescription = "Set ${index + 1} repetitions",
            isActive = isActive,
            completed = set.completed,
            onValueChange = { onChanged(set.copy(reps = it)) },
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
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
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
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
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, HevyntAccent),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {}
            } else {
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, HevyntOutline),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
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
    var isFocused by remember { mutableStateOf(false) }
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
            .width(60.dp)
            .height(44.dp)
            .background(
                HevyntSurfaceInteractive,
                RoundedCornerShape(14.dp),
            )
            .then(
                if (isActive) {
                    Modifier.border(
                        1.5.dp,
                        HevyntAccent,
                        RoundedCornerShape(14.dp),
                    )
                } else {
                    Modifier
                },
            )
            .semantics { this.contentDescription = contentDescription }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused && fieldValue.text.isNotEmpty()) {
                    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = when {
                completed || isFocused -> HevyntTextPrimary
                isActive -> UncompletedInputText
                else -> HevyntTextPrimary
            },
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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
