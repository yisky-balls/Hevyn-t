package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.WorkoutMetrics
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.history.ExerciseAutocomplete
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    previousSets: Map<String, PreviousSetPerformance> = emptyMap(),
    exerciseSuggestions: List<ExerciseSuggestion> = emptyList(),
    isRoutineBuilder: Boolean = false,
    onRequestExerciseSuggestions: () -> Unit,
    onAddExercise: (ExerciseSuggestion) -> Unit,
    onExerciseNameChanged: (String, String) -> Unit,
    onExerciseNotesChanged: (String, String) -> Unit,
    onExerciseRestChanged: (String, Int) -> Unit,
    onExerciseRemoved: (String) -> Unit,
    onExerciseMoved: (String, Int) -> Unit,
    onSetChanged: (String, String, WorkoutSet) -> Unit,
    onSetCompletionChanged: (String, String, Boolean) -> Unit,
    onSetRemoved: (String, String) -> Unit,
    onSetAdded: (String) -> Unit,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
) {
    var restExerciseId by remember { mutableStateOf<String?>(null) }
    var isAddingExercise by rememberSaveable { mutableStateOf(false) }
    val metrics = remember(workout) { WorkoutMetricsCalculator.calculate(workout) }

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
        contentPadding = PaddingValues(bottom = 148.dp),
    ) {
        item(key = "active-workout-header") {
            ActiveWorkoutHeader(
                workout = workout,
                metrics = metrics,
                isRoutineBuilder = isRoutineBuilder,
                onNavigateBack = if (isRoutineBuilder) onCancel else onMinimize,
                onFinish = onFinish,
                onDiscard = onCancel,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (workout.exercises.isEmpty() && !isAddingExercise) {
            item(key = "empty-workout") {
                EmptyWorkoutState(
                    isRoutineBuilder = isRoutineBuilder,
                    onAddExercise = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    onDiscard = onCancel,
                )
            }
        }

        itemsIndexed(workout.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
            ExerciseLogger(
                index = index,
                exercise = exercise,
                previousSets = previousSets,
                defaultExpanded = index == 0,
                onNameChanged = { onExerciseNameChanged(exercise.id, it) },
                onNotesChanged = { onExerciseNotesChanged(exercise.id, it) },
                onDelete = { onExerciseRemoved(exercise.id) },
                canMoveUp = index > 0,
                canMoveDown = index < workout.exercises.lastIndex,
                onMoveUp = { onExerciseMoved(exercise.id, -1) },
                onMoveDown = { onExerciseMoved(exercise.id, 1) },
                onEditRest = { restExerciseId = exercise.id },
                onSetChanged = { setId, updated -> onSetChanged(exercise.id, setId, updated) },
                onSetCompletionChanged = { setId, completed ->
                    onSetCompletionChanged(exercise.id, setId, completed)
                },
                onRemoveSet = { setId -> onSetRemoved(exercise.id, setId) },
                onAddSet = { onSetAdded(exercise.id) },
            )
            Spacer(Modifier.height(6.dp))
        }

        if (isAddingExercise) {
            item(key = "exercise-search") {
                ExerciseAutocompletePanel(
                    suggestions = exerciseSuggestions,
                    onSelect = { suggestion ->
                        onAddExercise(suggestion)
                        isAddingExercise = false
                    },
                    onDismiss = { isAddingExercise = false },
                )
            }
        } else if (workout.exercises.isNotEmpty()) {
            item(key = "add-exercise") {
                TextButton(
                    onClick = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(42.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "  ADD EXERCISE",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutHeader(
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
            title = { Text(if (isRoutineBuilder) "DISCARD ROUTINE?" else "DISCARD WORKOUT?", style = MaterialTheme.typography.titleLarge) },
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
private fun WorkoutStatsPanel(
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
private fun ElapsedWorkoutMetric(startedAtMillis: Long, modifier: Modifier = Modifier) {
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
private fun WorkoutMetric(
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

@Composable
private fun ExerciseAutocompletePanel(
    suggestions: List<ExerciseSuggestion>,
    onSelect: (ExerciseSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val matches = remember(suggestions, query) {
        ExerciseAutocomplete.search(suggestions, query)
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(HevyntSurfaceRaised, MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            text = "ADD EXERCISE",
            color = HevyntTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp)
                .border(1.dp, HevyntOutline, MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = HevyntTextSecondary,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        matches.firstOrNull()?.let(onSelect)
                            ?: query.takeIf(String::isNotBlank)
                                ?.let(ExerciseAutocomplete::custom)
                                ?.let(onSelect)
                    },
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "SEARCH EXERCISES…",
                                color = HevyntTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Close exercise search")
            }
        }

        matches.forEach { suggestion ->
            HevyntDottedRule(Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(suggestion) }
                    .height(58.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = suggestion.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    suggestion.lastPerformance?.let { previous ->
                        Text(
                            "LAST: ${PreviousSetResolver.format(previous)}",
                            modifier = Modifier.padding(top = 2.dp),
                            color = HevyntTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = "+",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp),
                )
            }
        }

        if (
            query.isNotBlank() &&
            !ExerciseAutocomplete.hasExactName(suggestions, query)
        ) {
            HevyntDottedRule()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(ExerciseAutocomplete.custom(query)) }
                    .height(50.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "+  ${query.trim()}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExerciseLogger(
    index: Int,
    exercise: ActiveExercise,
    previousSets: Map<String, PreviousSetPerformance>,
    defaultExpanded: Boolean,
    onNameChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditRest: () -> Unit,
    onSetChanged: (String, WorkoutSet) -> Unit,
    onSetCompletionChanged: (String, Boolean) -> Unit,
    onRemoveSet: (String) -> Unit,
    onAddSet: () -> Unit,
) {
    var expanded by rememberSaveable(exercise.id) { mutableStateOf(defaultExpanded) }
    var notesExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmExerciseDelete by remember { mutableStateOf(false) }
    var setPendingDelete by remember { mutableStateOf<WorkoutSet?>(null) }

    if (confirmExerciseDelete) {
        HevyntDialog(
            onDismissRequest = { confirmExerciseDelete = false },
            title = {
                Text(
                    text = "REMOVE EXERCISE?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text("This removes ${exercise.name.ifBlank { "the exercise" }} and all of its sets.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExerciseDelete = false
                    onDelete()
                }) {
                    Text("REMOVE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExerciseDelete = false }) {
                    Text("CANCEL", fontWeight = FontWeight.Medium)
                }
            },
        )
    }
    setPendingDelete?.let { set ->
        HevyntDialog(
            onDismissRequest = { setPendingDelete = null },
            title = {
                Text(
                    text = "DELETE COMPLETED SET?",
                    style = MaterialTheme.typography.titleLarge,
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
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { setPendingDelete = null }) {
                    Text("CANCEL", fontWeight = FontWeight.Medium)
                }
            },
        )
    }
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .background(HevyntSurfaceRaised, MaterialTheme.shapes.medium)
            .border(1.dp, HevyntOutline, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = HevyntSurfaceInteractive,
                shape = CircleShape,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (expanded) {
                BasicTextField(
                    value = exercise.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp, top = 10.dp, bottom = 10.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp, end = 4.dp),
                ) {
                    Text(
                        text = exercise.name.ifBlank { "Untitled exercise" },
                        color = HevyntTextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    collapsedPreviousSummary(exercise, previousSets)?.let { summary ->
                        Text(
                            text = "Last: $summary",
                            modifier = Modifier.padding(top = 2.dp),
                            color = HevyntTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse exercise" else "Expand exercise",
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Exercise options",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = HevyntSurfaceInteractive,
                ) {
                    DropdownMenuItem(
                        text = { Text("MOVE UP", style = MaterialTheme.typography.labelMedium) },
                        enabled = canMoveUp,
                        onClick = { menuExpanded = false; onMoveUp() },
                        leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("MOVE DOWN", style = MaterialTheme.typography.labelMedium) },
                        enabled = canMoveDown,
                        onClick = { menuExpanded = false; onMoveDown() },
                        leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("EDIT NAME", style = MaterialTheme.typography.labelMedium) },
                        onClick = { menuExpanded = false; expanded = true },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("REMOVE EXERCISE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) },
                        onClick = { menuExpanded = false; confirmExerciseDelete = true },
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
        }
        if (!expanded) return@Column

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            UtilityChip(
                label = "NOTES",
                active = exercise.notes.isNotBlank(),
                icon = Icons.Outlined.Edit,
                contentDescription = if (notesExpanded) "Close exercise notes" else "Open exercise notes",
                onClick = { notesExpanded = !notesExpanded },
            )
            UtilityChip(
                label = "REST %02d:%02d".format(exercise.restSeconds / 60, exercise.restSeconds % 60),
                icon = Icons.Outlined.Refresh,
                contentDescription = "Edit rest timer",
                onClick = onEditRest,
            )
        }

        if (notesExpanded) {
            NoteEditor(
                value = exercise.notes,
                onValueChange = onNotesChanged,
                onDone = { notesExpanded = false },
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
        SetHeader()
        exercise.sets.forEachIndexed { index, set ->
            key(set.id) {
                SetRow(
                    index = index,
                    set = set,
                    previous = previousSets[set.id],
                    onChanged = { updatedSet -> onSetChanged(set.id, updatedSet) },
                    onCompleted = {
                        val becomingComplete = !set.completed
                        onSetCompletionChanged(set.id, becomingComplete)
                    },
                    onDelete = {
                        if (set.completed) setPendingDelete = set else onRemoveSet(set.id)
                    },
                )
                if (index != exercise.sets.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                }
            }
        }
        TextButton(
            onClick = onAddSet,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = "ADD SET",
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun collapsedPreviousSummary(
    exercise: ActiveExercise,
    previousSets: Map<String, PreviousSetPerformance>,
): String? {
    val values = exercise.sets.mapNotNull { set ->
        previousSets[set.id]?.let(PreviousSetResolver::format)
    }
    return values.takeIf { it.isNotEmpty() }?.joinToString()
}

@Composable
private fun UtilityChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        color = if (active) HevyntAccentContainer else HevyntSurfaceInteractive,
        border = BorderStroke(1.dp, if (active) HevyntAccent.copy(alpha = 0.5f) else HevyntOutline),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .height(32.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) HevyntAccent else HevyntTextSecondary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 5.dp),
                color = if (active) HevyntAccent else HevyntTextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun NoteEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp, max = 74.dp)
            .padding(bottom = 6.dp)
            .background(HevyntSurfaceInteractive, MaterialTheme.shapes.small)
            .focusRequester(focusRequester)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = HevyntTextPrimary,
            fontSize = 13.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        "ADD A SHORT NOTE…",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                innerTextField()
            }
        },
    )
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
            .padding(horizontal = HevyntPageHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Build,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = HevyntTextSecondary,
        )
        Text(
            text = if (isRoutineBuilder) "Build your routine" else "Get started",
            modifier = Modifier.padding(top = 22.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Add an exercise to ${if (isRoutineBuilder) "build your routine" else "start your workout"}",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            color = HevyntTextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        HevyntPrimaryButton(
            onClick = onAddExercise,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(
                text = "  ADD EXERCISE",
                style = MaterialTheme.typography.labelLarge,
            )
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
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetColumnLabel("SET", Modifier.width(34.dp), TextAlign.Center)
        SetColumnLabel("PREVIOUS", Modifier.weight(1f).padding(start = 4.dp))
        SetColumnLabel("KG", Modifier.width(54.dp), TextAlign.Center)
        SetColumnLabel("REPS", Modifier.width(54.dp), TextAlign.Center)
        SetColumnLabel("DONE", Modifier.width(48.dp), TextAlign.Center)
    }
}

@Composable
private fun SetColumnLabel(
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
private fun SetRow(
    index: Int,
    set: WorkoutSet,
    previous: PreviousSetPerformance?,
    onChanged: (WorkoutSet) -> Unit,
    onCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
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
                        color = if (set.completed) HevyntTextSecondary else HevyntTextPrimary,
                        style = MaterialTheme.typography.labelMedium,
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
                previous?.let {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChanged(ActiveWorkoutOperations.copyPreviousSet(set, it))
                }
            },
            enabled = previous != null,
            color = if (previous == null) Color.Transparent else HevyntSurfaceInteractive,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .padding(end = 3.dp)
                .semantics {
                    if (previous != null) {
                        contentDescription =
                            "Copy previous ${PreviousSetResolver.format(previous)} to set ${index + 1}"
                    }
                },
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    PreviousSetResolver.format(previous),
                    color = if (previous == null) HevyntTextSecondary else HevyntTextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CompactNumberField(
            value = set.weight,
            allowDecimal = true,
            imeAction = ImeAction.Next,
            contentDescription = "Set ${index + 1} weight in kilograms",
            completed = set.completed,
            onValueChange = { onChanged(set.copy(weight = it)) },
        )
        CompactNumberField(
            value = set.reps,
            imeAction = ImeAction.Done,
            contentDescription = "Set ${index + 1} repetitions",
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
            Surface(
                color = if (set.completed) HevyntAccent else Color.Transparent,
                border = BorderStroke(
                    if (set.completed) 0.dp else 1.5.dp,
                    if (set.completed) Color.Transparent else HevyntOutline,
                ),
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (set.completed) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
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
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "REST TIMER",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(seconds / 60, seconds % 60),
                    fontFamily = HevyntDisplayFont,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                    ),
                )
                Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HevyntOutlineButton(onClick = { seconds = (seconds - 15).coerceAtLeast(15) }) {
                        Text("−15 SEC", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    HevyntOutlineButton(onClick = { seconds = (seconds + 15).coerceAtMost(900) }) {
                        Text("+15 SEC", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                Text("SAVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Medium) }
        },
    )
}

@Composable
private fun CompactNumberField(
    value: String,
    allowDecimal: Boolean = false,
    imeAction: ImeAction,
    contentDescription: String,
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
                MaterialTheme.shapes.small,
            )
            .border(
                1.dp,
                if (completed) HevyntOutline.copy(alpha = 0.5f) else HevyntOutline,
                MaterialTheme.shapes.small,
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

private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}