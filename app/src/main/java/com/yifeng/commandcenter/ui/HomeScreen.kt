package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutWeekDay
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    routines: List<RoutineSummary>,
    workoutHistory: List<WorkoutHistorySession>,
    likelyNext: RoutineRecommendation? = null,
    scrollState: ScrollState = rememberScrollState(),
    onOpenPrograms: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
    onRepeatLastWorkout: () -> Unit = {},
) {
    val routine = likelyNext?.routine ?: routines.firstOrNull()
    val todayMillis = remember { System.currentTimeMillis() }
    val todayDateString = remember(todayMillis) {
        SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(Date(todayMillis))
    }

    val weekDays = remember(todayMillis, workoutHistory) {
        WorkoutCalendar.currentWeek(
            nowMillis = todayMillis,
            completedAtMillis = workoutHistory.map { it.completedAtMillis },
        )
    }

    val monthlyAggregate = remember(todayMillis, workoutHistory) {
        val monthRange = WorkoutCalendar.monthRange(
            year = Calendar.getInstance().get(Calendar.YEAR),
            month = Calendar.getInstance().get(Calendar.MONTH),
        )
        WorkoutCalendar.aggregate(workoutHistory, monthRange)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Top App Bar
        HomeTopBar(
            dateText = todayDateString,
            onMenuClick = onOpenPrograms,
            onTuneClick = onOpenPrograms,
        )

        Spacer(Modifier.height(18.dp))

        // Hero Dot-Matrix Routine Header
        HomeHeroHeader(
            routine = routine,
        )

        Spacer(Modifier.height(20.dp))

        // Card 1: Start Workout Card
        StartWorkoutCard(
            routine = routine,
            onStart = { routine?.id?.let(onStartRoutine) ?: onOpenPrograms() },
        )

        Spacer(Modifier.height(14.dp))

        // Card 2: This Week Card
        ThisWeekCard(
            weekDays = weekDays,
            onOpenPrograms = onOpenPrograms,
            onOpenDay = onOpenDay,
        )

        Spacer(Modifier.height(14.dp))

        // Card 3: This Month Card
        ThisMonthCard(
            aggregate = monthlyAggregate,
            onOpenPrograms = onOpenPrograms,
        )

        // Card 4: Last Session Card (if history exists)
        workoutHistory.firstOrNull()?.let { lastSession ->
            Spacer(Modifier.height(14.dp))
            LastSessionCard(
                session = lastSession,
                onClick = { onOpenSession(lastSession.id) },
                onRepeat = onRepeatLastWorkout,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeTopBar(
    dateText: String,
    onMenuClick: () -> Unit,
    onTuneClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Outlined.Menu,
                contentDescription = "Menu",
                tint = HevyntTextPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = HevyntTextPrimary,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onTuneClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = "Settings",
                tint = HevyntTextPrimary,
            )
        }
    }
}

@Composable
private fun HomeHeroHeader(
    routine: RoutineSummary?,
) {
    val rawName = routine?.name ?: "START WORKOUT"
    val routineName = cleanRoutineName(rawName).uppercase(Locale.getDefault())
    val lastDoneText = formatLastDone(routine?.lastPerformedAtMillis)
    val exerciseCount = routine?.exerciseCount ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = routineName,
            fontFamily = HevyntDisplayFont,
            fontSize = 42.sp,
            fontWeight = FontWeight.Normal,
            color = HevyntTextPrimary,
            letterSpacing = 2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Next workout · $exerciseCount exercise${if (exerciseCount == 1) "" else "s"} · $lastDoneText",
            color = HevyntTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun StartWorkoutCard(
    routine: RoutineSummary?,
    onStart: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStart),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(HevyntAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = "Start",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start workout",
                    color = HevyntTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${cleanRoutineName(routine?.name ?: "Routine")} · ${routine?.exerciseCount ?: 0} exercises",
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
private fun ThisWeekCard(
    weekDays: List<WorkoutWeekDay>,
    onOpenPrograms: () -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPrograms),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HevyntTextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_diagonal),
                    contentDescription = null,
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val range = WorkoutDateRange(day.timestampMillis, day.timestampMillis + 86_400_000L)
                            onOpenDay(range)
                        },
                    ) {
                        Text(
                            text = day.dayOfWeekName,
                            color = HevyntTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${day.dayOfMonth}",
                            color = HevyntTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (day.isToday && !day.hasWorkout) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(HevyntAccent, CircleShape),
                            )
                        } else if (day.hasWorkout) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(HevyntTextPrimary, CircleShape),
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .border(1.dp, HevyntOutline, CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThisMonthCard(
    aggregate: MonthlyWorkoutAggregate,
    onOpenPrograms: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPrograms),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HevyntTextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_diagonal),
                    contentDescription = null,
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "${aggregate.workoutCount}",
                        fontFamily = HevyntDisplayFont,
                        fontSize = 32.sp,
                        color = HevyntTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Workouts",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                    )
                }
                Column {
                    Text(
                        text = "${aggregate.completedSetCount}",
                        fontFamily = HevyntDisplayFont,
                        fontSize = 32.sp,
                        color = HevyntTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sets",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                    )
                }
                Column {
                    Text(
                        text = WorkoutCalendar.formatVolumeTonnage(aggregate.totalVolumeKg),
                        fontFamily = HevyntDisplayFont,
                        fontSize = 32.sp,
                        color = HevyntTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Volume",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastSessionCard(
    session: WorkoutHistorySession,
    onClick: () -> Unit,
    onRepeat: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Last session",
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val dateStr = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(session.completedAtMillis))
                val volumeStr = formatWeightWithCommas(session.totalVolumeKg)
                Text(
                    text = "$dateStr · ${session.completedSetCount} sets · $volumeStr kg",
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!session.isLegacySummaryOnly) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HevyntSurfaceInteractive,
                    border = BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier.clickable(onClick = onRepeat),
                ) {
                    Text(
                        text = "Repeat",
                        color = HevyntTextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun formatLastDone(lastPerformedMillis: Long?): String {
    if (lastPerformedMillis == null) return "not performed yet"
    val diffMillis = System.currentTimeMillis() - lastPerformedMillis
    val days = (diffMillis / 86_400_000L).toInt()
    return when {
        days == 0 -> "today"
        days == 1 -> "yesterday"
        days < 7 -> "last done " + SimpleDateFormat("EEE", Locale.getDefault()).format(Date(lastPerformedMillis))
        else -> "last done ${days}d ago"
    }
}

private fun formatWeightWithCommas(value: Double): String {
    val rounded = Math.round(value)
    return NumberFormat.getNumberInstance(Locale.US).format(rounded)
}

internal fun formatRoutineExercisePreview(exercises: List<com.yifeng.commandcenter.data.RoutineExercise>): String =
    exercises.asSequence()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .take(3)
        .joinToString("  •  ")
        .ifBlank { "Add exercises to build your first program." }

