package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetrics
import com.yifeng.commandcenter.data.formatWeight
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
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
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
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(42.dp)) {
                Icon(
                    if (isRoutineBuilder) Icons.Outlined.Close else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isRoutineBuilder) "Cancel routine" else "Minimize workout",
                )
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = workout.routineName.ifBlank { "New routine" }.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isRoutineBuilder) "ROUTINE BUILDER" else "ACTIVE WORKOUT",
                    modifier = Modifier.padding(top = 2.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            HevyntOutlineButton(
                onClick = onFinish,
                enabled = !isRoutineBuilder || workout.exercises.isNotEmpty(),
                modifier = Modifier.height(38.dp),
                accent = true,
                contentPadding = PaddingValues(horizontal = 13.dp),
            ) {
                Text(
                    text = if (isRoutineBuilder) "SAVE" else "FINISH",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Workout options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = HevyntSurfaceInteractive,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isRoutineBuilder) "DISCARD ROUTINE" else "DISCARD WORKOUT",
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
        if (!isRoutineBuilder) {
            WorkoutStatsPanel(
                startedAtMillis = workout.startedAtMillis,
                metrics = metrics,
                totalSets = workout.exercises.sumOf { it.sets.size },
            )
        }
    }
}

@Composable
internal fun WorkoutStatsPanel(
    startedAtMillis: Long,
    metrics: WorkoutMetrics,
    totalSets: Int,
) {
    Surface(
        color = HevyntSurfaceRaised,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ElapsedWorkoutMetric(startedAtMillis, Modifier.weight(1f))
            WorkoutMetric(
                label = "VOLUME",
                value = "${formatWeight(metrics.volumeKg)} KG",
                modifier = Modifier.weight(1.15f),
            )
            WorkoutMetric(
                label = "SETS",
                value = "${metrics.completedSets} / $totalSets",
                modifier = Modifier.weight(.8f),
                identityValue = true,
            )
        }
    }
}

@Composable
internal fun ElapsedWorkoutMetric(startedAtMillis: Long, modifier: Modifier = Modifier) {
    var nowMillis by remember(startedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val seconds = ((nowMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)
    WorkoutMetric("DURATION", formatElapsed(seconds), modifier, identityValue = true)
}

@Composable
internal fun WorkoutMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    identityValue: Boolean = false,
) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            color = HevyntTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 3.dp),
            color = HevyntTextPrimary,
            style = if (identityValue) {
                MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            } else {
                MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)
            },
            maxLines = 1,
        )
    }
}
