package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun FolderPickerDialog(
    folders: List<RoutineFolderSummary>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateFolder: () -> Unit,
) {
    HevyntSheet(
        onDismiss = onDismiss,
        title = "Choose a folder",
        subtitle = "Select where to save the new routine",
    ) {
        if (folders.isNotEmpty()) {
            HevyntSheetSection {
                folders.forEachIndexed { index, folder ->
                    if (index > 0) {
                        HevyntDottedRule(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = HevyntOutline.copy(alpha = 0.7f),
                        )
                    }
                    HevyntSheetRow(
                        label = folder.name,
                        subtitle = "${folder.routines.size} routine${if (folder.routines.size == 1) "" else "s"}",
                        onClick = { onSelect(folder.id) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        HevyntSheetSection {
            HevyntSheetRow(
                label = "Create new folder",
                icon = Icons.Outlined.Add,
                onClick = onCreateFolder,
            )
        }
    }
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
    val submit = {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) {
            onConfirm(trimmed)
        }
    }

    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
            )
        },
        text = {
            HevyntTextField(
                value = value,
                onValueChange = { value = it },
                label = "Folder name",
                placeholder = "e.g. Push / Pull / Legs",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = {
            HevyntPrimaryButton(
                enabled = value.isNotBlank(),
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = confirmLabel,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            HevyntTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HevyntTextSecondary,
                )
            }
        },
    )
}

@Composable
internal fun DeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    HevyntConfirmDialog(
        title = title,
        message = message,
        confirmText = "Delete",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
