package com.yifeng.commandcenter.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.cleanRoutineName as stripRoutineDayPrefix
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

internal fun cleanRoutineName(name: String): String = stripRoutineDayPrefix(name)

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
    canStartEmpty: Boolean = true,
    canRepeatLast: Boolean = true,
    onStartEmpty: () -> Unit,
    onRepeatLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val emptyContentColor = if (canStartEmpty) HevyntTextPrimary else HevyntTextSecondary.copy(alpha = 0.5f)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = HevyntSurfaceRaised,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable(enabled = canStartEmpty, onClick = onStartEmpty),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = emptyContentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Empty workout",
                    color = emptyContentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }

        val repeatContentColor = if (canRepeatLast) HevyntTextPrimary else HevyntTextSecondary.copy(alpha = 0.5f)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = HevyntSurfaceRaised,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable(enabled = canRepeatLast, onClick = onRepeatLast),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = repeatContentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repeat last",
                    color = repeatContentColor,
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
    onReorderRoutines: (List<String>) -> Unit = {},
) {
    var folderMenuExpanded by rememberSaveable(folder.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var localRoutines by remember(folder.id) { mutableStateOf(folder.routines) }
    var draggingRoutineId by remember(folder.id) { mutableStateOf<String?>(null) }
    var dragOffsetY by remember(folder.id) { mutableFloatStateOf(0f) }
    val itemHeightsPx = remember(folder.id) { mutableStateMapOf<String, Int>() }

    LaunchedEffect(folder.routines, draggingRoutineId) {
        if (draggingRoutineId == null) {
            localRoutines = folder.routines
        }
    }

    if (folderMenuExpanded) {
        val countLabel = "${folder.routines.size} routine${if (folder.routines.size == 1) "" else "s"}"
        HevyntActionSheet(
            title = folder.name,
            subtitle = countLabel,
            actions = listOf(
                HevyntSheetAction(
                    label = "Add routine",
                    icon = Icons.Outlined.Add,
                    onClick = onAddRoutine,
                ),
                HevyntSheetAction(
                    label = "Rename folder",
                    icon = Icons.Outlined.Edit,
                    onClick = onEditFolder,
                ),
                HevyntSheetAction(
                    label = "Delete folder",
                    icon = Icons.Outlined.Delete,
                    destructive = true,
                    onClick = onDeleteFolder,
                ),
            ),
            onDismiss = { folderMenuExpanded = false },
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            if (expanded) {
                localRoutines.forEach { routine ->
                    val isDragging = draggingRoutineId == routine.id
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

                    if (routineMenuExpanded) {
                        HevyntActionSheet(
                            title = cleanTitle,
                            subtitle = "${routine.exerciseCount} exercise${if (routine.exerciseCount == 1) "" else "s"}",
                            actions = listOf(
                                HevyntSheetAction(
                                    label = "Edit routine",
                                    icon = Icons.Outlined.Edit,
                                    onClick = { onEditRoutine(routine.id) },
                                ),
                                HevyntSheetAction(
                                    label = "Delete routine",
                                    icon = Icons.Outlined.Delete,
                                    destructive = true,
                                    onClick = { onDeleteRoutine(routine.id) },
                                ),
                            ),
                            onDismiss = { routineMenuExpanded = false },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .onSizeChanged { size ->
                                if (size.height > 0) itemHeightsPx[routine.id] = size.height
                            }
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffsetY
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                    shadowElevation = 12f
                                }
                            }
                            .background(
                                color = if (isDragging) HevyntSurfaceInteractive else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .pointerInput(folder.id, routine.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (localRoutines.size < 2) return@detectDragGesturesAfterLongPress
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingRoutineId = routine.id
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (draggingRoutineId != routine.id) return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val currentIndex = localRoutines.indexOfFirst { it.id == routine.id }
                                        if (currentIndex < 0) return@detectDragGesturesAfterLongPress

                                        if (dragOffsetY > 0f && currentIndex < localRoutines.lastIndex) {
                                            val nextItem = localRoutines[currentIndex + 1]
                                            val threshold = (itemHeightsPx[nextItem.id] ?: 160) * 0.55f
                                            if (dragOffsetY >= threshold) {
                                                val mutable = localRoutines.toMutableList()
                                                val moved = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex + 1, moved)
                                                localRoutines = mutable
                                                dragOffsetY -= (itemHeightsPx[nextItem.id] ?: 160).toFloat()
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else if (dragOffsetY < 0f && currentIndex > 0) {
                                            val prevItem = localRoutines[currentIndex - 1]
                                            val threshold = (itemHeightsPx[prevItem.id] ?: 160) * 0.55f
                                            if (-dragOffsetY >= threshold) {
                                                val mutable = localRoutines.toMutableList()
                                                val moved = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex - 1, moved)
                                                localRoutines = mutable
                                                dragOffsetY += (itemHeightsPx[prevItem.id] ?: 160).toFloat()
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggingRoutineId == routine.id) {
                                            val orderedIds = localRoutines.map { it.id }
                                            draggingRoutineId = null
                                            dragOffsetY = 0f
                                            if (orderedIds != folder.routines.map { it.id }) {
                                                onReorderRoutines(orderedIds)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        draggingRoutineId = null
                                        dragOffsetY = 0f
                                        localRoutines = folder.routines
                                    },
                                )
                            }
                            .clickable(enabled = draggingRoutineId == null) { onOpenRoutine(routine.id) }
                            .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
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

                        IconButton(
                            onClick = { routineMenuExpanded = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Routine options",
                                tint = HevyntTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RoutinePlayCircleBg)
                                .border(1.dp, RoutinePlayCircleBorder, CircleShape)
                                .clickable {
                                    if (routine.exerciseCount > 0) {
                                        onStartRoutine(routine.id)
                                    } else {
                                        onOpenRoutine(routine.id)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow_filled),
                                contentDescription = if (routine.exerciseCount > 0) "Start routine" else "Edit routine",
                                tint = if (routine.exerciseCount > 0) HevyntTextPrimary else HevyntTextSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun formatLastDoneShort(completedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedDays = WorkoutCalendar.daysBetweenLocalDates(completedAtMillis, nowMillis)
    return when (elapsedDays) {
        0 -> "Last done today"
        1 -> "Last done yesterday"
        else -> "Last done ${elapsedDays}d ago"
    }
}

internal fun formatLauncherElapsed(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
