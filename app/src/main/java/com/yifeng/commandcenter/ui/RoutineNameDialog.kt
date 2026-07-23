package com.yifeng.commandcenter.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.ui.theme.DisplayFont

@Composable
fun RoutineNameDialog(
    initialName: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "SAVE ROUTINE" else "NAME YOUR ROUTINE",
                fontFamily = DisplayFont,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Routine name") },
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
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) },
            ) {
                Text(
                    "SAVE",
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