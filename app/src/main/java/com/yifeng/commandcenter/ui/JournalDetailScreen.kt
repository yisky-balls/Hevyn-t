package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun JournalDetailScreen(
    graph: WorkoutHistoryGraph,
    photoFile: File?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HevyntPageHorizontalPadding,
            end = HevyntPageHorizontalPadding,
            top = 10.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to journal")
                }
                Text(
                    "SESSION DETAIL",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    letterSpacing = 0.5.sp,
                )
            }
        }
        item {
            JournalPhoto(
                photoFile = photoFile,
                modifier = Modifier.fillMaxWidth().height(250.dp),
            )
        }
        item {
            Text(
                graph.session.routineName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
                    .format(Date(graph.session.completedAtMillis)),
                modifier = Modifier.padding(top = 5.dp),
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            HevyntPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HevyntMetricItem("DURATION", formatHistoryDuration(graph.session.durationMillis))
                    HevyntMetricItem("SETS", graph.session.completedSetCount.toString())
                    HevyntMetricItem("VOLUME", "${formatWeight(graph.session.totalVolumeKg)} kg")
                }
            }
        }
        graph.session.note.takeIf(String::isNotBlank)?.let { note ->
            item {
                HevyntPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("NOTE", color = HevyntTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(note, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    }
                }
            }
        }
        if (graph.session.isLegacySummaryOnly) {
            item {
                Text(
                    "Detailed sets unavailable for workouts saved by an older version",
                    modifier = Modifier.padding(top = 8.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
            }
        } else {
            items(graph.exercises, key = { it.exercise.id }) { exerciseGraph ->
                HevyntPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            exerciseGraph.exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        exerciseGraph.exercise.note.takeIf(String::isNotBlank)?.let { note ->
                            Text(note, modifier = Modifier.padding(top = 4.dp), color = HevyntTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        exerciseGraph.sets.filter { it.completed }.forEach { set ->
                            Row(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    (set.position + 1).toString(),
                                    modifier = Modifier.width(36.dp),
                                    color = HevyntTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    set.weightKg?.let { "${formatWeight(it)} kg" } ?: "—",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                                Text(
                                    set.repetitions?.let { "$it reps" } ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            HevyntOutlineButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 4.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp),
                )
                Text("  DELETE SESSION", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun JournalPhoto(photoFile: File?, modifier: Modifier) {
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.medium).background(HevyntSurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.List,
            contentDescription = null,
            tint = HevyntTextSecondary,
            modifier = Modifier.size(28.dp),
        )
        photoFile?.let {
            AsyncImage(
                model = it,
                contentDescription = "Workout photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
