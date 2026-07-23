package com.yifeng.commandcenter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftDivider
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
            title = {
                Text(
                    "DISCARD WORKOUT?",
                    fontFamily = DisplayFont,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
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
                        fontFamily = DisplayFont,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("KEEP WORKOUT", fontFamily = DisplayFont)
                }
            },
        )
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            DottedDivider(color = LiftDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable(onClick = onResume)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onResume) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Resume workout",
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SignalDot(Modifier.size(5.dp))
                        Text(
                            "WORKOUT",
                            modifier = Modifier.padding(start = 9.dp),
                            fontFamily = DisplayFont,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Text(
                        formatWorkoutElapsed(elapsedSeconds),
                        modifier = Modifier.padding(top = 5.dp),
                        fontFamily = DisplayFont,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        currentExercise.ifBlank { "Active workout" },
                        modifier = Modifier.padding(top = 2.dp),
                        color = LiftMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { showDiscardDialog = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Discard workout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(23.dp),
                    )
                }
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