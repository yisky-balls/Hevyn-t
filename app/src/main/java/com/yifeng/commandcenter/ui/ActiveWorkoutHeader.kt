package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetrics
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import kotlinx.coroutines.delay

@Composable
internal fun ActiveWorkoutHeader(
    workout: ActiveWorkout,
    metrics: WorkoutMetrics,
    isRoutineBuilder: Boolean,
    isGlyphSupported: Boolean = false,
    isGlyphTimerEnabled: Boolean = true,
    isGlyphPulseEnabled: Boolean = true,
    onToggleGlyphTimer: (Boolean) -> Unit = {},
    onToggleGlyphPulse: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showGlyphSettings by remember { mutableStateOf(false) }

    if (showGlyphSettings) {
        GlyphSettingsDialog(
            isTimerEnabled = isGlyphTimerEnabled,
            isPulseEnabled = isGlyphPulseEnabled,
            onToggleTimer = onToggleGlyphTimer,
            onTogglePulse = onToggleGlyphPulse,
            onDismiss = { showGlyphSettings = false },
        )
    }

    if (confirmDiscard) {
        HevyntDialog(
            onDismissRequest = { confirmDiscard = false },
            title = {
                Text(
                    if (isRoutineBuilder) "DISCARD ROUTINE?" else "DISCARD WORKOUT?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text("This removes the active session from this device. Completed workout history is not affected.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) {
                    Text("DISCARD", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelMedium)
                }
            },
        )
    }

    val currentExerciseIndex = remember(workout) {
        val idx = workout.exercises.indexOfFirst { ex -> ex.sets.any { !it.completed } }
        if (idx >= 0) idx else (workout.exercises.size - 1).coerceAtLeast(0)
    }
    val totalExercises = workout.exercises.size
    val totalSets = workout.exercises.sumOf { it.sets.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = HevyntTextPrimary,
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = workout.routineName.ifBlank { "Workout" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = HevyntTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1f))

            // Finish Pill Button
            Surface(
                shape = RoundedCornerShape(50),
                color = HevyntSurfaceRaised,
                border = BorderStroke(1.dp, HevyntOutline),
                modifier = Modifier.clickable(onClick = onFinish),
            ) {
                Text(
                    text = if (isRoutineBuilder) "Save" else "Finish",
                    color = HevyntTextPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                )
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = HevyntTextSecondary,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = HevyntSurfaceInteractive,
                ) {
                    if (isGlyphSupported) {
                        DropdownMenuItem(
                            text = { Text("GLYPH SETTINGS", style = MaterialTheme.typography.labelMedium) },
                            onClick = {
                                menuExpanded = false
                                showGlyphSettings = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isRoutineBuilder) "DISCARD ROUTINE" else "DISCARD WORKOUT",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        onClick = { menuExpanded = false; confirmDiscard = true },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hero Timer & Exercise Progress
        if (!isRoutineBuilder) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Large Dot-Matrix Chronometer
                ElapsedChronometer(
                    startedAtMillis = workout.startedAtMillis,
                )

                Spacer(Modifier.weight(1f))

                // Stacked Subtitle Stats
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Exercise ${if (totalExercises > 0) currentExerciseIndex + 1 else 0} of $totalExercises",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↑ ",
                            color = HevyntAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${formatWeight(metrics.volumeKg)} kg · ${metrics.completedSets}/$totalSets sets",
                            color = HevyntTextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ElapsedChronometer(startedAtMillis: Long) {
    var nowMillis by remember(startedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((nowMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeText = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    Text(
        text = timeText,
        fontFamily = HevyntDisplayFont,
        fontSize = 40.sp,
        fontWeight = FontWeight.Normal,
        color = HevyntTextPrimary,
        letterSpacing = 2.sp,
    )
}
