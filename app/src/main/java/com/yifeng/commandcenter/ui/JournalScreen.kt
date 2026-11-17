package com.yifeng.commandcenter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutJournalEntry
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
            top = 14.dp,
            bottom = 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HevyntPageHeader(
                title = "Journal",
                subtitle = if (filter == null) {
                    "Completed training, newest first."
                } else {
                    DateFormat.getDateInstance(DateFormat.FULL).format(Date(filter.startMillisInclusive))
                },
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        filter?.let {
            item {
                TextButton(onClick = onClearFilter, contentPadding = PaddingValues(0.dp)) {
                    Text("SHOW ALL SESSIONS", fontSize = 14.sp)
                }
            }
        }
        if (visibleEntries.isEmpty()) {
            item { JournalEmptyState(filter != null) }
        } else {
            items(visibleEntries, key = { it.session.id }) { entry ->
                JournalEntryRow(
                    entry = entry,
                    photoFile = entry.photo?.let { photoFiles[it.id] },
                    onClick = { onOpenSession(entry.session.id) },
                )
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
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JournalPhoto(
                photoFile = photoFile,
                modifier = Modifier.size(width = 82.dp, height = 74.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    entry.session.routineName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(entry.session.completedAtMillis)),
                    modifier = Modifier.padding(top = 3.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${entry.session.completedSetCount} sets · ${formatWeight(entry.session.totalVolumeKg)} kg · " +
                        formatHistoryDuration(entry.session.durationMillis),
                    modifier = Modifier.padding(top = 3.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                entry.session.note.takeIf(String::isNotBlank)?.let { note ->
                    Text(
                        note,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.yifeng.commandcenter.R.drawable.ic_arrow_diagonal),
                contentDescription = "Open workout",
                tint = HevyntTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
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