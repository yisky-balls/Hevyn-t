package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutMonth
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftBorder
import com.yifeng.commandcenter.ui.theme.LiftDivider
import com.yifeng.commandcenter.ui.theme.LiftMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    routines: List<Routine>,
    workoutHistory: List<LastWorkoutSummary>,
    lastWorkout: LastWorkoutSummary?,
    onOpenPrograms: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
) {
    val routine = routines.firstOrNull()
    var monthOffset by remember { mutableIntStateOf(0) }
    val month = remember(monthOffset, workoutHistory) {
        val target = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
        WorkoutCalendar.month(
            nowMillis = target.timeInMillis,
            completedAtMillis = workoutHistory.map { it.completedAtMillis },
        ).let { if (monthOffset == 0) it else it.copy(today = null) }
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
        )

        Spacer(Modifier.height(22.dp))
        DottedDivider()
        TodayTrainingRow(
            routine = routine,
            onStart = { routine?.let(onStartRoutine) ?: onOpenPrograms() },
        )
        DottedDivider()
        MonthlyTrainingRow(
            count = month.workoutDays.size,
            onClick = onOpenPrograms,
        )

        if (lastWorkout != null) {
            Text(
                "LAST SESSION  /  ${lastWorkout.name.uppercase(Locale.getDefault())}  /  " +
                    "${lastWorkout.completedSets} SETS  /  ${lastWorkout.volumeKg} KG",
                modifier = Modifier.padding(top = 18.dp),
                color = LiftMuted,
                fontFamily = DisplayFont,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.8.sp,
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
                Icons.Outlined.ArrowBackIosNew,
                contentDescription = "Previous month",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Outlined.ArrowForwardIos,
                contentDescription = "Next month",
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, LiftBorder),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = LiftMuted,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            repeat(month.weekCount) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - month.firstDayOffset + 1
                        val valid = day in 1..month.daysInMonth
                        val workout = valid && day in month.workoutDays
                        val today = valid && day == month.today
                        Box(
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (valid) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .then(
                                            if (today) Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(day.toString(), fontSize = 14.sp)
                                    if (workout) {
                                        SignalDot(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 2.dp)
                                                .size(5.dp),
                                        )
                                    }
                                }
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
                fontFamily = DisplayFont,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
            )
            Text(
                routine?.name?.uppercase(Locale.getDefault()) ?: "CREATE A ROUTINE",
                modifier = Modifier.padding(top = 5.dp),
                fontFamily = DisplayFont,
                fontSize = 21.sp,
                letterSpacing = 1.sp,
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
                fontFamily = DisplayFont,
                fontSize = 11.sp,
                letterSpacing = 1.1.sp,
            )
            Icon(
                Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).size(20.dp),
            )
        }
    }
}

@Composable
private fun MonthlyTrainingRow(
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "TRAINING DAYS  $count THIS MONTH",
            modifier = Modifier.weight(1f),
            fontFamily = DisplayFont,
            fontSize = 12.sp,
            letterSpacing = 1.1.sp,
        )
        Icon(
            Icons.Outlined.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
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
