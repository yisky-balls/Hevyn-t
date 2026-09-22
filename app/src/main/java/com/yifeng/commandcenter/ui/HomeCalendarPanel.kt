package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.WorkoutMonth
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun WorkoutCalendarPanel(
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
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            letterSpacing = 0.5.sp,
        )
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = HevyntTextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.4.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    color = if (today) HevyntAccent else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (today || workoutCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                )
                                if (workoutCount > 0) {
                                    Box(
                                        Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(HevyntAccent, CircleShape)
                                    )
                                } else {
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                            if (workoutCount > 1) {
                                Text(
                                    "×$workoutCount",
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp),
                                    color = HevyntAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
