package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.ui.theme.LiftMuted
import com.yifeng.commandcenter.ui.theme.LiftRed

@Composable
fun WorkoutProgramsScreen(
    folders: List<RoutineFolder>,
    onStartRoutine: (Routine) -> Unit,
    onBuildRoutine: (RoutineFolder, Routine?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (RoutineFolder, String) -> Unit,
    onDeleteFolder: (RoutineFolder) -> Unit,
    onDeleteRoutine: (RoutineFolder, Routine) -> Unit,
    onStartEmpty: () -> Unit,
) {
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<RoutineFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<RoutineFolder?>(null) }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            onDismiss = { showFolderPicker = false },
            onSelect = { folder ->
                showFolderPicker = false
                onBuildRoutine(folder, null)
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
            onDismiss = { folderToRename = null },
            onConfirm = {
                folderToRename = null
                onRenameFolder(folder, it)
            },
        )
    }
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete ${folder.name}?") },
            text = {
                Text(
                    if (folder.routines.isEmpty()) {
                        "This folder will be removed."
                    } else {
                        "This will also remove ${folder.routines.size} routine${if (folder.routines.size == 1) "" else "s"}."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderToDelete = null
                        onDeleteFolder(folder)
                    }
                ) { Text("Delete", color = LiftRed) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Workout", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Choose a plan and get to work.", color = LiftMuted)
            Spacer(Modifier.height(24.dp))
            Card(
                onClick = onStartEmpty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(
                        "Start Empty Workout",
                        modifier = Modifier.padding(start = 14.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Routines",
                    modifier = Modifier.weight(1f),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { showCreateFolder = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Create folder")
                }
            }
            Card(
                onClick = {
                    if (folders.isEmpty()) showCreateFolder = true else showFolderPicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                    Text(
                        "New Routine",
                        modifier = Modifier.padding(start = 12.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${folders.size} FOLDER${if (folders.size == 1) "" else "S"}",
                color = LiftMuted,
                fontSize = 12.sp,
            )
        }

        folders.forEach { folder ->
            val isExpanded = expandedFolders[folder.id] ?: true
            item(key = "header-${folder.id}") {
                FolderHeader(
                    folder = folder,
                    isExpanded = isExpanded,
                    onToggle = { expandedFolders[folder.id] = !isExpanded },
                    onRename = { folderToRename = folder },
                    onDelete = { folderToDelete = folder },
                )
            }
            if (isExpanded) {
                folder.routines.forEach { routine ->
                    item(key = routine.id) {
                        RoutineCard(
                            routine = routine,
                            onStart = { onStartRoutine(routine) },
                            onEdit = { onBuildRoutine(folder, routine) },
                            onDelete = { onDeleteRoutine(folder, routine) },
                        )
                    }
                }
                item(key = "add-${folder.id}") {
                    AddRoutineButton(onClick = { onBuildRoutine(folder, null) })
                }
            }
        }

        if (folders.isEmpty()) {
            item {
                AddRoutineButton(
                    label = "+ Create a folder to add routines",
                    onClick = { showCreateFolder = true },
                )
            }
        }
    }
}

@Composable
private fun FolderHeader(
    folder: RoutineFolder,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
            contentDescription = if (isExpanded) "Collapse folder" else "Expand folder",
            tint = LiftMuted,
        )
        Text(
            if (folder.routines.isEmpty()) folder.name else "${folder.name} (${folder.routines.size})",
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = LiftMuted,
            fontSize = 20.sp,
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "Folder options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Rename folder") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete folder") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun AddRoutineButton(
    label: String = "+ Add new routine",
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    routine.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "Routine options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit routine") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Text(
                routine.exercises.joinToString(", ") { it.name }.ifBlank { "No exercises yet" },
                color = LiftMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp,
            )
            Button(
                onClick = onStart,
                enabled = routine.exercises.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(50.dp),
            ) {
                Text("Start Routine", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FolderPickerDialog(
    folders: List<RoutineFolder>,
    onDismiss: () -> Unit,
    onSelect: (RoutineFolder) -> Unit,
    onCreateFolder: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onSelect(folder) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(folder.name, modifier = Modifier.fillMaxWidth())
                    }
                }
                TextButton(onClick = onCreateFolder, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null)
                    Text("  Create new folder")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Folder name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()) },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}