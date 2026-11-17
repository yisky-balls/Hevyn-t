package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.ExerciseAutocomplete
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun ExerciseLogger(
    exercise: ActiveExercise,
    previousSets: Map<String, PreviousSetPerformance>,
    exerciseHistory: ExerciseHistorySummary? = null,
    isRoutineBuilder: Boolean = false,
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
    onDuplicateSet: (String) -> Unit,
    onRequestAddExercise: () -> Unit = {},
    onDiscardWorkout: () -> Unit = {},
    isGlyphSupported: Boolean = false,
    isGlyphTimerEnabled: Boolean = true,
    isGlyphPulseEnabled: Boolean = true,
    onToggleGlyphTimer: (Boolean) -> Unit = {},
    onToggleGlyphPulse: (Boolean) -> Unit = {},
) {
    var notesExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var isEditingTitle by rememberSaveable(exercise.id) { mutableStateOf(isRoutineBuilder) }
    var selectedSetId by rememberSaveable(exercise.id) { mutableStateOf<String?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var confirmExerciseDelete by remember { mutableStateOf(false) }
    var confirmDiscardSession by remember { mutableStateOf(false) }
    var setPendingDelete by remember { mutableStateOf<WorkoutSet?>(null) }

    if (showDetailsDialog) {
        ExerciseDetailsAndOptionsDialog(
            exerciseName = exercise.name,
            exerciseHistory = exerciseHistory,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            isRoutineBuilder = isRoutineBuilder,
            isGlyphSupported = isGlyphSupported,
            isGlyphTimerEnabled = isGlyphTimerEnabled,
            isGlyphPulseEnabled = isGlyphPulseEnabled,
            onToggleGlyphTimer = onToggleGlyphTimer,
            onToggleGlyphPulse = onToggleGlyphPulse,
            onRename = {
                showDetailsDialog = false
                isEditingTitle = true
            },
            onMoveUp = {
                showDetailsDialog = false
                onMoveUp()
            },
            onMoveDown = {
                showDetailsDialog = false
                onMoveDown()
            },
            onAddExercise = {
                showDetailsDialog = false
                onRequestAddExercise()
            },
            onRemoveExercise = {
                showDetailsDialog = false
                confirmExerciseDelete = true
            },
            onDiscardWorkout = {
                showDetailsDialog = false
                confirmDiscardSession = true
            },
            onDismiss = { showDetailsDialog = false },
        )
    }

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

    if (confirmDiscardSession) {
        HevyntDialog(
            onDismissRequest = { confirmDiscardSession = false },
            title = {
                Text(
                    text = if (isRoutineBuilder) "DISCARD ROUTINE?" else "DISCARD WORKOUT?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text("This removes the active session from this device. Completed workout history is not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscardSession = false
                    onDiscardWorkout()
                }) {
                    Text("DISCARD", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscardSession = false }) {
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

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            // Exercise Title & ↗ History/Options Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditingTitle) {
                    BasicTextField(
                        value = exercise.name,
                        onValueChange = onNameChanged,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = HevyntTextPrimary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (!isRoutineBuilder) isEditingTitle = false },
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    )
                } else {
                    Text(
                        text = exercise.name.ifBlank { "Untitled exercise" },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = HevyntTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showDetailsDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_diagonal),
                        contentDescription = "Exercise options",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Notes & Rest 4:00 pills
            val restMin = exercise.restSeconds / 60
            val restSec = exercise.restSeconds % 60
            val restLabel = "Rest $restMin:${restSec.toString().padStart(2, '0')}"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { notesExpanded = !notesExpanded },
                    color = if (notesExpanded) HevyntAccentContainer else HevyntSurfaceInteractive,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.semantics {
                        contentDescription = if (notesExpanded) "Close exercise notes" else "Open exercise notes"
                    },
                ) {
                    Text(
                        text = "Notes",
                        color = if (notesExpanded) HevyntAccent else HevyntTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }

                Surface(
                    onClick = onEditRest,
                    color = HevyntSurfaceInteractive,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.semantics {
                        contentDescription = "Edit rest timer"
                    },
                ) {
                    Text(
                        text = restLabel,
                        color = HevyntTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
            }

            if (notesExpanded) {
                Spacer(Modifier.height(10.dp))
                NoteEditor(
                    value = exercise.notes,
                    onValueChange = onNotesChanged,
                    onDone = { notesExpanded = false },
                )
            }

            Spacer(Modifier.height(16.dp))

            SetHeader()

            Spacer(Modifier.height(4.dp))

            val firstUncompletedSetId = exercise.sets.firstOrNull { !it.completed }?.id
            val activeSetId = selectedSetId ?: firstUncompletedSetId
            exercise.sets.forEachIndexed { idx, set ->
                key(set.id) {
                    val isSelected = activeSetId == set.id
                    val preceding = exercise.sets.getOrNull(idx - 1)
                    SetRow(
                        index = idx,
                        set = set,
                        previous = previousSets[set.id],
                        precedingSet = preceding,
                        isSelected = isSelected,
                        onSelect = {
                            selectedSetId = if (selectedSetId == set.id) null else set.id
                        },
                        onChanged = { updatedSet -> onSetChanged(set.id, updatedSet) },
                        onCompleted = {
                            val becomingComplete = !set.completed
                            onSetCompletionChanged(set.id, becomingComplete)
                            if (becomingComplete) {
                                selectedSetId = exercise.sets.firstOrNull { it.id != set.id && !it.completed }?.id
                            }
                        },
                        onDelete = {
                            if (set.completed) setPendingDelete = set else onRemoveSet(set.id)
                        },
                        onDuplicate = { onDuplicateSet(set.id) },
                        onCopy = {
                            val prev = previousSets[set.id]
                            if (prev != null) {
                                onSetChanged(set.id, ActiveWorkoutOperations.copyPreviousSet(set, prev))
                            } else if (preceding != null) {
                                onSetChanged(set.id, ActiveWorkoutOperations.copyPrecedingSet(set, preceding))
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddSet)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+ Add set",
                    color = HevyntTextSecondary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ExerciseDetailsAndOptionsDialog(
    exerciseName: String,
    exerciseHistory: ExerciseHistorySummary?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isRoutineBuilder: Boolean,
    isGlyphSupported: Boolean,
    isGlyphTimerEnabled: Boolean,
    isGlyphPulseEnabled: Boolean,
    onToggleGlyphTimer: (Boolean) -> Unit,
    onToggleGlyphPulse: (Boolean) -> Unit,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = exerciseName.ifBlank { "Exercise" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ExerciseHistoryPanel(history = exerciseHistory)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit exercise name", modifier = Modifier.fillMaxWidth(), color = HevyntTextPrimary)
                }
                TextButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                    Text("Add exercise to workout", modifier = Modifier.fillMaxWidth(), color = HevyntTextPrimary)
                }
                if (canMoveUp) {
                    TextButton(onClick = onMoveUp, modifier = Modifier.fillMaxWidth()) {
                        Text("Move exercise up", modifier = Modifier.fillMaxWidth(), color = HevyntTextPrimary)
                    }
                }
                if (canMoveDown) {
                    TextButton(onClick = onMoveDown, modifier = Modifier.fillMaxWidth()) {
                        Text("Move exercise down", modifier = Modifier.fillMaxWidth(), color = HevyntTextPrimary)
                    }
                }
                if (isGlyphSupported) {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Glyph rest timer", color = HevyntTextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isGlyphTimerEnabled,
                            onCheckedChange = onToggleGlyphTimer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Glyph pulse on finish", color = HevyntTextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isGlyphPulseEnabled,
                            onCheckedChange = onToggleGlyphPulse,
                            enabled = isGlyphTimerEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onRemoveExercise, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Remove exercise",
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onDiscardWorkout, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isRoutineBuilder) "Discard routine" else "Discard workout",
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = HevyntAccent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
internal fun ExerciseAutocompletePanel(
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
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 6.dp)
            .background(HevyntSurfaceRaised, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                .border(1.dp, HevyntOutline, RoundedCornerShape(14.dp))
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
internal fun NoteEditor(
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
            .background(HevyntSurfaceInteractive, RoundedCornerShape(14.dp))
            .focusRequester(focusRequester)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
internal fun EmptyWorkoutState(
    isRoutineBuilder: Boolean,
    onAddExercise: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .padding(horizontal = 24.dp),
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
