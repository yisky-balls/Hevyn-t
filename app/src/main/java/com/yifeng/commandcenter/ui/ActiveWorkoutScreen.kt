package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftBorder
import com.yifeng.commandcenter.ui.theme.LiftMuted
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    isRoutineBuilder: Boolean = false,
    onAddExercise: () -> Unit,
    onExerciseNameChanged: (String, String) -> Unit,
    onExerciseNotesChanged: (String, String) -> Unit,
    onExerciseRestChanged: (String, Int) -> Unit,
    onExerciseRemoved: (String) -> Unit,
    onSetChanged: (String, String, WorkoutSet) -> Unit,
    onSetCompletionChanged: (String, String, Boolean) -> Unit,
    onSetRemoved: (String, String) -> Unit,
    onSetAdded: (String) -> Unit,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
) {
    var restExerciseId by remember { mutableStateOf<String?>(null) }
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
    restExerciseId?.let { exerciseId ->
        val exercise = workout.exercises.firstOrNull { it.id == exerciseId }
        RestDurationDialog(
            initialSeconds = exercise?.restSeconds ?: 180,
            onDismiss = { restExerciseId = null },
            onSave = { seconds ->
                restExerciseId = null
                onExerciseRestChanged(exerciseId, seconds)
            },
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 150.dp),
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .padding(horizontal = 10.dp),
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
                            if (isRoutineBuilder) "BUILD ROUTINE" else "LOG WORKOUT",
                            fontFamily = DisplayFont,
                            fontSize = 24.sp,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            workout.routineName.ifBlank { "New routine" }.uppercase(),
                            color = LiftMuted,
                            fontSize = 11.sp,
                            fontFamily = DisplayFont,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = onFinish,
                        enabled = !isRoutineBuilder || workout.exercises.isNotEmpty(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(44.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 15.dp),
                    ) {
                        Text(
                            if (isRoutineBuilder) "SAVE" else "FINISH",
                            fontFamily = DisplayFont,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
            if (!isRoutineBuilder) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LiftPageHorizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WorkoutMetric("DURATION", formatElapsed(duration), Modifier.weight(1f))
                    DottedVerticalDivider(Modifier.height(52.dp))
                    WorkoutMetric("VOLUME", "$volume KG", Modifier.weight(1f))
                    DottedVerticalDivider(Modifier.height(52.dp))
                    WorkoutMetric("SETS", completedSets.toString(), Modifier.weight(1f))
                }
            }
            DottedDivider(Modifier.padding(horizontal = LiftPageHorizontalPadding))
        }

        if (workout.exercises.isEmpty()) {
            item {
                EmptyWorkoutState(
                    isRoutineBuilder = isRoutineBuilder,
                    onAddExercise = onAddExercise,
                    onDiscard = onCancel,
                )
            }
        }

        items(workout.exercises, key = { exercise -> exercise.id }) { exercise ->
            ExerciseLogger(
                exercise = exercise,
                onNameChanged = { onExerciseNameChanged(exercise.id, it) },
                onNotesChanged = { onExerciseNotesChanged(exercise.id, it) },
                onDelete = { onExerciseRemoved(exercise.id) },
                onEditRest = { restExerciseId = exercise.id },
                onSetChanged = { setId, updated -> onSetChanged(exercise.id, setId, updated) },
                onSetCompletionChanged = { setId, completed ->
                    onSetCompletionChanged(exercise.id, setId, completed)
                },
                onRemoveSet = { setId -> onSetRemoved(exercise.id, setId) },
                onAddSet = { onSetAdded(exercise.id) },
            )
            DottedDivider(Modifier.padding(horizontal = LiftPageHorizontalPadding, vertical = 8.dp))
        }

        if (workout.exercises.isNotEmpty()) item {
            TextButton(
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LiftPageHorizontalPadding, vertical = 12.dp)
                    .height(50.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("  ADD EXERCISE", fontFamily = DisplayFont, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun WorkoutMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            color = LiftMuted,
            fontFamily = DisplayFont,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Text(
            value,
            modifier = Modifier.padding(top = 8.dp),
            fontFamily = DisplayFont,
            fontSize = 22.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun ExerciseLogger(
    exercise: ActiveExercise,
    onNameChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDelete: () -> Unit,
    onEditRest: () -> Unit,
    onSetChanged: (String, WorkoutSet) -> Unit,
    onSetCompletionChanged: (String, Boolean) -> Unit,
    onRemoveSet: (String) -> Unit,
    onAddSet: () -> Unit,
) {
    var setPendingDelete by remember { mutableStateOf<WorkoutSet?>(null) }
    setPendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { setPendingDelete = null },
            title = {
                Text(
                    "DELETE COMPLETED SET?",
                    fontFamily = DisplayFont,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                )
            },
            text = { Text("This set will be removed from your completed sets and workout volume.") },
            confirmButton = {
                TextButton(onClick = {
                    setPendingDelete = null
                    onRemoveSet(set.id)
                }) {
                    Text(
                        "DELETE",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DisplayFont,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) {
                    Text("CANCEL", fontFamily = DisplayFont)
                }
            },
        )
    }
    Column(
        Modifier
            .animateContentSize(animationSpec = tween(220))
            .padding(horizontal = LiftPageHorizontalPadding, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IndustrialDumbbell(Modifier.size(width = 40.dp, height = 24.dp))
            BasicTextField(
                value = exercise.name.uppercase(),
                onValueChange = onNameChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove exercise",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        BasicTextField(
            value = exercise.notes,
            onValueChange = onNotesChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LiftMuted),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(Modifier.padding(vertical = 8.dp)) {
                    if (exercise.notes.isEmpty()) Text("Add notes…", color = LiftMuted)
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditRest)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = LiftMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "REST   /   %02d:%02d".format(exercise.restSeconds / 60, exercise.restSeconds % 60),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DisplayFont,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            )
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit rest timer",
                tint = LiftMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        DottedDivider()
        SetHeader()
        exercise.sets.forEachIndexed { index, set ->
            key(set.id) {
                AnimatedVisibility(
                    visible = true,
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        SetRow(
                            index = index,
                            set = set,
                            onChanged = { updatedSet -> onSetChanged(set.id, updatedSet) },
                            onCompleted = {
                                val becomingComplete = !set.completed
                                onSetCompletionChanged(set.id, becomingComplete)
                            },
                            onDelete = {
                                if (set.completed) setPendingDelete = set else onRemoveSet(set.id)
                            },
                        )
                        DottedDivider()
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onAddSet,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(164.dp)
                .padding(top = 12.dp)
                .height(48.dp),
            border = BorderStroke(1.dp, LiftBorder),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(19.dp))
            Text("  ADD SET", fontFamily = DisplayFont, fontSize = 12.sp, letterSpacing = 1.sp)
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
            .height(430.dp)
            .padding(horizontal = LiftPageHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = LiftMuted,
        )
        Text(
            if (isRoutineBuilder) "Build your routine" else "Get started",
            modifier = Modifier.padding(top = 22.dp),
            fontFamily = DisplayFont,
            fontSize = 24.sp,
            letterSpacing = 1.sp,
        )
        Text(
            "Add an exercise to ${if (isRoutineBuilder) "build your routine" else "start your workout"}",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            color = LiftMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAddExercise,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("  ADD EXERCISE", fontFamily = DisplayFont, fontSize = 13.sp, letterSpacing = 1.sp)
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
            .padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("SET", modifier = Modifier.width(42.dp), color = LiftMuted, fontFamily = DisplayFont, fontSize = 11.sp)
        Text("PREVIOUS", modifier = Modifier.weight(1f), color = LiftMuted, fontFamily = DisplayFont, fontSize = 11.sp)
        Text("KG", modifier = Modifier.width(58.dp), color = LiftMuted, fontFamily = DisplayFont, fontSize = 11.sp)
        Text("REPS", modifier = Modifier.width(58.dp), color = LiftMuted, fontFamily = DisplayFont, fontSize = 11.sp)
        Spacer(Modifier.width(38.dp))
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
            .height(58.dp)
            .clip(RectangleShape),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
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
                .height(58.dp)
                .background(MaterialTheme.colorScheme.background)
                .animateContentSize()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Box(
            Modifier
                .width(3.dp)
                .height(46.dp)
                .background(if (set.completed) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        Text(
            (index + 1).toString(),
            modifier = Modifier.width(39.dp),
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
            fontFamily = DisplayFont,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${set.weight.ifBlank { "—" }}kg × ${set.reps.ifBlank { "—" }}",
            modifier = Modifier.weight(1f),
            color = LiftMuted,
            fontFamily = DisplayFont,
            fontSize = 13.sp,
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
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                if (set.completed) MaterialTheme.colorScheme.primary else LiftBorder,
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (set.completed) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Complete set",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
        title = {
            Text(
                "REST TIMER",
                fontFamily = DisplayFont,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(seconds / 60, seconds % 60),
                    fontFamily = DisplayFont,
                    fontSize = 36.sp,
                    letterSpacing = 1.5.sp,
                )
                Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = { seconds = (seconds - 15).coerceAtLeast(15) }) {
                        Text("−15 SEC", fontFamily = DisplayFont, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { seconds = (seconds + 15).coerceAtMost(900) }) {
                        Text("+15 SEC", fontFamily = DisplayFont, fontSize = 12.sp)
                    }
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
        confirmButton = {
            TextButton(onClick = { onSave(seconds) }) {
                Text("SAVE", fontFamily = DisplayFont, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = DisplayFont) }
        },
    )
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        modifier = Modifier
            .width(58.dp)
            .height(46.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            fontFamily = DisplayFont,
            fontSize = 17.sp,
        ),
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