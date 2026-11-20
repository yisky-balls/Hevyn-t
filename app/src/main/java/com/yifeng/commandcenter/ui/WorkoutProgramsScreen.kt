package com.yifeng.commandcenter.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.program.ProgramSheetState
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
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
    programSheet: ProgramSheetState = ProgramSheetState(),
    onOpenProgramSheetConfig: () -> Unit = {},
    onRefreshProgramSheet: () -> Unit = {},
    onSelectProgramSheetWeek: (Int) -> Unit = {},
    onStartProgramSheetDay: (weekNumber: Int, dayName: String) -> Unit = { _, _ -> },
    onReorderRoutines: (folderId: String, orderedRoutineIds: List<String>) -> Unit = { _, _ -> },
) {
    val allRoutines = folders.flatMap { it.routines }
    val targetRoutine = likelyNext?.routine
        ?: allRoutines.firstOrNull { it.exerciseCount > 0 }
        ?: allRoutines.firstOrNull()
    val defaultFolderId = targetRoutine?.folderId
        ?: folders.firstOrNull { it.routines.isNotEmpty() }?.id
        ?: folders.firstOrNull()?.id
    var expandedFolderIds by rememberSaveable(defaultFolderId) {
        mutableStateOf(listOfNotNull(defaultFolderId))
    }
    var showTopMenu by rememberSaveable { mutableStateOf(false) }
    var showFolderPicker by rememberSaveable { mutableStateOf(false) }
    var showCreateFolder by rememberSaveable { mutableStateOf(false) }
    var folderToRenameId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var routineToDelete by rememberSaveable { mutableStateOf<List<String>?>(null) }

    val folderToRename = folders.firstOrNull { it.id == folderToRenameId }
    val folderToDelete = folders.firstOrNull { it.id == folderToDeleteId }

    if (showTopMenu) {
        HevyntActionSheet(
            title = "Workout",
            actions = listOf(
                HevyntSheetAction(
                    label = "New routine",
                    icon = Icons.Outlined.Add,
                    onClick = {
                        if (folders.isEmpty()) showCreateFolder = true else showFolderPicker = true
                    },
                ),
                HevyntSheetAction(
                    label = "New folder",
                    icon = Icons.Outlined.Add,
                    onClick = { showCreateFolder = true },
                ),
                HevyntSheetAction(
                    label = "Google Sheet program",
                    icon = Icons.Outlined.KeyboardArrowDown,
                    onClick = onOpenProgramSheetConfig,
                ),
                HevyntSheetAction(
                    label = "Import workout plan",
                    icon = Icons.Outlined.KeyboardArrowDown,
                    onClick = onImportPlan,
                ),
                HevyntSheetAction(
                    label = "Manage saved exercises",
                    icon = Icons.AutoMirrored.Outlined.List,
                    onClick = onManageSavedExercises,
                ),
            ),
            onDismiss = { showTopMenu = false },
        )
    }

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
            title = "New folder",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateFolder = false },
            onConfirm = {
                showCreateFolder = false
                onCreateFolder(it)
            },
        )
    }
    folderToRename?.let { folder ->
        NameDialog(
            title = "Rename folder",
            initialValue = folder.name,
            confirmLabel = "Save",
            onDismiss = { folderToRenameId = null },
            onConfirm = {
                folderToRenameId = null
                onRenameFolder(folder.id, it)
            },
        )
    }
    folderToDelete?.let { folder ->
        DeleteDialog(
            title = "Delete ${folder.name}?",
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
            title = "Delete routine?",
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
            top = 14.dp,
            bottom = 124.dp,
        ),
    ) {
        // Top App Bar: "Workout" title on left, MoreVert on right
        item(key = "top-bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Workout",
                    color = HevyntTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.weight(1f),
                )

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
            }

            Spacer(Modifier.height(14.dp))
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
                    HeroUpNextCard(
                        recommendation = likelyNext,
                        fallbackRoutine = targetRoutine,
                        onStart = {
                            targetRoutine?.let { routine ->
                                if (routine.exerciseCount > 0) {
                                    onStartRoutine(routine.id)
                                } else {
                                    onBuildRoutine(routine.folderId, routine.id)
                                }
                            }
                        },
                        onOpen = {
                            targetRoutine?.let { onBuildRoutine(it.folderId, it.id) }
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // Quick Actions Row: "+ Empty workout" and "↻ Repeat last"
        item(key = "quick-actions") {
            QuickActionsRow(
                canStartEmpty = activeWorkout == null,
                canRepeatLast = canRepeatLast && activeWorkout == null,
                onStartEmpty = onStartEmpty,
                onRepeatLast = onRepeatLast,
            )

            Spacer(Modifier.height(14.dp))
        }

        // Google Sheet Program Section (weekly pre-filled loads + deloads)
        item(key = "program-sheet") {
            ProgramSheetSection(
                state = programSheet,
                canStartWorkout = activeWorkout == null,
                onOpenConfig = onOpenProgramSheetConfig,
                onRefresh = onRefreshProgramSheet,
                onSelectWeek = onSelectProgramSheetWeek,
                onStartDay = onStartProgramSheetDay,
            )

            Spacer(Modifier.height(20.dp))
        }

        // Routines Section Header: "Routines" on left, circular "+" on right
        item(key = "routines-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Routines",
                    color = HevyntTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
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

            Spacer(Modifier.height(12.dp))
        }

        // Folder Accordion Cards
        folders.forEach { folder ->
            val expanded = folder.id in expandedFolderIds
            item(key = "folder-card:${folder.id}") {
                RoutineFolderCard(
                    folder = folder,
                    expanded = expanded,
                    recommendedRoutineId = targetRoutine?.id,
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
                    onReorderRoutines = { orderedIds -> onReorderRoutines(folder.id, orderedIds) },
                )

                Spacer(Modifier.height(10.dp))
            }
        }

        if (folders.isEmpty()) {
            item(key = "empty-routines") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = HevyntSurfaceRaised,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No routines yet",
                            color = HevyntTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Create a folder or import a workout plan to get started.",
                            color = HevyntTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HevyntTonalButton(onClick = onImportPlan) {
                                Text("Import plan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            HevyntPrimaryButton(onClick = { showCreateFolder = true }) {
                                Text("New folder", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
