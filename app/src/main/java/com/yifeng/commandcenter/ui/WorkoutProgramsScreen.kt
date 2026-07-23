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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftBorder
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
            title = {
                Text(
                    "DELETE ${folder.name.uppercase()}?",
                    fontFamily = DisplayFont,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                )
            },
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
                ) {
                    Text(
                        "DELETE",
                        color = LiftRed,
                        fontFamily = DisplayFont,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("CANCEL", fontFamily = DisplayFont)
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LiftPageHorizontalPadding,
            end = LiftPageHorizontalPadding,
            top = 20.dp,
            bottom = 28.dp,
        ),
    ) {
        item {
            LiftPageHeader(
                title = "WORKOUT",
                subtitle = "Choose a plan and get to work.",
            )
            Spacer(Modifier.height(20.dp))
            DottedDivider()
            PrimaryActionRow(
                label = "START EMPTY WORKOUT",
                onClick = onStartEmpty,
            )
            DottedDivider()
        }

        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ROUTINES",
                    modifier = Modifier.weight(1f),
                    fontFamily = DisplayFont,
                    fontSize = 26.sp,
                    letterSpacing = 1.2.sp,
                )
                LiftSectionLabel(
                    text = "${folders.size} FOLDER${if (folders.size == 1) "" else "S"}",
                    color = LiftMuted,
                )
                IconButton(onClick = { showCreateFolder = true }) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = "Create folder")
                }
            }
            DottedDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .clickable {
                    if (folders.isEmpty()) showCreateFolder = true else showFolderPicker = true
                }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(21.dp))
                LiftSectionLabel("NEW ROUTINE", Modifier.padding(start = 14.dp))
            }
            DottedDivider()
            Spacer(Modifier.height(16.dp))
        }

        folders.forEach { folder ->
            val isExpanded = expandedFolders[folder.id] ?: true
            item(key = folder.id) {
                FolderGroup(
                    folder = folder,
                    isExpanded = isExpanded,
                    onToggle = { expandedFolders[folder.id] = !isExpanded },
                    onRename = { folderToRename = folder },
                    onDelete = { folderToDelete = folder },
                    onAddRoutine = { onBuildRoutine(folder, null) },
                    onStartRoutine = onStartRoutine,
                    onEditRoutine = { onBuildRoutine(folder, it) },
                    onDeleteRoutine = { onDeleteRoutine(folder, it) },
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        if (folders.isEmpty()) {
            item {
                Text(
                    "NO ROUTINES YET. CREATE A FOLDER TO BEGIN.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateFolder = true }
                        .padding(vertical = 24.dp),
                    color = LiftMuted,
                    fontFamily = DisplayFont,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            label,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            fontFamily = DisplayFont,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
        )
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(25.dp),
        )
    }
}

@Composable
private fun FolderGroup(
    folder: RoutineFolder,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddRoutine: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onEditRoutine: (Routine) -> Unit,
    onDeleteRoutine: (Routine) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, LiftBorder),
        shape = MaterialTheme.shapes.large,
    ) {
        FolderHeader(folder, isExpanded, onToggle, onRename, onDelete)
        if (isExpanded) {
            DottedDivider(Modifier.padding(horizontal = 12.dp))
            folder.routines.forEach { routine ->
                RoutineRow(
                    routine = routine,
                    onStart = { onStartRoutine(routine) },
                    onEdit = { onEditRoutine(routine) },
                    onDelete = { onDeleteRoutine(routine) },
                )
                DottedDivider(Modifier.padding(horizontal = 12.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable(onClick = onAddRoutine)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                LiftSectionLabel("ADD ROUTINE", Modifier.padding(start = 13.dp), LiftMuted)
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
            .height(66.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
            contentDescription = if (isExpanded) "Collapse folder" else "Expand folder",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Text(
            folder.name.uppercase(),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            fontFamily = DisplayFont,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
        LiftSectionLabel(
            text = "${folder.routines.size} ROUTINE${if (folder.routines.size == 1) "" else "S"}",
            color = LiftMuted,
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Folder options")
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
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
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
private fun RoutineRow(
    routine: Routine,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = routine.exercises.isNotEmpty(), onClick = onStart)
            .padding(start = 18.dp, end = 10.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Build,
            contentDescription = null,
            tint = LiftMuted,
            modifier = Modifier.size(23.dp),
        )
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                routine.name.uppercase(),
                fontSize = 18.sp,
                fontFamily = DisplayFont,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                routine.exercises.joinToString("  ·  ") { it.name }.ifBlank { "No exercises yet" },
                modifier = Modifier.padding(top = 4.dp),
                color = LiftMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
            )
        }
        if (routine.exercises.isNotEmpty()) {
            LiftSectionLabel("START", color = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Routine options")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
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
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
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
private fun FolderPickerDialog(
    folders: List<RoutineFolder>,
    onDismiss: () -> Unit,
    onSelect: (RoutineFolder) -> Unit,
    onCreateFolder: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "CHOOSE A FOLDER",
                fontFamily = DisplayFont,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
        },
        text = {
            Column {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onSelect(folder) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            folder.name.uppercase(),
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = DisplayFont,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    DottedDivider()
                }
                TextButton(
                    onClick = onCreateFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        Icons.Outlined.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "CREATE NEW FOLDER",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        fontFamily = DisplayFont,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = DisplayFont) }
        },
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
        title = {
            Text(
                title.uppercase(),
                fontFamily = DisplayFont,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Folder name") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DisplayFont),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()) },
            ) {
                Text(
                    confirmLabel.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = DisplayFont,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = DisplayFont) }
        },
    )
}