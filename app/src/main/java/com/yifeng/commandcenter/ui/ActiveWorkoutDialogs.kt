package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun RestDurationDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onStart: ((Int) -> Unit)? = null,
) {
    var seconds by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    val presets = listOf(60, 90, 120, 180, 300)
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "REST TIMER",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(seconds / 60, seconds % 60),
                    fontFamily = HevyntDisplayFont,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                    ),
                )
                Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HevyntOutlineButton(onClick = { seconds = (seconds - 15).coerceAtLeast(15) }) {
                        Text("−15 SEC", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    HevyntOutlineButton(onClick = { seconds = (seconds + 15).coerceAtMost(900) }) {
                        Text("+15 SEC", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        TextButton(
                            onClick = { seconds = preset },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            Text("${preset / 60}:${(preset % 60).toString().padStart(2, '0')}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onStart != null) {
                    TextButton(onClick = { onStart(seconds) }) {
                        Text("START", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { onSave(seconds) }) {
                    Text("SAVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Medium) }
        },
    )
}

@Composable
internal fun GlyphSettingsDialog(
    isTimerEnabled: Boolean,
    isPulseEnabled: Boolean,
    onToggleTimer: (Boolean) -> Unit,
    onTogglePulse: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "NOTHING GLYPH",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone (4a) rear LED rest countdown bar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HevyntTextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show timer on Glyph", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Drains six segments as rest elapses",
                            style = MaterialTheme.typography.labelSmall,
                            color = HevyntTextSecondary,
                        )
                    }
                    Switch(
                        checked = isTimerEnabled,
                        onCheckedChange = onToggleTimer,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Pulse on completion", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Flashes all zones when rest ends",
                            style = MaterialTheme.typography.labelSmall,
                            color = HevyntTextSecondary,
                        )
                    }
                    Switch(
                        checked = isPulseEnabled,
                        onCheckedChange = onTogglePulse,
                        enabled = isTimerEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        },
    )
}

internal fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
