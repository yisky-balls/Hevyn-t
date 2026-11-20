package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutJournalEntry
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            bottom = 124.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HevyntPageHeader(
                title = "Journal",
                subtitle = when {
                    filter == null -> "Completed training, newest first"
                    filter.isMultiDay -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(Date(filter.startMillisInclusive))
                    else -> DateFormat.getDateInstance(DateFormat.FULL)
                        .format(Date(filter.startMillisInclusive))
                },
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        filter?.let {
            item {
                Surface(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(50),
                    color = HevyntAccentContainer,
                    border = BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Show all sessions",
                            color = HevyntAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear filter",
                            tint = HevyntAccent,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
        if (visibleEntries.isEmpty()) {
            item { JournalEmptyState(filtered = filter != null, isMultiDay = filter?.isMultiDay == true) }
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
    val date = remember(entry.session.completedAtMillis) { Date(entry.session.completedAtMillis) }
    val dayNumber = remember(entry.session.completedAtMillis) {
        SimpleDateFormat("d", Locale.getDefault()).format(date)
    }
    val monthShort = remember(entry.session.completedAtMillis) {
        SimpleDateFormat("MMM", Locale.getDefault()).format(date)
    }
    val timeText = remember(entry.session.completedAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (photoFile != null) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Workout photo",
                    modifier = Modifier
                        .size(width = 64.dp, height = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HevyntSurfaceInteractive),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HevyntSurfaceInteractive),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayNumber,
                            fontFamily = HevyntDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = HevyntTextPrimary,
                        )
                        Text(
                            text = monthShort,
                            fontSize = 11.5.sp,
                            color = HevyntTextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = cleanRoutineName(entry.session.routineName).ifBlank { entry.session.routineName },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeText,
                    modifier = Modifier.padding(top = 2.dp),
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                )
                Text(
                    text = "${entry.session.completedSetCount} set${if (entry.session.completedSetCount == 1) "" else "s"} · " +
                        "${formatWeight(entry.session.totalVolumeKg)} kg · " +
                        formatHistoryDuration(entry.session.durationMillis),
                    modifier = Modifier.padding(top = 3.dp),
                    color = HevyntTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.session.note.takeIf(String::isNotBlank)?.let { note ->
                    Text(
                        text = note,
                        modifier = Modifier.padding(top = 4.dp),
                        color = HevyntTextPrimary.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_diagonal),
                contentDescription = "Open workout",
                tint = HevyntTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun JournalEmptyState(filtered: Boolean, isMultiDay: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(HevyntSurfaceInteractive, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = when {
                    !filtered -> "No completed workouts yet"
                    isMultiDay -> "No workouts in this period"
                    else -> "No workouts on this date"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HevyntTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (filtered) "Clear the filter to see your full journal." else "Completed sessions will appear here.",
                color = HevyntTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
