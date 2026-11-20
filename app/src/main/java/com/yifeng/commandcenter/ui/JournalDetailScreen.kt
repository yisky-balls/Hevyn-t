package com.yifeng.commandcenter.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalDetailScreen(
    graph: WorkoutHistoryGraph,
    photoFile: File?,
    isPhotoBusy: Boolean = false,
    onPhotoSelected: (Uri) -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhotoSelected)
    }
    fun launchPhotoPicker() {
        if (!isPhotoBusy) {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    val (volumeValue, volumeUnit) = if (graph.session.totalVolumeKg >= 1000.0) {
        String.format(Locale.US, "%.1f", graph.session.totalVolumeKg / 1000.0) to "t"
    } else {
        formatWeight(graph.session.totalVolumeKg) to "kg"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HevyntPageHorizontalPadding,
            end = HevyntPageHorizontalPadding,
            top = 10.dp,
            bottom = 124.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to journal",
                        tint = HevyntTextPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Session detail",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                    text = cleanRoutineName(graph.session.routineName).ifBlank { graph.session.routineName }.uppercase(),
                    fontFamily = HevyntDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 0.8.sp,
                    color = HevyntTextPrimary,
                )
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
                        .format(Date(graph.session.completedAtMillis)),
                    modifier = Modifier.padding(top = 6.dp),
                    color = HevyntTextSecondary,
                    fontSize = 14.sp,
                )
            }
        }
        item {
            if (photoFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(HevyntSurfaceRaised)
                        .clickable(enabled = !isPhotoBusy) { launchPhotoPicker() },
                ) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = "Workout photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.68f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(enabled = !isPhotoBusy) { launchPhotoPicker() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (isPhotoBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Change photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Text(
                                    text = "Change photo",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.68f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isPhotoBusy, onClick = onRemovePhoto),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Remove photo",
                                    tint = HevyntError,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HevyntSurfaceRaised,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !isPhotoBusy) { launchPhotoPicker() },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isPhotoBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = HevyntTextPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = HevyntTextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Add session photo",
                            color = HevyntTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        item {
            HevyntPanel(modifier = Modifier.fillMaxWidth()) {
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
        graph.session.note.takeIf(String::isNotBlank)?.let { note ->
            item {
                HevyntPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            text = "Note",
                            color = HevyntTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = note,
                            color = HevyntTextPrimary,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
        if (graph.session.isLegacySummaryOnly) {
            item {
                HevyntPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Detailed sets are unavailable for workouts saved by an older version.",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        color = HevyntTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        } else {
            items(graph.exercises, key = { it.exercise.id }) { exerciseGraph ->
                HevyntPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            text = exerciseGraph.exercise.name,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = HevyntTextPrimary,
                        )
                        exerciseGraph.exercise.note.takeIf(String::isNotBlank)?.let { note ->
                            Text(
                                text = note,
                                modifier = Modifier.padding(top = 4.dp),
                                color = HevyntTextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                        val completedSets = exerciseGraph.sets.filter { it.completed }
                        if (completedSets.isEmpty()) {
                            Text(
                                text = "No completed sets",
                                modifier = Modifier.padding(top = 6.dp),
                                color = HevyntTextSecondary,
                                fontSize = 13.sp,
                            )
                        } else {
                            Spacer(Modifier.height(10.dp))
                            completedSets.forEachIndexed { idx, set ->
                                if (idx > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(HevyntDivider),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = (set.position + 1).toString(),
                                        modifier = Modifier.width(32.dp),
                                        color = HevyntTextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = set.weightKg?.let { "${formatWeight(it)} kg" } ?: "—",
                                        modifier = Modifier.weight(1f),
                                        color = HevyntTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = set.repetitions?.let { "$it reps" } ?: "—",
                                        color = HevyntTextSecondary,
                                        fontSize = 14.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            HevyntTonalButton(
                onClick = onDelete,
                destructive = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = HevyntError,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delete session",
                    color = HevyntError,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
