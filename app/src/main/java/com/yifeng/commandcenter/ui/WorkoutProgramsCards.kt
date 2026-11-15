package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRecommendationReason
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@Composable
internal fun ContinueWorkoutCard(workout: ActiveWorkout, onContinue: () -> Unit) {
    var nowMillis by rememberSaveable(workout.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val completedSets = workout.exercises.sumOf { exercise -> exercise.sets.count { it.completed } }
    HevyntPanel(Modifier.fillMaxWidth().clickable(onClick = onContinue)) {
        Column(Modifier.padding(18.dp)) {
            HevyntSectionLabel("CONTINUE WORKOUT", color = HevyntAccent)
            Text(
                workout.routineName.ifBlank { "Active workout" },
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$completedSets sets · ${formatLauncherElapsed(nowMillis - workout.startedAtMillis)}",
                    modifier = Modifier.weight(1f),
                    color = HevyntTextSecondary,
                    fontFamily = HevyntDisplayFont,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HevyntSectionLabel("CONTINUE", color = HevyntAccent)
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HevyntAccent,
                    modifier = Modifier.padding(start = 8.dp).size(19.dp),
                )
            }
        }
    }
}

@Composable
internal fun LikelyNextRoutineCard(
    recommendation: RoutineRecommendation,
    onStart: () -> Unit,
    onOpen: () -> Unit,
) {
    val routine = recommendation.routine
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(HevyntAccent, CircleShape)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = "Start",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    color = HevyntTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(3.dp))
                Text(
                    text = routineMetadata(routine),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_diagonal),
                contentDescription = null,
                tint = HevyntTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun SecondaryActionRow(
    label: String,
    enabled: Boolean,
    icon: ImageVector = Icons.Outlined.Add,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = HevyntSurfaceRaised,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else HevyntTextSecondary,
                modifier = Modifier.size(20.dp),
            )
            HevyntSectionLabel(
                label,
                Modifier.weight(1f).padding(start = 12.dp),
                if (enabled) MaterialTheme.colorScheme.onSurface else HevyntTextSecondary,
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = if (enabled) HevyntTextSecondary else HevyntTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun FolderRow(
    folder: RoutineFolderSummary,
    expanded: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(folder.id) { mutableStateOf(false) }
    HevyntPanel(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 13.dp, vertical = 15.dp)) {
                Text(
                    folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HevyntSectionLabel(
                    formatRoutineCount(folder.routines.size).uppercase(),
                    modifier = Modifier.padding(top = 4.dp),
                    color = HevyntTextSecondary,
                )
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Close folder" else "Open folder",
                modifier = Modifier.size(23.dp),
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Folder options")
                }
                DropdownMenu(menuExpanded, { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("RENAME FOLDER") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("DELETE FOLDER") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
internal fun RoutineCard(
    routine: RoutineSummary,
    startEnabled: Boolean,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(routine.id) { mutableStateOf(false) }
    HevyntPanel(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 15.dp, bottom = 7.dp)) {
            Text(
                routine.name,
                modifier = Modifier.padding(end = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                routineMetadata(routine),
                modifier = Modifier.padding(top = 5.dp, end = 10.dp),
                color = HevyntTextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = if (routine.exerciseCount == 0) onOpen else onStart,
                    enabled = routine.exerciseCount == 0 || startEnabled,
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    HevyntSectionLabel(
                        if (routine.exerciseCount == 0) "EDIT" else "START",
                        modifier = Modifier.padding(start = 5.dp),
                        color = if (routine.exerciseCount == 0 || startEnabled) {
                            HevyntAccent
                        } else {
                            HevyntTextSecondary
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Routine options")
                    }
                    DropdownMenu(menuExpanded, { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("EDIT ROUTINE") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("DELETE ROUTINE") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

internal fun routineMetadata(routine: RoutineSummary): String =
    "${routine.exerciseCount} exercise${if (routine.exerciseCount == 1) "" else "s"} · " +
        (routine.lastPerformedAtMillis?.let(::formatLastPerformed) ?: "not performed yet")

internal fun formatLastPerformed(completedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedDays = TimeUnit.MILLISECONDS.toDays((nowMillis - completedAtMillis).coerceAtLeast(0L))
    return when (elapsedDays) {
        0L -> "last done today"
        1L -> "last done yesterday"
        else -> "last done ${elapsedDays}d ago"
    }
}

internal fun formatRoutineCount(count: Int) = "$count routine${if (count == 1) "" else "s"}"

internal fun formatLauncherElapsed(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
