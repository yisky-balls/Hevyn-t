package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
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
    val initialExpanded = rememberSaveable(folders.map { it.id }) {
        val targetFolder = likelyNext?.routine?.folderId ?: folders.firstOrNull()?.id
        listOfNotNull(targetFolder)
    }
    var expandedFolderIds by rememberSaveable { mutableStateOf(initialExpanded) }
    var showTopMenu by rememberSaveable { mutableStateOf(false) }
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
                "This also removes ${folder.routines.size} routine${if (folder.routines.size == 1) "" else "s"}."
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
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 100.dp,
        ),
    ) {
        // Top App Bar: "Workout" title on left, MoreVert on right
        item(key = "top-bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Workout",
                    color = HevyntTextPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 30.sp,
                    modifier = Modifier.weight(1f),
                )

                Box {
                    IconButton(
                        onClick = { showTopMenu = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Workout options",
                            tint = HevyntTextPrimary,
                        )
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import workout plan") },
                            leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onImportPlan()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Manage saved exercises") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                onManageSavedExercises()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }

        // Hero Up next Card / Continue Workout Card
        item(key = "hero-card") {
            when {
                activeWorkout != null -> {
                    ContinueWorkoutCard(
                        workout = activeWorkout,
                        onContinue = onContinueWorkout,
                    )
                }
                else -> {
                    val fallback = folders.flatMap { it.routines }.firstOrNull()
                    HeroUpNextCard(
                        recommendation = likelyNext,
                        fallbackRoutine = fallback,
                        onStart = {
                            val routineId = likelyNext?.routine?.id ?: fallback?.id
                            routineId?.let(onStartRoutine)
                        },
                        onOpen = {
                            val routine = likelyNext?.routine ?: fallback
                            routine?.let { onBuildRoutine(it.folderId, it.id) }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        // Quick Actions Row: "+ Empty workout" and "↻ Repeat last"
        item(key = "quick-actions") {
            QuickActionsRow(
                onStartEmpty = onStartEmpty,
                onRepeatLast = onRepeatLast,
            )

            Spacer(Modifier.height(24.dp))
        }

        // Routines Section Header: "Routines" on left, circular "+" on right
        item(key = "routines-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Routines",
                    color = HevyntTextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, HevyntOutline, CircleShape)
                        .clickable {
                            if (folders.isEmpty()) showCreateFolder = true else showFolderPicker = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New routine or folder",
                        tint = HevyntTextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        // Folder Accordion Cards
        folders.forEach { folder ->
            val expanded = folder.id in expandedFolderIds
            item(key = "folder-card:${folder.id}") {
                RoutineFolderCard(
                    folder = folder,
                    expanded = expanded,
                    recommendedRoutineId = likelyNext?.routine?.id,
                    onToggleExpand = {
                        expandedFolderIds = if (expanded) {
                            expandedFolderIds - folder.id
                        } else {
                            expandedFolderIds + folder.id
                        }
                    },
                    onStartRoutine = onStartRoutine,
                    onOpenRoutine = { routineId -> onBuildRoutine(folder.id, routineId) },
                    onEditFolder = { folderToRenameId = folder.id },
                    onDeleteFolder = { folderToDeleteId = folder.id },
                    onAddRoutine = { onBuildRoutine(folder.id, null) },
                    onEditRoutine = { routineId -> onBuildRoutine(folder.id, routineId) },
                    onDeleteRoutine = { routineId -> routineToDelete = listOf(folder.id, routineId) },
                )

                Spacer(Modifier.height(12.dp))
            }
        }

        if (folders.isEmpty()) {
            item(key = "empty-routines") {
                Text(
                    text = "NO ROUTINES YET",
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
