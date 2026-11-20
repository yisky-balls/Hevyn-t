package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
fun RoutineNameDialog(
    initialName: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val submit = {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            onSave(trimmed)
        }
    }

    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Save routine" else "Name your routine",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
            )
        },
        text = {
            HevyntTextField(
                value = name,
                onValueChange = { name = it },
                label = "Routine name",
                placeholder = "e.g. Upper A",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = {
            HevyntPrimaryButton(
                enabled = name.isNotBlank(),
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Save",
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
