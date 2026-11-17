package com.yifeng.commandcenter.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

private val RoutinePlayCircleBg = Color(0xFF222225)
private val RoutinePlayCircleBorder = Color(0xFF333338)

internal fun cleanRoutineName(name: String): String =
    name.replace(
        Regex("^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY|MON|TUE|WED|THU|FRI|SAT|SUN|DAY\\s*\\d+)\\s*[-—–:.]\\s*", RegexOption.IGNORE_CASE),
        "",
    ).trim()

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
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onContinue),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(HevyntAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = "Continue",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active workout",
                    color = HevyntAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cleanRoutineName(workout.routineName).ifBlank { "Workout in progress" },
                    color = HevyntTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$completedSets sets completed · ${formatLauncherElapsed(nowMillis - workout.startedAtMillis)}",
                    color = HevyntTextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HeroUpNextCard(
    recommendation: RoutineRecommendation?,
    fallbackRoutine: RoutineSummary?,
    onStart: () -> Unit,
    onOpen: () -> Unit,
) {
    val routine = recommendation?.routine ?: fallbackRoutine ?: return
    val cleanName = cleanRoutineName(routine.name).uppercase()
    val lastDone = routine.lastPerformedAtMillis?.let(::formatLastDoneShort) ?: "not performed yet"

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onStart,
                onLongClick = onOpen,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(HevyntAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = "Start up next workout",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Up next",
                    color = HevyntTextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cleanName,
                    fontFamily = HevyntDisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    fontSize = 28.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${routine.exerciseCount} exercises · ${lastDone.lowercase()}",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
            }
        }
    }
}

@Composable
internal fun QuickActionsRow(
    onStartEmpty: () -> Unit,
    onRepeatLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = HevyntSurfaceRaised,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable(onClick = onStartEmpty),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = HevyntTextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Empty workout",
                    color = HevyntTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = HevyntSurfaceRaised,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable(onClick = onRepeatLast),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = HevyntTextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repeat last",
                    color = HevyntTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoutineFolderCard(
    folder: RoutineFolderSummary,
    expanded: Boolean,
    recommendedRoutineId: String?,
    onToggleExpand: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onEditFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onAddRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    onDeleteRoutine: (String) -> Unit,
) {
    var folderMenuExpanded by rememberSaveable(folder.id) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = onToggleExpand,
                            onLongClick = { folderMenuExpanded = true },
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            fontWeight = FontWeight.Bold,
                            color = HevyntTextPrimary,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${folder.routines.size} routine${if (folder.routines.size == 1) "" else "s"}",
                            color = HevyntTextSecondary,
                            fontSize = 13.sp,
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                DropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false },
                    containerColor = HevyntSurfaceInteractive,
                ) {
                    DropdownMenuItem(
                        text = { Text("Add routine") },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        onClick = { folderMenuExpanded = false; onAddRoutine() },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename folder") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { folderMenuExpanded = false; onEditFolder() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete folder", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { folderMenuExpanded = false; onDeleteFolder() },
                    )
                }
            }

            if (expanded) {
                folder.routines.forEach { routine ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HevyntDivider),
                    )

                    val isUpNext = routine.id == recommendedRoutineId
                    val cleanTitle = cleanRoutineName(routine.name)
                    val lastDoneStr = routine.lastPerformedAtMillis?.let(::formatLastDoneShort) ?: "Not performed yet"
                    val subtitle = if (isUpNext) "$lastDoneStr · up next" else lastDoneStr

                    var routineMenuExpanded by rememberSaveable(routine.id) { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenRoutine(routine.id) },
                                    onLongClick = { routineMenuExpanded = true },
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isUpNext) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(HevyntAccent, CircleShape),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = cleanTitle,
                                        fontWeight = FontWeight.Bold,
                                        color = HevyntTextPrimary,
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = subtitle,
                                    color = HevyntTextSecondary,
                                    fontSize = 13.sp,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RoutinePlayCircleBg)
                                    .border(1.dp, RoutinePlayCircleBorder, CircleShape)
                                    .clickable { onStartRoutine(routine.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                                    contentDescription = "Start routine",
                                    tint = HevyntTextPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = routineMenuExpanded,
                            onDismissRequest = { routineMenuExpanded = false },
                            containerColor = HevyntSurfaceInteractive,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit routine") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = { routineMenuExpanded = false; onEditRoutine(routine.id) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete routine", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { routineMenuExpanded = false; onDeleteRoutine(routine.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun formatLastDoneShort(completedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedDays = TimeUnit.MILLISECONDS.toDays((nowMillis - completedAtMillis).coerceAtLeast(0L))
    return when (elapsedDays) {
        0L -> "Last done today"
        1L -> "Last done yesterday"
        else -> "Last done ${elapsedDays}d ago"
    }
}

internal fun formatLauncherElapsed(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
