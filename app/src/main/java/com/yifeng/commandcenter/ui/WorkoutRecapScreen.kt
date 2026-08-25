package com.yifeng.commandcenter.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun WorkoutRecapScreen(
    graph: WorkoutHistoryGraph,
    photoFile: File?,
    isPhotoBusy: Boolean,
    errorMessage: String?,
    onPhotoSelected: (android.net.Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onDone: (String) -> Unit,
) {
    var note by remember(graph.session.id) { mutableStateOf(graph.session.note) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhotoSelected)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = HevyntPageHorizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HevyntPageHeader("WORKOUT SAVED", "Your completed sets are safely stored.")
        }
        item { HevyntDottedRule() }
        item {
            Text(
                graph.session.routineName,
                fontFamily = HevyntDisplayFont,
                fontSize = 28.sp,
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(graph.session.completedAtMillis)),
                color = HevyntTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RecapMetric("DURATION", formatHistoryDuration(graph.session.durationMillis))
                RecapMetric("SETS", graph.session.completedSetCount.toString())
                RecapMetric("VOLUME", "${formatWeight(graph.session.totalVolumeKg)} kg")
            }
        }
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Session note") },
                minLines = 3,
                colors = hevyntTextFieldColors(),
            )
        }
        item {
            if (photoFile != null) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Workout photo",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HevyntOutlineButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isPhotoBusy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Replace photo") }
                    HevyntOutlineButton(
                        onClick = onRemovePhoto,
                        enabled = !isPhotoBusy,
                    ) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                HevyntOutlineButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isPhotoBusy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                    Text(if (isPhotoBusy) "  Saving photo…" else "  Add workout photo")
                }
            }
        }
        errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        }
        item {
            HevyntPrimaryButton(
                onClick = { onDone(note.trim()) },
                enabled = !isPhotoBusy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Done", fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun RecapMetric(label: String, value: String) {
    Column {
        Text(label, color = HevyntTextSecondary, fontSize = 12.sp)
        Text(value, fontFamily = HevyntDisplayFont, fontSize = 20.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

internal fun formatHistoryDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}