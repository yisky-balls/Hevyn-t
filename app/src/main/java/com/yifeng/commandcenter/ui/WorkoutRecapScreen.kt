package com.yifeng.commandcenter.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
    BackHandler(enabled = !isPhotoBusy) { onDone(note.trim()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhotoSelected)
    }
    val (volumeValue, volumeUnit) = if (graph.session.totalVolumeKg >= 1000.0) {
        String.format(Locale.US, "%.1f", graph.session.totalVolumeKg / 1000.0) to "t"
    } else {
        formatWeight(graph.session.totalVolumeKg) to "kg"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = HevyntPageHorizontalPadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(color = HevyntAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Workout saved",
                        color = HevyntAccent,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cleanRoutineName(graph.session.routineName).ifBlank { graph.session.routineName }.uppercase(),
                    fontFamily = HevyntDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 0.8.sp,
                    color = HevyntTextPrimary,
                )
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(graph.session.completedAtMillis)),
                    color = HevyntTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = HevyntSurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HevyntMetricItem(
                        label = "Duration",
                        value = formatHistoryDuration(graph.session.durationMillis),
                        valueFontSize = 28.sp,
                    )
                    HevyntMetricItem(
                        label = "Sets",
                        value = graph.session.completedSetCount.toString(),
                        valueFontSize = 28.sp,
                    )
                    HevyntMetricItem(
                        label = "Volume",
                        value = volumeValue,
                        unit = volumeUnit,
                        valueFontSize = 28.sp,
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = HevyntSurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    HevyntTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Session note",
                        placeholder = "How did the workout feel?",
                        singleLine = false,
                        minHeight = 96.dp,
                        maxLines = 5,
                    )
                }
            }
        }
        item {
            if (photoFile != null) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Workout photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HevyntTonalButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isPhotoBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Replace photo", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HevyntTonalButton(
                        onClick = onRemovePhoto,
                        enabled = !isPhotoBusy,
                        destructive = true,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Remove photo",
                            tint = HevyntError,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            } else {
                HevyntTonalButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isPhotoBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPhotoBusy) "Saving photo…" else "Add workout photo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = HevyntError,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            HevyntPrimaryButton(
                onClick = { onDone(note.trim()) },
                enabled = !isPhotoBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Done", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

internal fun formatHistoryDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
