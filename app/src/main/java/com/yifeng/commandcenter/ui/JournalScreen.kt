package com.yifeng.commandcenter.ui

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutJournalEntry
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun JournalScreen(
    entries: List<WorkoutJournalEntry>,
    photoFiles: Map<String, File>,
    filter: WorkoutDateRange?,
    listState: LazyListState,
    onClearFilter: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val visibleEntries = remember(entries, filter) {
        filter?.let { range ->
            entries.filter { it.session.completedAtMillis in range }
        } ?: entries
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HevyntPageHorizontalPadding,
            end = HevyntPageHorizontalPadding,
            top = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HevyntPageHeader(
                title = "JOURNAL",
                subtitle = if (filter == null) {
                    "Completed training, newest first."
                } else {
                    DateFormat.getDateInstance(DateFormat.FULL).format(Date(filter.startMillisInclusive))
                },
            )
        }
        filter?.let {
            item {
                TextButton(onClick = onClearFilter, contentPadding = PaddingValues(0.dp)) {
                    Text("Show all sessions", fontSize = 14.sp)
                }
            }
        }
        item { HevyntDottedRule() }
        if (visibleEntries.isEmpty()) {
            item { JournalEmptyState(filter != null) }
        } else {
            items(visibleEntries, key = { it.session.id }) { entry ->
                JournalEntryRow(
                    entry = entry,
                    photoFile = entry.photo?.let { photoFiles[it.id] },
                    onClick = { onOpenSession(entry.session.id) },
                )
                HevyntDottedRule(Modifier.padding(top = 14.dp))
            }
        }
    }
}

@Composable
private fun JournalEntryRow(
    entry: WorkoutJournalEntry,
    photoFile: File?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JournalPhoto(
            photoFile = photoFile,
            modifier = Modifier.size(width = 92.dp, height = 82.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                entry.session.routineName,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(entry.session.completedAtMillis)),
                modifier = Modifier.padding(top = 4.dp),
                color = HevyntTextSecondary,
                fontSize = 13.sp,
            )
            Text(
                "${entry.session.completedSetCount} sets · ${formatWeight(entry.session.totalVolumeKg)} kg · " +
                    formatHistoryDuration(entry.session.durationMillis),
                modifier = Modifier.padding(top = 4.dp),
                color = HevyntTextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
            )
            entry.session.note.takeIf(String::isNotBlank)?.let { note ->
                Text(
                    note,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Open workout",
            tint = HevyntTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

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
                Text("SESSION DETAIL", fontFamily = HevyntDisplayFont, fontSize = 24.sp, letterSpacing = 1.sp)
            }
        }
        item {
            JournalPhoto(
                photoFile = photoFile,
                modifier = Modifier.fillMaxWidth().height(250.dp),
            )
        }
        item {
            Text(graph.session.routineName, fontFamily = HevyntDisplayFont, fontSize = 28.sp)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
                    .format(Date(graph.session.completedAtMillis)),
                modifier = Modifier.padding(top = 5.dp),
                color = HevyntTextSecondary,
                fontSize = 14.sp,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                JournalMetric("DURATION", formatHistoryDuration(graph.session.durationMillis))
                JournalMetric("SETS", graph.session.completedSetCount.toString())
                JournalMetric("VOLUME", "${formatWeight(graph.session.totalVolumeKg)} kg")
            }
        }
        graph.session.note.takeIf(String::isNotBlank)?.let { note ->
            item {
                HevyntDottedRule()
                Text("NOTE", modifier = Modifier.padding(top = 14.dp), color = HevyntTextSecondary, fontSize = 12.sp)
                Text(note, modifier = Modifier.padding(top = 6.dp), fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
        if (graph.session.isLegacySummaryOnly) {
            item {
                HevyntDottedRule()
                Text(
                    "Detailed sets unavailable for workouts saved by an older version",
                    modifier = Modifier.padding(top = 14.dp),
                    color = HevyntTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        } else {
            items(graph.exercises, key = { it.exercise.id }) { exerciseGraph ->
                HevyntDottedRule()
                Column(Modifier.padding(top = 14.dp)) {
                    Text(exerciseGraph.exercise.name, fontSize = 17.sp)
                    exerciseGraph.exercise.note.takeIf(String::isNotBlank)?.let { note ->
                        Text(note, modifier = Modifier.padding(top = 4.dp), color = HevyntTextSecondary, fontSize = 14.sp)
                    }
                    exerciseGraph.sets.filter { it.completed }.forEach { set ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                (set.position + 1).toString(),
                                modifier = Modifier.width(38.dp),
                                color = HevyntTextSecondary,
                                fontSize = 14.sp,
                            )
                            Text(
                                set.weightKg?.let { "${formatWeight(it)} kg" } ?: "—",
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                            )
                            Text(
                                set.repetitions?.let { "$it reps" } ?: "—",
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
        item {
            HevyntDottedRule()
            HevyntOutlineButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 10.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp),
                )
                Text("  Delete session", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun JournalPhoto(photoFile: File?, modifier: Modifier) {
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

@Composable
private fun JournalMetric(label: String, value: String) {
    Column {
        Text(value, fontFamily = HevyntDisplayFont, fontSize = 20.sp)
        Text(label, modifier = Modifier.padding(top = 3.dp), color = HevyntTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun JournalEmptyState(filtered: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.List,
            contentDescription = null,
            tint = HevyntTextSecondary,
            modifier = Modifier.size(38.dp),
        )
        Text(
            if (filtered) "No workouts on this date" else "No completed workouts yet",
            modifier = Modifier.padding(top = 14.dp),
            fontSize = 16.sp,
        )
        Text(
            if (filtered) "Clear the date filter to see your full journal." else "Completed sessions will appear here.",
            modifier = Modifier.padding(top = 5.dp),
            color = HevyntTextSecondary,
            fontSize = 14.sp,
        )
    }
}