package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntError

@Composable
internal fun FolderPickerDialog(
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
internal fun NameDialog(
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
internal fun DeleteDialog(
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
