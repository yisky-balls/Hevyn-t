package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRecommendationReason
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

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

@Composable
private fun ContinueWorkoutCard(workout: ActiveWorkout, onContinue: () -> Unit) {
    var nowMillis by rememberSaveable(workout.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val completedSets = workout.exercises.sumOf { exercise -> exercise.sets.count { it.completed } }
    HevyntPanel(Modifier.fillMaxWidth().clickable(onClick = onContinue)) {
        Column(Modifier.padding(18.dp)) {
            HevyntSectionLabel("CONTINUE WORKOUT", color = HevyntAccent)
            Text(
                workout.routineName.ifBlank { "Active workout" },
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$completedSets sets · ${formatLauncherElapsed(nowMillis - workout.startedAtMillis)}",
                    modifier = Modifier.weight(1f),
                    color = HevyntTextSecondary,
                    fontFamily = HevyntDisplayFont,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HevyntSectionLabel("CONTINUE", color = HevyntAccent)
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HevyntAccent,
                    modifier = Modifier.padding(start = 8.dp).size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun LikelyNextRoutineCard(
    recommendation: RoutineRecommendation,
    onStart: () -> Unit,
    onOpen: () -> Unit,
) {
    val routine = recommendation.routine
    HevyntPanel(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 17.dp, bottom = 10.dp)) {
            HevyntSectionLabel("LIKELY NEXT ROUTINE")
            HevyntSectionLabel(
                if (recommendation.reason == RoutineRecommendationReason.ObservedTransition) {
                    "NEXT IN ROTATION"
                } else {
                    "LEAST RECENTLY TRAINED"
                },
                modifier = Modifier.padding(top = 4.dp),
                color = HevyntAccent,
            )
            Text(
                routine.name,
                modifier = Modifier.padding(top = 10.dp, end = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    routineMetadata(routine),
                    modifier = Modifier.weight(1f),
                    color = HevyntTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = onStart, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    HevyntSectionLabel("START", Modifier.padding(start = 5.dp), HevyntAccent)
                }
            }
        }
    }
}

@Composable
private fun SecondaryActionRow(
    label: String,
    enabled: Boolean,
    icon: ImageVector = Icons.Outlined.Add,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = HevyntSurfaceRaised,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else HevyntTextSecondary,
                modifier = Modifier.size(20.dp),
            )
            HevyntSectionLabel(
                label,
                Modifier.weight(1f).padding(start = 12.dp),
                if (enabled) MaterialTheme.colorScheme.onSurface else HevyntTextSecondary,
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = if (enabled) HevyntTextSecondary else HevyntTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FolderRow(
    folder: RoutineFolderSummary,
    expanded: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(folder.id) { mutableStateOf(false) }
    HevyntPanel(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 13.dp, vertical = 15.dp)) {
                Text(
                    folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HevyntSectionLabel(
                    formatRoutineCount(folder.routines.size).uppercase(),
                    modifier = Modifier.padding(top = 4.dp),
                    color = HevyntTextSecondary,
                )
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Close folder" else "Open folder",
                modifier = Modifier.size(23.dp),
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Folder options")
                }
                DropdownMenu(menuExpanded, { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("RENAME FOLDER") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("DELETE FOLDER") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    startEnabled: Boolean,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(routine.id) { mutableStateOf(false) }
    HevyntPanel(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 15.dp, bottom = 7.dp)) {
            Text(
                routine.name,
                modifier = Modifier.padding(end = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                routineMetadata(routine),
                modifier = Modifier.padding(top = 5.dp, end = 10.dp),
                color = HevyntTextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = if (routine.exerciseCount == 0) onOpen else onStart,
                    enabled = routine.exerciseCount == 0 || startEnabled,
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    HevyntSectionLabel(
                        if (routine.exerciseCount == 0) "EDIT" else "START",
                        modifier = Modifier.padding(start = 5.dp),
                        color = if (routine.exerciseCount == 0 || startEnabled) {
                            HevyntAccent
                        } else {
                            HevyntTextSecondary
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Routine options")
                    }
                    DropdownMenu(menuExpanded, { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("EDIT ROUTINE") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("DELETE ROUTINE") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerDialog(
    folders: List<RoutineFolderSummary>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateFolder: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = { Text("CHOOSE A FOLDER", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onSelect(folder.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(folder.name, modifier = Modifier.fillMaxWidth(), maxLines = 1)
                    }
                    HevyntDottedRule()
                }
                TextButton(onClick = onCreateFolder, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null)
                    Text("CREATE NEW FOLDER", Modifier.padding(start = 10.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun NameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Folder name") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = hevyntTextFieldColors(),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
                Text(confirmLabel.uppercase(), color = HevyntAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun DeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("DELETE", color = HevyntError, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

private fun routineMetadata(routine: RoutineSummary): String =
    "${routine.exerciseCount} exercise${if (routine.exerciseCount == 1) "" else "s"} · " +
        (routine.lastPerformedAtMillis?.let(::formatLastPerformed) ?: "not performed yet")

internal fun formatLastPerformed(completedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedDays = TimeUnit.MILLISECONDS.toDays((nowMillis - completedAtMillis).coerceAtLeast(0L))
    return when (elapsedDays) {
        0L -> "last done today"
        1L -> "last done yesterday"
        else -> "last done ${elapsedDays}d ago"
    }
}

private fun formatRoutineCount(count: Int) = "$count routine${if (count == 1) "" else "s"}"

private fun formatLauncherElapsed(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}