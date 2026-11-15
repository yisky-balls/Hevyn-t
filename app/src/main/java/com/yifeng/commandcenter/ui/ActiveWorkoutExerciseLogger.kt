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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    index: Int,
    exercise: ActiveExercise,
    previousSets: Map<String, PreviousSetPerformance>,
    exerciseHistory: ExerciseHistorySummary? = null,
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
    onDuplicateSet: (String) -> Unit,
) {
    var expanded by rememberSaveable(exercise.id) { mutableStateOf(defaultExpanded) }
    var notesExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var historyExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var selectedSetId by rememberSaveable(exercise.id) { mutableStateOf<String?>(null) }
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(HevyntSurfaceRaised, RoundedCornerShape(22.dp))
            .border(1.dp, HevyntOutline, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    onNameChanged,
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
                    collapsedPreviousSummary(exercise, previousSets, exerciseHistory)?.let { summary ->
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
                label = "HISTORY",
                active = historyExpanded,
                icon = Icons.Outlined.Info,
                contentDescription = if (historyExpanded) "Close exercise history" else "View exercise history",
                onClick = { historyExpanded = !historyExpanded },
            )
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

        if (historyExpanded) {
            ExerciseHistoryPanel(history = exerciseHistory)
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
        val firstUncompletedSetId = exercise.sets.firstOrNull { !it.completed }?.id
        val activeSetId = selectedSetId ?: firstUncompletedSetId
        exercise.sets.forEachIndexed { index, set ->
            key(set.id) {
                val isSelected = activeSetId == set.id
                val preceding = exercise.sets.getOrNull(index - 1)
                SetRow(
                    index = index,
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
                if (isSelected) {
                    QuickSetControlsBar(
                        set = set,
                        previous = previousSets[set.id],
                        precedingSet = preceding,
                        onWeightAdjust = { delta ->
                            onSetChanged(set.id, ActiveWorkoutOperations.adjustWeight(set, delta))
                        },
                        onRepsAdjust = { delta ->
                            onSetChanged(set.id, ActiveWorkoutOperations.adjustReps(set, delta))
                        },
                        onCopy = {
                            val prev = previousSets[set.id]
                            if (prev != null) {
                                onSetChanged(set.id, ActiveWorkoutOperations.copyPreviousSet(set, prev))
                            } else if (preceding != null) {
                                onSetChanged(set.id, ActiveWorkoutOperations.copyPrecedingSet(set, preceding))
                            }
                        },
                        onDuplicate = { onDuplicateSet(set.id) },
                    )
                }
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
            Text(
                text = "+ Add set",
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
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
internal fun EmptyWorkoutState(
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
internal fun UtilityChip(
    label: String,
    icon: ImageVector,
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
