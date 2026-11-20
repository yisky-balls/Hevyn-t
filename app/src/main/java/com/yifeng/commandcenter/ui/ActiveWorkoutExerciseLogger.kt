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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
        HevyntConfirmDialog(
            title = "Remove exercise?",
            message = "This removes ${exercise.name.ifBlank { "the exercise" }} and all of its sets.",
            confirmText = "Remove",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                confirmExerciseDelete = false
                onDelete()
            },
            onDismiss = { confirmExerciseDelete = false },
        )
    }

    if (confirmDiscardSession) {
        HevyntConfirmDialog(
            title = if (isRoutineBuilder) "Discard routine?" else "Discard workout?",
            message = "This removes the active session from this device. Completed workout history is not affected.",
            confirmText = "Discard",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                confirmDiscardSession = false
                onDiscardWorkout()
            },
            onDismiss = { confirmDiscardSession = false },
        )
    }

    setPendingDelete?.let { set ->
        HevyntConfirmDialog(
            title = "Delete completed set?",
            message = "This set will be removed from your completed sets and workout volume.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                setPendingDelete = null
                onRemoveSet(set.id)
            },
            onDismiss = { setPendingDelete = null },
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
    HevyntSheet(
        onDismiss = onDismiss,
        title = exerciseName.ifBlank { "Exercise" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            ExerciseHistoryPanel(history = exerciseHistory)

            Spacer(Modifier.height(14.dp))

            HevyntSheetSection {
                HevyntSheetRow(
                    label = "Edit exercise name",
                    icon = Icons.Outlined.Edit,
                    onClick = onRename,
                )
                HevyntDottedRule(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = HevyntOutline.copy(alpha = 0.7f),
                )
                HevyntSheetRow(
                    label = "Add exercise to workout",
                    icon = Icons.Outlined.Add,
                    onClick = onAddExercise,
                )
                if (canMoveUp) {
                    HevyntDottedRule(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = HevyntOutline.copy(alpha = 0.7f),
                    )
                    HevyntSheetRow(
                        label = "Move exercise up",
                        icon = Icons.Outlined.KeyboardArrowUp,
                        onClick = onMoveUp,
                    )
                }
                if (canMoveDown) {
                    HevyntDottedRule(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = HevyntOutline.copy(alpha = 0.7f),
                    )
                    HevyntSheetRow(
                        label = "Move exercise down",
                        icon = Icons.Outlined.KeyboardArrowDown,
                        onClick = onMoveDown,
                    )
                }
            }

            if (isGlyphSupported) {
                Spacer(Modifier.height(12.dp))
                HevyntSheetSection {
                    HevyntSheetRow(
                        label = "Glyph rest timer",
                        subtitle = "Rear LED countdown during rest",
                        onClick = { onToggleGlyphTimer(!isGlyphTimerEnabled) },
                        trailing = {
                            Switch(
                                checked = isGlyphTimerEnabled,
                                onCheckedChange = onToggleGlyphTimer,
                                colors = hevyntSwitchColors(),
                            )
                        },
                    )
                    HevyntDottedRule(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = HevyntOutline.copy(alpha = 0.7f),
                    )
                    HevyntSheetRow(
                        label = "Glyph pulse on finish",
                        subtitle = "Flash all zones when rest ends",
                        enabled = isGlyphTimerEnabled,
                        onClick = { onToggleGlyphPulse(!isGlyphPulseEnabled) },
                        trailing = {
                            Switch(
                                checked = isGlyphPulseEnabled,
                                onCheckedChange = onToggleGlyphPulse,
                                enabled = isGlyphTimerEnabled,
                                colors = hevyntSwitchColors(),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            HevyntSheetSection {
                HevyntSheetRow(
                    label = "Remove exercise",
                    icon = Icons.Outlined.Delete,
                    destructive = true,
                    onClick = onRemoveExercise,
                )
                HevyntDottedRule(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = HevyntOutline.copy(alpha = 0.7f),
                )
                HevyntSheetRow(
                    label = if (isRoutineBuilder) "Discard routine" else "Discard workout",
                    icon = Icons.Outlined.Close,
                    destructive = true,
                    onClick = onDiscardWorkout,
                )
            }
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
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 6.dp)
            .background(HevyntSurfaceRaised, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Add exercise",
                color = HevyntTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close exercise search",
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HevyntSurfaceInteractive)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = HevyntTextSecondary,
                modifier = Modifier.size(19.dp),
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
                    color = HevyntTextPrimary,
                    fontSize = 15.5.sp,
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
                cursorBrush = SolidColor(HevyntAccent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search exercises…",
                                color = HevyntTextSecondary,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (matches.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }

        matches.forEachIndexed { idx, suggestion ->
            if (idx > 0) {
                HevyntDottedRule()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(suggestion) }
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = suggestion.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HevyntTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    suggestion.lastPerformance?.let { previous ->
                        Text(
                            text = "Last: ${PreviousSetResolver.format(previous)}",
                            modifier = Modifier.padding(top = 2.dp),
                            color = HevyntTextSecondary,
                            fontSize = 12.5.sp,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(HevyntSurfaceInteractive, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add ${suggestion.name}",
                        tint = HevyntAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(ExerciseAutocomplete.custom(query)) }
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(HevyntAccentContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = HevyntAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "Create \"${query.trim()}\"",
                    color = HevyntAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp),
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
            fontSize = 13.5.sp,
        ),
        cursorBrush = SolidColor(HevyntAccent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Add a short note…",
                        color = HevyntTextSecondary,
                        fontSize = 13.5.sp,
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
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(HevyntSurfaceInteractive, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_dumbbell),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = HevyntTextPrimary,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (isRoutineBuilder) "Build your routine" else "Get started",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add an exercise to ${if (isRoutineBuilder) "build your routine" else "start your workout"}.",
                color = HevyntTextSecondary,
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            HevyntPrimaryButton(
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                Text(
                    text = "  Add exercise",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            HevyntTonalButton(
                onClick = onDiscard,
                destructive = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = if (isRoutineBuilder) "Discard routine" else "Discard workout",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
