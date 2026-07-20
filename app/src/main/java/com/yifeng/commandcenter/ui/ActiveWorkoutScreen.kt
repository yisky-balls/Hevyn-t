package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.NextSetTarget
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.ui.theme.LiftGreen
import com.yifeng.commandcenter.ui.theme.LiftMuted
import com.yifeng.commandcenter.ui.theme.LiftSurfaceRaised
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    isRoutineBuilder: Boolean = false,
    onWorkoutChanged: (ActiveWorkout) -> Unit,
    onSetRemoved: (String, String) -> Unit,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
    onStartRest: (String, NextSetTarget?) -> Unit,
) {
    var showRestDialog by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.startedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val duration = ((nowMillis - workout.startedAtMillis) / 1_000L).coerceAtLeast(0L)
    val completedSets = workout.exercises.sumOf { exercise ->
        exercise.sets.count { it.completed }
    }
    val volume = workout.exercises.sumOf { exercise ->
        exercise.sets.filter { it.completed }.sumOf { set ->
            (set.weight.toIntOrNull() ?: 0) * (set.reps.toIntOrNull() ?: 0)
        }
    }
    if (showRestDialog) {
        RestDurationDialog(
            initialSeconds = workout.restSeconds,
            onDismiss = { showRestDialog = false },
            onSave = { seconds ->
                showRestDialog = false
                onWorkoutChanged(ActiveWorkoutOperations.updateRestSeconds(workout, seconds))
            },
        )
    }
    fun addExercise() {
        onWorkoutChanged(
            workout.copy(exercises = workout.exercises + ActiveExercise(name = "New exercise"))
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 170.dp),
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = if (isRoutineBuilder) onCancel else onMinimize) {
                        Icon(
                            if (isRoutineBuilder) Icons.Outlined.Close else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isRoutineBuilder) "Cancel routine" else "Minimize workout",
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isRoutineBuilder) "Build Routine" else "Log Workout",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            workout.routineName.ifBlank { "New routine" },
                            color = LiftMuted,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(Icons.Outlined.Timer, contentDescription = null)
                    Button(
                        onClick = onFinish,
                        enabled = !isRoutineBuilder || workout.exercises.isNotEmpty(),
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!isRoutineBuilder) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WorkoutMetric("Duration", formatElapsed(duration), MaterialTheme.colorScheme.primary)
                    WorkoutMetric("Volume", "$volume kg", Color.White)
                    WorkoutMetric("Sets", completedSets.toString(), Color.White)
                }
            }
            HorizontalDivider(color = LiftSurfaceRaised)
        }

        if (workout.exercises.isEmpty()) {
            item {
                EmptyWorkoutState(
                    isRoutineBuilder = isRoutineBuilder,
                    onAddExercise = ::addExercise,
                    onDiscard = onCancel,
                )
            }
        }

        itemsIndexed(workout.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
            ExerciseLogger(
                exercise = exercise,
                restSeconds = workout.restSeconds,
                enableRestTimer = !isRoutineBuilder,
                onExerciseChanged = { updatedExercise ->
                    onWorkoutChanged(
                        workout.copy(
                            exercises = workout.exercises.mapIndexed { currentIndex, current ->
                                if (currentIndex == index) updatedExercise else current
                            }
                        )
                    )
                },
                onDelete = {
                    onWorkoutChanged(
                        workout.copy(
                            exercises = workout.exercises.filterIndexed { currentIndex, _ ->
                                currentIndex != index
                            }
                        )
                    )
                },
                onEditRest = { showRestDialog = true },
                onRemoveSet = { setId -> onSetRemoved(exercise.id, setId) },
                onStartRest = { setIndex ->
                    onStartRest(
                        exercise.name,
                        WorkoutProgress.nextSetAfter(workout, index, setIndex),
                    )
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            )
        }

        if (workout.exercises.isNotEmpty()) item {
            TextButton(
                onClick = ::addExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(58.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("  Add exercise", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun WorkoutMetric(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = LiftMuted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 21.sp)
    }
}

@Composable
private fun ExerciseLogger(
    exercise: ActiveExercise,
    restSeconds: Int,
    enableRestTimer: Boolean,
    onExerciseChanged: (ActiveExercise) -> Unit,
    onDelete: () -> Unit,
    onEditRest: () -> Unit,
    onRemoveSet: (String) -> Unit,
    onStartRest: (Int) -> Unit,
) {
    var setPendingDelete by remember { mutableStateOf<WorkoutSet?>(null) }
    setPendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { setPendingDelete = null },
            title = { Text("Delete completed set?") },
            text = { Text("This set will be removed from your completed sets and workout volume.") },
            confirmButton = {
                TextButton(onClick = {
                    setPendingDelete = null
                    onRemoveSet(set.id)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
    Column(
        Modifier
            .animateContentSize(animationSpec = tween(220))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            OutlinedTextField(
                value = exercise.name,
                onValueChange = { onExerciseChanged(exercise.copy(name = it)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove exercise",
                    tint = LiftMuted,
                )
            }
        }
        OutlinedTextField(
            value = exercise.notes,
            onValueChange = { onExerciseChanged(exercise.copy(notes = it)) },
            placeholder = { Text("Add notes here…", color = LiftMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditRest)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Rest Timer: ${restSeconds / 60}min ${restSeconds % 60}s",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit rest timer",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        SetHeader()
        exercise.sets.forEachIndexed { index, set ->
            key(set.id) {
                AnimatedVisibility(
                    visible = true,
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SetRow(
                        index = index,
                        set = set,
                        onChanged = { updatedSet ->
                            onExerciseChanged(
                                exercise.copy(
                                    sets = exercise.sets.mapIndexed { setIndex, current ->
                                        if (setIndex == index) updatedSet else current
                                    }
                                )
                            )
                        },
                        onCompleted = {
                            val becomingComplete = !set.completed
                            onExerciseChanged(
                                exercise.copy(
                                    sets = exercise.sets.mapIndexed { setIndex, current ->
                                        if (setIndex == index) current.copy(completed = becomingComplete)
                                        else current
                                    }
                                )
                            )
                            if (becomingComplete && enableRestTimer) onStartRest(index)
                        },
                        onDelete = {
                            if (set.completed) setPendingDelete = set else onRemoveSet(set.id)
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                val previous = exercise.sets.lastOrNull()
                onExerciseChanged(
                    exercise.copy(
                        sets = exercise.sets + WorkoutSet(
                            weight = previous?.weight.orEmpty(),
                            reps = previous?.reps.orEmpty(),
                        )
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("  Add Set")
        }
    }
}

@Composable
private fun EmptyWorkoutState(
    isRoutineBuilder: Boolean,
    onAddExercise: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = LiftMuted,
        )
        Text(
            if (isRoutineBuilder) "Build your routine" else "Get started",
            modifier = Modifier.padding(top = 28.dp),
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Add an exercise to ${if (isRoutineBuilder) "build your routine" else "start your workout"}",
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp),
            color = LiftMuted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAddExercise,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("  Add Exercise", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onDiscard,
            modifier = Modifier.padding(top = 14.dp),
        ) {
            Text(
                if (isRoutineBuilder) "Discard routine" else "Discard workout",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SetHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("SET", modifier = Modifier.width(42.dp), color = LiftMuted, fontSize = 12.sp)
        Text("PREVIOUS", modifier = Modifier.weight(1f), color = LiftMuted, fontSize = 12.sp)
        Text("KG", modifier = Modifier.width(64.dp), color = LiftMuted, fontSize = 12.sp)
        Text("REPS", modifier = Modifier.width(64.dp), color = LiftMuted, fontSize = 12.sp)
        Spacer(Modifier.width(42.dp))
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: WorkoutSet,
    onChanged: (WorkoutSet) -> Unit,
    onCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val rowBackground = if (set.completed) {
        LiftGreen.copy(alpha = 0.75f).compositeOver(MaterialTheme.colorScheme.background)
    } else {
        MaterialTheme.colorScheme.background
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RectangleShape),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(end = 18.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Text(" Delete", fontWeight = FontWeight.Bold)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(rowBackground)
                .animateContentSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Box(modifier = Modifier.width(42.dp)) {
            TextButton(
                onClick = { menuExpanded = true },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    (index + 1).toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(Icons.Outlined.MoreVert, contentDescription = "Set options", modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Delete set") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
        Text(
            "${set.weight.ifBlank { "—" }}kg × ${set.reps.ifBlank { "—" }}",
            modifier = Modifier.weight(1f),
            color = if (set.completed) Color.White else LiftMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CompactNumberField(
            value = set.weight,
            onValueChange = { onChanged(set.copy(weight = it)) },
        )
        CompactNumberField(
            value = set.reps,
            onValueChange = { onChanged(set.copy(reps = it)) },
        )
        Surface(
            onClick = onCompleted,
            color = if (set.completed) MaterialTheme.colorScheme.primary else LiftMuted,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, contentDescription = "Complete set")
            }
        }
        }
    }
}

@Composable
private fun RestDurationDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var seconds by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    val presets = listOf(60, 90, 120, 180, 300)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest timer") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(seconds / 60, seconds % 60),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = { seconds = (seconds - 15).coerceAtLeast(15) }) { Text("−15s") }
                    Button(onClick = { seconds = (seconds + 15).coerceAtMost(900) }) { Text("+15s") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        TextButton(
                            onClick = { seconds = preset },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            Text("${preset / 60}:${(preset % 60).toString().padStart(2, '0')}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(seconds) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        modifier = Modifier
            .width(64.dp)
            .height(50.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}