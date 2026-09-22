package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutMonth
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    routines: List<RoutineSummary>,
    workoutHistory: List<WorkoutHistorySession>,
    scrollState: ScrollState = rememberScrollState(),
    onOpenPrograms: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
    onRepeatLastWorkout: () -> Unit = {},
) {
    val routine = routines.firstOrNull()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val month = remember(monthOffset, workoutHistory) {
        val target = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
        WorkoutCalendar.month(
            nowMillis = target.timeInMillis,
            completedAtMillis = workoutHistory.map { it.completedAtMillis },
            todayMillis = System.currentTimeMillis(),
        )
    }
    val monthlyAggregate = remember(month, workoutHistory) {
        WorkoutCalendar.aggregate(workoutHistory, month.range)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 20.dp),
    ) {
        HomeMasthead()
        Spacer(Modifier.height(14.dp))

        WorkoutCalendarPanel(
            month = month,
            onPreviousMonth = { monthOffset-- },
            onNextMonth = { monthOffset++ },
            onWorkoutDayClick = { day, workoutCount ->
                val range = WorkoutCalendar.dayRange(month.year, month.month, day)
                if (workoutCount == 1) {
                    workoutHistory.firstOrNull { it.completedAtMillis in range }?.let { session ->
                        onOpenSession(session.id)
                    }
                } else {
                    onOpenDay(range)
                }
            },
        )

        Spacer(Modifier.height(14.dp))
        TodayTrainingRow(
            routine = routine,
            onStart = { routine?.id?.let(onStartRoutine) ?: onOpenPrograms() },
        )
        Spacer(Modifier.height(14.dp))
        MonthlyTrainingRow(
            aggregate = monthlyAggregate,
        )

        workoutHistory.firstOrNull()?.let { lastWorkout ->
            Spacer(Modifier.height(14.dp))
            LastSessionRow(
                session = lastWorkout,
                onClick = { onOpenSession(lastWorkout.id) },
                onRepeat = onRepeatLastWorkout,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HomeMasthead() {
    HevyntPageHeader(
        title = "HOME",
        subtitle = "Build consistency, one set at a time.",
    )
}


@Composable
private fun TodayTrainingRow(
    routine: RoutineSummary?,
    onStart: () -> Unit,
) {
    HevyntPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStart),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    "TODAY'S TRAINING",
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    routine?.name?.uppercase(Locale.getDefault()) ?: "CREATE A ROUTINE",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    routine?.let {
                        "${it.exerciseCount} exercise${if (it.exerciseCount == 1) "" else "s"}"
                    } ?: "Create a reusable workout structure.",
                    modifier = Modifier.padding(top = 3.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            Surface(
                color = HevyntAccentContainer,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, HevyntAccent.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "START",
                        color = HevyntAccent,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = HevyntAccent,
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrainingRow(
    aggregate: MonthlyWorkoutAggregate,
) {
    HevyntPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                "THIS MONTH",
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.8.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MonthlyMetric("WORKOUTS", aggregate.workoutCount.toString())
                MonthlyMetric("SETS", aggregate.completedSetCount.toString())
                MonthlyMetric("VOLUME", "${formatWeight(aggregate.totalVolumeKg)} kg")
            }
        }
    }
}

@Composable
private fun MonthlyMetric(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            label,
            modifier = Modifier.padding(top = 3.dp),
            color = HevyntTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LastSessionRow(
    session: WorkoutHistorySession,
    onClick: () -> Unit,
    onRepeat: () -> Unit,
) {
    HevyntPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    "LAST SESSION",
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    session.routineName,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(session.completedAtMillis))} · " +
                        "${session.completedSetCount} sets · ${formatWeight(session.totalVolumeKg)} kg",
                    modifier = Modifier.padding(top = 3.dp),
                    color = HevyntTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!session.isLegacySummaryOnly) {
                TextButton(
                    onClick = onRepeat,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        "REPEAT",
                        color = HevyntAccent,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Open last session",
                tint = HevyntTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

internal fun formatRoutineExercisePreview(exercises: List<RoutineExercise>): String =
    exercises.asSequence()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .take(3)
        .joinToString("  •  ")
        .ifBlank { "Add exercises to build your first program." }
