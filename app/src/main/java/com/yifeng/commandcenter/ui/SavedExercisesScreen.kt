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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ExerciseRoutineUsage
import com.yifeng.commandcenter.data.SavedExerciseSummary
import com.yifeng.commandcenter.data.SavedExercisesDraft
import com.yifeng.commandcenter.history.normalizeExerciseName
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.Locale

@Composable
fun SavedExercisesScreen(
    draft: SavedExercisesDraft,
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

    LaunchedEffect(draft.duplicateCount) {
        if (draft.duplicateCount == 0 && onlyDuplicatesFilter) {
            onlyDuplicatesFilter = false
        }
    }

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
            title = "Delete ${exercise.canonicalName}?",
            message = "This removes \"${exercise.canonicalName}\" from ${exercise.totalRoutinesCount} routine${if (exercise.totalRoutinesCount == 1) "" else "s"}. Changes stay uncommitted until you save.",
            confirmText = "Delete",
            dismissText = "Cancel",
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
            title = "Remove duplicate?",
            message = "Remove the duplicate \"${usage.exerciseName}\" entry from \"${usage.routineName}\"?",
            confirmText = "Remove",
            dismissText = "Cancel",
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
            title = "Deduplicate all exercises?",
            message = "Unifies casing/spacing differences and merges duplicate entries within the same routines. You can review all changes before saving.",
            confirmText = "Deduplicate",
            dismissText = "Cancel",
            onConfirm = {
                showDeduplicateAllConfirm = false
                onDeduplicateAll()
            },
            onDismiss = { showDeduplicateAllConfirm = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (draft.hasChanges) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Surface(
                        color = HevyntSurfaceRaised,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HevyntSignalDot(color = HevyntAccent)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = "Unsaved changes",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HevyntTextPrimary,
                                    )
                                }
                                Text(
                                    text = "Review edits before saving",
                                    fontSize = 12.sp,
                                    color = HevyntTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HevyntTonalButton(
                                    onClick = onBack,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = "Discard",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HevyntTextSecondary,
                                    )
                                }
                                HevyntPrimaryButton(
                                    onClick = onCommit,
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = "Save",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
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
                top = 12.dp,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = HevyntTextPrimary,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved exercises",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HevyntTextPrimary,
                        )
                        Text(
                            text = "Edit names, defaults, and clean up duplicates",
                            fontSize = 13.sp,
                            color = HevyntTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (draft.hasChanges) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = onCommit,
                            shape = RoundedCornerShape(50),
                            color = HevyntAccent,
                        ) {
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            item(key = "search-bar") {
                HevyntTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search saved exercises…",
                    containerColor = HevyntSurfaceRaised,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = HevyntTextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = HevyntTextSecondary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            if (draft.duplicateCount > 0) {
                item(key = "duplicate-banner") {
                    Surface(
                        color = HevyntSurfaceRaised,
                        border = BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HevyntSignalDot(color = HevyntAccent)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = "${draft.duplicateCount} duplicate${if (draft.duplicateCount == 1) "" else "s"} detected",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HevyntTextPrimary,
                                    )
                                }
                                Text(
                                    text = "Conflicting names or repeated entries found.",
                                    fontSize = 12.5.sp,
                                    color = HevyntTextSecondary,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            HevyntOutlineButton(
                                onClick = { showDeduplicateAllConfirm = true },
                                accent = true,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text("Clean up", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item(key = "filter-chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HevyntChip(
                        text = "All · ${draft.exercises.size}",
                        active = !onlyDuplicatesFilter,
                        onClick = { onlyDuplicatesFilter = false },
                    )
                    if (draft.duplicateCount > 0) {
                        HevyntChip(
                            text = "Duplicates · ${draft.duplicateCount}",
                            active = onlyDuplicatesFilter,
                            leadingDotColor = HevyntAccent,
                            onClick = { onlyDuplicatesFilter = true },
                        )
                    }
                }
            }

            if (filteredExercises.isEmpty()) {
                item(key = "empty-list") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = HevyntSurfaceRaised,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 44.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching exercises" else "No saved exercises yet",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = HevyntTextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "Try clearing or adjusting your search query."
                                } else {
                                    "Exercises from your routine library will appear here."
                                },
                                fontSize = 13.5.sp,
                                color = HevyntTextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            items(filteredExercises, key = { it.normalizedKey }) { exercise ->
                SavedExerciseCard(
                    exercise = exercise,
                    onEdit = { exerciseToEdit = exercise },
                    onMerge = { exerciseToMerge = exercise },
                    onDelete = { exerciseToDelete = exercise },
                    onDeleteInstance = { usage -> instanceToDelete = usage },
                )
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
        shape = RoundedCornerShape(24.dp),
        border = if (exercise.isDuplicate) BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.45f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = exercise.canonicalName,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (exercise.isDuplicate) {
                    HevyntChip(
                        text = "Duplicate",
                        active = true,
                        leadingDotColor = HevyntAccent,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            val restMin = exercise.restSeconds / 60
            val restSec = exercise.restSeconds % 60
            val restFormatted = "$restMin:${restSec.toString().padStart(2, '0')} rest"
            val metaParts = buildList {
                add("${exercise.totalRoutinesCount} routine${if (exercise.totalRoutinesCount == 1) "" else "s"}")
                add(restFormatted)
                if (exercise.notes.isNotBlank()) add(exercise.notes)
            }
            Text(
                text = metaParts.joinToString(" · "),
                fontSize = 13.sp,
                color = HevyntTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (exercise.variantNames.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Variants: ${exercise.variantNames.joinToString(", ")}",
                    fontSize = 12.5.sp,
                    color = HevyntAccent,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(12.dp))
            HevyntDottedRule(color = HevyntDivider)
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    fontSize = 13.5.sp,
                                    color = if (isDuplicateInRoutine) HevyntAccent else HevyntTextPrimary,
                                    fontWeight = if (isDuplicateInRoutine) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = " · ${usage.setCount} sets",
                                    fontSize = 13.sp,
                                    color = HevyntTextSecondary,
                                )
                                if (isDuplicateInRoutine && index > 0) {
                                    Text(
                                        text = " · Duplicate",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HevyntError,
                                    )
                                }
                            }
                            if (isDuplicateInRoutine && index > 0) {
                                IconButton(
                                    onClick = { onDeleteInstance(usage) },
                                    modifier = Modifier.size(28.dp),
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

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onMerge,
                    shape = RoundedCornerShape(50),
                    color = HevyntSurfaceInteractive,
                ) {
                    Text(
                        text = "Merge",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HevyntTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(50),
                    color = HevyntSurfaceInteractive,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = HevyntTextPrimary,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Edit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntTextPrimary,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(50),
                    color = HevyntAccentContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete exercise",
                            modifier = Modifier.size(14.dp),
                            tint = HevyntError,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Delete",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntError,
                        )
                    }
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
    val normalizedNew = normalizeExerciseName(name)
    val willMerge = normalizedNew.isNotEmpty() &&
        normalizedNew != exercise.normalizedKey &&
        existingNames.any { normalizeExerciseName(it) == normalizedNew }

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Edit exercise",
        subtitle = "Updates defaults across all linked routines",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HevyntTextField(
                value = name,
                onValueChange = { name = it },
                label = "Exercise name",
                placeholder = "e.g. Bench press",
                singleLine = true,
            )

            if (willMerge) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HevyntAccentContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "\"${name.trim()}\" already exists. Saving will merge them across all routines.",
                        fontSize = 13.sp,
                        color = HevyntAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Default rest",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = HevyntTextSecondary,
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", restSeconds / 60, restSeconds % 60),
                        fontFamily = HevyntDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = HevyntRestBlue,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        val selected = restSeconds == preset
                        Surface(
                            onClick = { restSeconds = preset },
                            shape = RoundedCornerShape(50),
                            color = if (selected) HevyntRestBlue.copy(alpha = 0.16f) else HevyntSurfaceInteractive,
                            border = if (selected) BorderStroke(1.dp, HevyntRestBlue.copy(alpha = 0.8f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${preset / 60}:${(preset % 60).toString().padStart(2, '0')}",
                                    fontSize = 12.sp,
                                    color = if (selected) HevyntRestBlue else HevyntTextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            HevyntTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Default notes",
                placeholder = "Technique cues or setup notes",
                singleLine = false,
                minHeight = 76.dp,
                maxLines = 3,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HevyntTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HevyntTextSecondary,
                    )
                }
                HevyntPrimaryButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            onConfirm(clean, notes.trim(), restSeconds)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (willMerge) "Merge & save" else "Save",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
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

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Merge \"${sourceExercise.canonicalName}\"",
        subtitle = "Select the target exercise to merge all routines into.",
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HevyntTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = "Filter target exercises…",
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredTargets, key = { it.normalizedKey }) { target ->
                    val isSelected = selectedTarget == target.canonicalName
                    Surface(
                        onClick = { selectedTarget = target.canonicalName },
                        color = if (isSelected) HevyntAccentContainer else HevyntSurfaceInteractive,
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) BorderStroke(1.dp, HevyntAccent) else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.canonicalName,
                                    fontSize = 15.sp,
                                    color = if (isSelected) HevyntAccent else HevyntTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${target.totalRoutinesCount} routine${if (target.totalRoutinesCount == 1) "" else "s"}",
                                    fontSize = 12.5.sp,
                                    color = HevyntTextSecondary,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = HevyntAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HevyntTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HevyntTextSecondary,
                    )
                }
                HevyntPrimaryButton(
                    enabled = selectedTarget != null,
                    onClick = { selectedTarget?.let(onConfirm) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Merge",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
