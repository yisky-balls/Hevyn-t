package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutMonth
import com.yifeng.commandcenter.ui.theme.LiftMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun HomeScreen(
    routines: List<Routine>,
    workoutHistory: List<LastWorkoutSummary>,
    lastWorkout: LastWorkoutSummary?,
    onOpenPrograms: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("Home", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("Build consistency, one set at a time.", color = LiftMuted, fontSize = 16.sp)
        Spacer(Modifier.height(22.dp))

        WorkoutCalendarCard(
            month = WorkoutCalendar.month(
                nowMillis = System.currentTimeMillis(),
                completedAtMillis = workoutHistory.map { it.completedAtMillis },
            )
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("TODAY'S TRAINING", color = LiftMuted, fontSize = 12.sp)
                        Text(
                            routines.firstOrNull()?.name ?: "Create a routine",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    routines.firstOrNull()?.exercises?.take(3)?.joinToString("  •  ")
                        ?: "Add exercises to build your first program.",
                    color = LiftMuted,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        routines.firstOrNull()?.let(onStartRoutine) ?: onOpenPrograms()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                    Text("  Start workout", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("Latest workout", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (lastWorkout == null) {
                Column(Modifier.padding(20.dp)) {
                    Text("No completed workouts yet", fontWeight = FontWeight.SemiBold)
                    Text("Finish a session and its summary will appear here.", color = LiftMuted)
                }
            } else {
                Column(Modifier.padding(20.dp)) {
                    Text(lastWorkout.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault())
                            .format(Date(lastWorkout.completedAtMillis)),
                        color = LiftMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${lastWorkout.completedSets} sets  •  ${lastWorkout.volumeKg} kg volume",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun WorkoutCalendarCard(month: WorkoutMonth) {
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
        Calendar.getInstance().apply {
            clear()
            set(month.year, month.month, 1)
        }.time
    ).uppercase(Locale.getDefault())
    val weekdayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text(monthTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(7) { weekday ->
                        val cell = week * 7 + weekday
                        val day = cell - month.firstDayOffset + 1
                        val isValidDay = day in 1..month.daysInMonth
                        val isWorkoutDay = isValidDay && day in month.workoutDays
                        val isToday = isValidDay && day == month.today
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (isValidDay) {
                                val dayModifier = Modifier
                                    .size(34.dp)
                                    .then(
                                        if (isWorkoutDay) {
                                            Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                        } else if (isToday) {
                                            Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                        } else Modifier
                                    )
                                Column(
                                    modifier = dayModifier,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        color = if (isWorkoutDay) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isWorkoutDay || isToday) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
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