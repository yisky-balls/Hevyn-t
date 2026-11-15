package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
fun WorkoutProgramsScreen(
    folders: List<RoutineFolderSummary>,
    likelyNext: RoutineRecommendation?,
    activeWorkout: ActiveWorkout?,
    canRepeatLast: Boolean = false,
    listState: LazyListState,
    onContinueWorkout: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onBuildRoutine: (folderId: String, routineId: String?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (folderId: String, name: String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onDeleteRoutine: (folderId: String, routineId: String) -> Unit,
    onStartEmpty: () -> Unit,
    onRepeatLast: () -> Unit = {},
    onImportPlan: () -> Unit = {},
    onManageSavedExercises: () -> Unit = {},
) {
    var expandedFolderIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showFolderPicker by rememberSaveable { mutableStateOf(false) }
    var showCreateFolder by rememberSaveable { mutableStateOf(false) }
    var folderToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var routineToDelete by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val folderToRename = folders.firstOrNull { it.id == folderToRenameId }
    val folderToDelete = folders.firstOrNull { it.id == folderToDeleteId }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            onDismiss = { showFolderPicker = false },
            onSelect = { folderId ->
                showFolderPicker = false
                onBuildRoutine(folderId, null)
            },
            onCreateFolder = {
                showFolderPicker = false
                showCreateFolder = true
            },
        )
    }
    if (showCreateFolder) {
        NameDialog(
            title = "NEW FOLDER",
            initialValue = "",
            confirmLabel = "CREATE",
            onDismiss = { showCreateFolder = false },
            onConfirm = {
                showCreateFolder = false
                onCreateFolder(it)
            },
        )
    }
    folderToRename?.let { folder ->
        NameDialog(
            title = "RENAME FOLDER",
            initialValue = folder.name,
            confirmLabel = "SAVE",
            onDismiss = { folderToRenameId = null },
            onConfirm = {
                folderToRenameId = null
                onRenameFolder(folder.id, it)
            },
        )
    }
    folderToDelete?.let { folder ->
        DeleteDialog(
            title = "DELETE ${folder.name.uppercase()}?",
            message = if (folder.routines.isEmpty()) {
                "This folder will be removed."
            } else {
                "This also removes ${formatRoutineCount(folder.routines.size).lowercase()}."
            },
            onDismiss = { folderToDeleteId = null },
            onConfirm = {
                folderToDeleteId = null
                onDeleteFolder(folder.id)
            },
        )
    }
    routineToDelete?.takeIf { it.size == 2 }?.let { ids ->
        val folderId = ids[0]
        val routineId = ids[1]
        val routineName = folders.asSequence()
            .flatMap { it.routines.asSequence() }
            .firstOrNull { it.id == routineId }
            ?.name
            .orEmpty()
        DeleteDialog(
            title = "DELETE ROUTINE?",
            message = "${routineName.ifBlank { "This routine" }} will be removed.",
            onDismiss = { routineToDelete = null },
            onConfirm = {
                routineToDelete = null
                onDeleteRoutine(folderId, routineId)
            },
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HevyntPageHorizontalPadding,
            end = HevyntPageHorizontalPadding,
            top = 20.dp,
            bottom = 28.dp,
        ),
    ) {
        item(key = "launcher-header") {
            HevyntPageHeader(
                title = "WORKOUT",
                subtitle = "Choose your next session.",
            )
            Spacer(Modifier.height(18.dp))
            when {
                activeWorkout != null -> ContinueWorkoutCard(activeWorkout, onContinueWorkout)
                likelyNext != null -> LikelyNextRoutineCard(
                    recommendation = likelyNext,
                    onStart = { onStartRoutine(likelyNext.routine.id) },
                    onOpen = { onBuildRoutine(likelyNext.routine.folderId, likelyNext.routine.id) },
                )
            }
            Spacer(Modifier.height(12.dp))
            SecondaryActionRow(
                label = "START EMPTY WORKOUT",
                enabled = activeWorkout == null,
                onClick = onStartEmpty,
            )
            if (canRepeatLast) {
                Spacer(Modifier.height(8.dp))
                SecondaryActionRow(
                    label = "REPEAT LAST WORKOUT",
                    icon = Icons.Outlined.Refresh,
                    enabled = activeWorkout == null,
                    onClick = onRepeatLast,
                )
            }
            Spacer(Modifier.height(8.dp))
            SecondaryActionRow(
                label = "IMPORT WORKOUT PLAN",
                icon = Icons.Outlined.KeyboardArrowDown,
                enabled = true,
                onClick = onImportPlan,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryActionRow(
                label = "MANAGE SAVED EXERCISES",
                icon = Icons.AutoMirrored.Outlined.List,
                enabled = true,
                onClick = onManageSavedExercises,
            )
        }

        item(key = "routine-heading") {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ROUTINES",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
                HevyntSectionLabel(
                    text = "${folders.sumOf { it.routines.size }} SAVED",
                    color = HevyntTextSecondary,
                )
                IconButton(onClick = { showCreateFolder = true }) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = "Create folder")
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = {
                    if (folders.isEmpty()) showCreateFolder = true else showFolderPicker = true
                },
                color = HevyntSurfaceRaised,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, HevyntOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    HevyntSectionLabel("NEW ROUTINE", Modifier.padding(start = 12.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        folders.forEach { folder ->
            val expanded = folder.id in expandedFolderIds
            item(key = "folder:${folder.id}") {
                FolderRow(
                    folder = folder,
                    expanded = expanded,
                    onOpen = {
                        expandedFolderIds = if (expanded) {
                            expandedFolderIds - folder.id
                        } else {
                            expandedFolderIds + folder.id
                        }
                    },
                    onRename = { folderToRenameId = folder.id },
                    onDelete = { folderToDeleteId = folder.id },
                )
                Spacer(Modifier.height(if (expanded) 8.dp else 12.dp))
            }
            if (expanded) {
                folder.routines.forEach { routine ->
                    item(key = "routine:${routine.id}") {
                        RoutineCard(
                            routine = routine,
                            startEnabled = activeWorkout == null && routine.exerciseCount > 0,
                            onOpen = { onBuildRoutine(folder.id, routine.id) },
                            onStart = { onStartRoutine(routine.id) },
                            onEdit = { onBuildRoutine(folder.id, routine.id) },
                            onDelete = { routineToDelete = listOf(folder.id, routine.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item(key = "folder-add:${folder.id}") {
                    TextButton(
                        onClick = { onBuildRoutine(folder.id, null) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        HevyntSectionLabel(
                            "ADD ROUTINE",
                            Modifier.padding(start = 10.dp),
                            HevyntTextSecondary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (folders.isEmpty()) {
            item(key = "empty-library") {
                Text(
                    "NO ROUTINES YET",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateFolder = true }
                        .padding(vertical = 30.dp),
                    color = HevyntTextSecondary,
                    fontFamily = HevyntDisplayFont,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}