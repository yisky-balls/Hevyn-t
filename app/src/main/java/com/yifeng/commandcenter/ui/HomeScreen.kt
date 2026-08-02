package com.yifeng.commandcenter.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutMonth
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftMuted
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    routines: List<Routine>,
    workoutHistory: List<WorkoutHistorySession>,
    onOpenPrograms: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
) {
    val routine = routines.firstOrNull()
    var monthOffset by remember { mutableIntStateOf(0) }
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LiftPageHorizontalPadding, vertical = 20.dp),
    ) {
        HomeMasthead()
        Spacer(Modifier.height(20.dp))
        DottedDivider()
        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(22.dp))
        DottedDivider()
        TodayTrainingRow(
            routine = routine,
            onStart = { routine?.let(onStartRoutine) ?: onOpenPrograms() },
        )
        DottedDivider()
        MonthlyTrainingRow(
            aggregate = monthlyAggregate,
        )

        workoutHistory.firstOrNull()?.let { lastWorkout ->
            DottedDivider()
            LastSessionRow(
                session = lastWorkout,
                onClick = { onOpenSession(lastWorkout.id) },
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun HomeMasthead() {
    LiftPageHeader(
        title = "HOME",
        subtitle = "Build consistency, one set at a time.",
    )
}

@Composable
private fun WorkoutCalendarPanel(
    month: WorkoutMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onWorkoutDayClick: (day: Int, workoutCount: Int) -> Unit,
) {
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
        Calendar.getInstance().apply {
            clear()
            set(month.year, month.month, 1)
        }.time
    ).uppercase(Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            monthTitle,
            modifier = Modifier.weight(1f),
            fontFamily = DisplayFont,
            fontSize = 25.sp,
            letterSpacing = 1.2.sp,
        )
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = LiftMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        repeat(month.weekCount) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - month.firstDayOffset + 1
                    val valid = day in 1..month.daysInMonth
                    val workoutCount = if (valid) month.workoutCountsByDay[day] ?: 0 else 0
                    val today = valid && day == month.today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .then(
                                if (workoutCount > 0) {
                                    Modifier.clickable { onWorkoutDayClick(day, workoutCount) }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (valid) {
                            if (workoutCount > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .width(2.dp)
                                        .height(22.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                day.toString(),
                                color = if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                            )
                            if (workoutCount > 1) {
                                Text(
                                    "×$workoutCount",
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTrainingRow(
    routine: Routine?,
    onStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(onClick = onStart)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            IndustrialDumbbell(Modifier.size(width = 44.dp, height = 26.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
            Text(
                "TODAY'S TRAINING",
                color = LiftMuted,
                fontSize = 12.sp,
                letterSpacing = 0.7.sp,
            )
            Text(
                routine?.name?.uppercase(Locale.getDefault()) ?: "CREATE A ROUTINE",
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 18.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
            Text(
                formatRoutineExercisePreview(routine?.exercises.orEmpty()),
                modifier = Modifier.padding(top = 4.dp),
                color = LiftMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "START",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).size(20.dp),
            )
        }
    }
}

@Composable
private fun MonthlyTrainingRow(
    aggregate: MonthlyWorkoutAggregate,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp),
    ) {
        Text(
            "THIS MONTH",
            color = LiftMuted,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthlyMetric("WORKOUTS", aggregate.workoutCount.toString())
            MonthlyMetric("SETS", aggregate.completedSetCount.toString())
            MonthlyMetric("VOLUME", "${formatWeight(aggregate.totalVolumeKg)} kg")
        }
    }
}

@Composable
private fun MonthlyMetric(label: String, value: String) {
    Column {
        Text(value, fontFamily = DisplayFont, fontSize = 20.sp)
        Text(label, modifier = Modifier.padding(top = 3.dp), color = LiftMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LastSessionRow(
    session: WorkoutHistorySession,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("LAST SESSION", color = LiftMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
            Text(
                session.routineName,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 16.sp,
                maxLines = 1,
            )
            Text(
                "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(session.completedAtMillis))} · " +
                    "${session.completedSetCount} sets · ${formatWeight(session.totalVolumeKg)} kg",
                modifier = Modifier.padding(top = 4.dp),
                color = LiftMuted,
                fontSize = 13.sp,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Open last session",
            tint = LiftMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

internal fun formatRoutineExercisePreview(exercises: List<RoutineExercise>): String =
    exercises.asSequence()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .take(3)
        .joinToString("  •  ")
        .ifBlank { "Add exercises to build your first program." }
