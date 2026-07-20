package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.ui.theme.LiftGreen
import com.yifeng.commandcenter.ui.theme.LiftMuted
import kotlinx.coroutines.delay

@Composable
fun MinimizedWorkoutBar(
    workout: ActiveWorkout,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.startedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((nowMillis - workout.startedAtMillis) / 1_000L).coerceAtLeast(0L)
    val currentExercise = workout.exercises.firstOrNull { exercise ->
        exercise.sets.any { !it.completed }
    }?.name ?: workout.exercises.lastOrNull()?.name ?: workout.routineName

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard workout?") },
            text = { Text("Your active workout and its completed sets will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscard()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep workout") }
            },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onResume) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Resume workout")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .background(LiftGreen, CircleShape)
                    )
                    Text(
                        "Workout",
                        modifier = Modifier.padding(start = 9.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        formatWorkoutElapsed(elapsedSeconds),
                        modifier = Modifier.padding(start = 6.dp),
                        fontSize = 17.sp,
                    )
                }
                Text(
                    currentExercise.ifBlank { "Active workout" },
                    color = LiftMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { showDiscardDialog = true }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Discard workout",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatWorkoutElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}