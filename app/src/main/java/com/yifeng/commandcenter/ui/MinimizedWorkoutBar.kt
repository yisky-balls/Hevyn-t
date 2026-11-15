package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
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
        HevyntDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    "DISCARD WORKOUT?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text("Your active workout and its completed sets will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscard()
                }) {
                    Text(
                        "DISCARD",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("KEEP WORKOUT", fontWeight = FontWeight.Medium)
                }
            },
        )
    }

    Surface(
        color = HevyntSurfaceRaised,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.yifeng.commandcenter.ui.theme.HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onResume)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Resume workout",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(Modifier.padding(end = 6.dp))
                    Text(
                        currentExercise.ifBlank { "Active workout" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatWorkoutElapsed(elapsedSeconds),
                    modifier = Modifier.padding(top = 2.dp),
                    fontFamily = HevyntDisplayFont,
                    fontSize = 15.sp,
                    color = HevyntTextSecondary,
                    letterSpacing = 0.5.sp,
                )
            }
            IconButton(onClick = { showDiscardDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Discard workout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatWorkoutElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}