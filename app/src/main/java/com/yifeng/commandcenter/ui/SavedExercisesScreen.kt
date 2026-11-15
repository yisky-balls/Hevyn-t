package com.yifeng.commandcenter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ExerciseRoutineUsage
import com.yifeng.commandcenter.data.SavedExerciseSummary
import com.yifeng.commandcenter.data.SavedExercisesDraft
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntCanvas
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
fun SavedExercisesScreen(
    draft: SavedExercisesDraft,
    onRenameExercise: (oldName: String, newName: String) -> Unit,
    onEditExerciseDetails: (oldName: String, newName: String, notes: String, restSeconds: Int) -> Unit,
    onMergeExercises: (sourceName: String, targetName: String) -> Unit,
    onDeleteExercise: (String) -> Unit,
    onDeleteExerciseInstance: (routineId: String, exerciseId: String) -> Unit,
    onDeduplicateAll: () -> Unit,
    onCommit: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var onlyDuplicatesFilter by rememberSaveable { mutableStateOf(false) }

    var exerciseToEdit by remember { mutableStateOf<SavedExerciseSummary?>(null) }
    var exerciseToMerge by remember { mutableStateOf<SavedExerciseSummary?>(null) }
    var exerciseToDelete by remember { mutableStateOf<SavedExerciseSummary?>(null) }
    var instanceToDelete by remember { mutableStateOf<ExerciseRoutineUsage?>(null) }
    var showDeduplicateAllConfirm by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val filteredExercises = remember(draft.exercises, searchQuery, onlyDuplicatesFilter) {
        draft.exercises.filter { exercise ->
            val matchesSearch = searchQuery.isBlank() ||
                exercise.canonicalName.contains(searchQuery.trim(), ignoreCase = true) ||
                exercise.variantNames.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            val matchesDuplicate = !onlyDuplicatesFilter || exercise.isDuplicate
            matchesSearch && matchesDuplicate
        }
    }

    // Dialogs
    exerciseToEdit?.let { exercise ->
        EditSavedExerciseDialog(
            exercise = exercise,
            existingNames = draft.exercises.map { it.canonicalName },
            onDismiss = { exerciseToEdit = null },
            onConfirm = { newName, newNotes, newRest ->
                exerciseToEdit = null
                onEditExerciseDetails(exercise.canonicalName, newName, newNotes, newRest)
            },
        )
    }

    exerciseToMerge?.let { exercise ->
        MergeSavedExerciseDialog(
            sourceExercise = exercise,
            availableTargets = draft.exercises.filter { it.normalizedKey != exercise.normalizedKey },
            onDismiss = { exerciseToMerge = null },
            onConfirm = { targetName ->
                exerciseToMerge = null
                onMergeExercises(exercise.canonicalName, targetName)
            },
        )
    }

    exerciseToDelete?.let { exercise ->
        HevyntConfirmDialog(
            title = "DELETE ${exercise.canonicalName.uppercase()}?",
            message = "This removes '${exercise.canonicalName}' from ${exercise.totalRoutinesCount} routine(s). This change will remain uncommitted until you save.",
            confirmText = "DELETE",
            isDestructive = true,
            onConfirm = {
                exerciseToDelete = null
                onDeleteExercise(exercise.canonicalName)
            },
            onDismiss = { exerciseToDelete = null },
        )
    }

    instanceToDelete?.let { usage ->
        HevyntConfirmDialog(
            title = "DELETE DUPLICATE IN ${usage.routineName.uppercase()}?",
            message = "Remove this duplicate '${usage.exerciseName}' from routine '${usage.routineName}'?",
            confirmText = "REMOVE",
            isDestructive = true,
            onConfirm = {
                instanceToDelete = null
                onDeleteExerciseInstance(usage.routineId, usage.exerciseId)
            },
            onDismiss = { instanceToDelete = null },
        )
    }

    if (showDeduplicateAllConfirm) {
        HevyntConfirmDialog(
            title = "DEDUPLICATE ALL EXERCISES?",
            message = "This unifies casing/spacing differences and merges duplicate exercise entries within the same routines (combining their sets). You can review all changes before committing.",
            confirmText = "DEDUPLICATE",
            onConfirm = {
                showDeduplicateAllConfirm = false
                onDeduplicateAll()
            },
            onDismiss = { showDeduplicateAllConfirm = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (draft.hasChanges) {
                Surface(
                    color = HevyntSurfaceRaised,
                    border = BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HevyntSignalDot(color = HevyntAccent)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "UNCOMMITTED CHANGES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HevyntAccent,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Text(
                                text = "Review edits before saving",
                                style = MaterialTheme.typography.bodySmall,
                                color = HevyntTextSecondary,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onBack) {
                                Text("DISCARD", color = HevyntTextSecondary)
                            }
                            HevyntPrimaryButton(
                                onClick = onCommit,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            ) {
                                Text("COMMIT", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = HevyntPageHorizontalPadding,
                end = HevyntPageHorizontalPadding,
                top = 16.dp,
                bottom = 24.dp,
            ),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to workout launcher",
                            tint = HevyntTextPrimary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAVED EXERCISES",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HevyntTextPrimary,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = "Edit exercises and clean up duplicates before committing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HevyntTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (draft.hasChanges) {
                        TextButton(onClick = onCommit) {
                            Text(
                                "SAVE",
                                color = HevyntAccent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Search bar
            item(key = "search-bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search saved exercises…", color = HevyntTextSecondary) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = HevyntTextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = HevyntTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = hevyntTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                )
                Spacer(Modifier.height(10.dp))
            }

            // Duplicate detection alert banner
            if (draft.duplicateCount > 0) {
                item(key = "duplicate-banner") {
                    Surface(
                        color = HevyntSurfaceRaised,
                        border = BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HevyntSignalDot(color = HevyntAccent)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${draft.duplicateCount} DUPLICATES DETECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HevyntAccent,
                                        letterSpacing = 0.6.sp,
                                    )
                                }
                                Text(
                                    text = "Conflicting names or in-routine duplicates found.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HevyntTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            HevyntOutlineButton(
                                onClick = { showDeduplicateAllConfirm = true },
                                accent = true,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text("DEDUPLICATE ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Filter chips
            item(key = "filter-chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HevyntChip(
                        text = "ALL (${draft.exercises.size})",
                        active = !onlyDuplicatesFilter,
                        onClick = { onlyDuplicatesFilter = false },
                    )
                    if (draft.duplicateCount > 0) {
                        HevyntChip(
                            text = "DUPLICATES (${draft.duplicateCount})",
                            active = onlyDuplicatesFilter,
                            leadingDotColor = HevyntAccent,
                            onClick = { onlyDuplicatesFilter = true },
                        )
                    }
                }
            }

            // Empty state
            if (filteredExercises.isEmpty()) {
                item(key = "empty-list") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "NO MATCHING EXERCISES" else "NO SAVED EXERCISES YET",
                                style = MaterialTheme.typography.titleMedium,
                                color = HevyntTextSecondary,
                                letterSpacing = 0.6.sp,
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try clearing your search query." else "Exercises from your routine library will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HevyntTextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            // Exercise items
            items(filteredExercises, key = { it.normalizedKey }) { exercise ->
                SavedExerciseCard(
                    exercise = exercise,
                    onEdit = { exerciseToEdit = exercise },
                    onMerge = { exerciseToMerge = exercise },
                    onDelete = { exerciseToDelete = exercise },
                    onDeleteInstance = { usage -> instanceToDelete = usage },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SavedExerciseCard(
    exercise: SavedExerciseSummary,
    onEdit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    onDeleteInstance: (ExerciseRoutineUsage) -> Unit,
) {
    Surface(
        color = HevyntSurfaceRaised,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (exercise.isDuplicate) HevyntAccent.copy(alpha = 0.6f) else HevyntOutline,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = exercise.canonicalName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HevyntTextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (exercise.isDuplicate) {
                    HevyntChip(
                        text = "DUPLICATE",
                        active = true,
                        leadingDotColor = HevyntAccent,
                    )
                }
            }

            // Details metadata
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${exercise.totalRoutinesCount} routine${if (exercise.totalRoutinesCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HevyntTextSecondary,
                )
                Text("•", style = MaterialTheme.typography.bodySmall, color = HevyntTextSecondary)
                Text(
                    text = "${exercise.restSeconds}s rest",
                    style = MaterialTheme.typography.bodySmall,
                    color = HevyntTextSecondary,
                )
                if (exercise.notes.isNotBlank()) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = HevyntTextSecondary)
                    Text(
                        text = exercise.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = HevyntTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Variant warning
            if (exercise.variantNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Variants: ${exercise.variantNames.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HevyntAccent,
                )
            }

            // Routine usages list
            Spacer(Modifier.height(8.dp))
            HevyntDottedRule()
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Group by routine to show in-routine duplicates cleanly
                val groupedByRoutine = exercise.usages.groupBy { it.routineId }
                groupedByRoutine.forEach { (_, usagesInRoutine) ->
                    val isDuplicateInRoutine = usagesInRoutine.size > 1
                    usagesInRoutine.forEachIndexed { index, usage ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = usage.routineName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDuplicateInRoutine) HevyntAccent else HevyntTextPrimary,
                                    fontWeight = if (isDuplicateInRoutine) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = " (${usage.setCount} sets)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HevyntTextSecondary,
                                )
                                if (isDuplicateInRoutine && index > 0) {
                                    Text(
                                        text = " [Duplicate Entry]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HevyntError,
                                    )
                                }
                            }
                            if (isDuplicateInRoutine && index > 0) {
                                IconButton(
                                    onClick = { onDeleteInstance(usage) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete duplicate in this routine",
                                        tint = HevyntError,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card Action Buttons
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onMerge,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("MERGE", fontSize = 12.sp, color = HevyntTextSecondary)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HevyntAccent,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("EDIT", fontSize = 12.sp, color = HevyntAccent)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HevyntError,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("DELETE", fontSize = 12.sp, color = HevyntError)
                }
            }
        }
    }
}

@Composable
private fun EditSavedExerciseDialog(
    exercise: SavedExerciseSummary,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newNotes: String, newRestSeconds: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(exercise.canonicalName) }
    var notes by rememberSaveable { mutableStateOf(exercise.notes) }
    var restSeconds by rememberSaveable { mutableIntStateOf(exercise.restSeconds) }

    val presets = listOf(60, 90, 120, 180, 240, 300)
    val willMerge = name.trim().lowercase() != exercise.canonicalName.lowercase() &&
        existingNames.any { it.trim().equals(name.trim(), ignoreCase = true) }

    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "EDIT EXERCISE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    colors = hevyntTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (willMerge) {
                    Text(
                        text = "An exercise named '${name.trim()}' already exists. Saving will merge them across all routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HevyntAccent,
                    )
                }

                Text(
                    text = "DEFAULT REST: %02d:%02d".format(restSeconds / 60, restSeconds % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = HevyntTextSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    presets.forEach { preset ->
                        TextButton(
                            onClick = { restSeconds = preset },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            Text(
                                "${preset}s",
                                fontSize = 11.sp,
                                color = if (restSeconds == preset) HevyntAccent else HevyntTextSecondary,
                                fontWeight = if (restSeconds == preset) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Default notes / cues") },
                    colors = hevyntTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        onConfirm(clean, notes.trim(), restSeconds)
                    }
                },
            ) {
                Text(
                    if (willMerge) "MERGE & SAVE" else "SAVE",
                    color = HevyntAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = HevyntTextSecondary)
            }
        },
    )
}

@Composable
private fun MergeSavedExerciseDialog(
    sourceExercise: SavedExerciseSummary,
    availableTargets: List<SavedExerciseSummary>,
    onDismiss: () -> Unit,
    onConfirm: (targetName: String) -> Unit,
) {
    var selectedTarget by remember { mutableStateOf<String?>(null) }
    var filterQuery by rememberSaveable { mutableStateOf("") }

    val filteredTargets = remember(availableTargets, filterQuery) {
        if (filterQuery.isBlank()) availableTargets
        else availableTargets.filter { it.canonicalName.contains(filterQuery.trim(), ignoreCase = true) }
    }

    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "MERGE '${sourceExercise.canonicalName.uppercase()}'",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(modifier = Modifier.height(320.dp)) {
                Text(
                    text = "Select the target exercise to merge into. All routines using '${sourceExercise.canonicalName}' will be updated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HevyntTextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filter targets…", color = HevyntTextSecondary) },
                    singleLine = true,
                    colors = hevyntTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTargets, key = { it.normalizedKey }) { target ->
                        val isSelected = selectedTarget == target.canonicalName
                        Surface(
                            onClick = { selectedTarget = target.canonicalName },
                            color = if (isSelected) HevyntAccent.copy(alpha = 0.15f) else HevyntCanvas,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, if (isSelected) HevyntAccent else HevyntOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = target.canonicalName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) HevyntAccent else HevyntTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(
                                    text = "${target.totalRoutinesCount} routines",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HevyntTextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedTarget != null,
                onClick = { selectedTarget?.let(onConfirm) },
            ) {
                Text("MERGE", color = HevyntAccent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = HevyntTextSecondary)
            }
        },
    )
}
