package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetrics
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ActiveWorkoutHeader(
    workout: ActiveWorkout,
    metrics: WorkoutMetrics,
    activeExerciseIndex: Int,
    isRoutineBuilder: Boolean,
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }

    if (confirmDiscard) {
        HevyntConfirmDialog(
            title = if (isRoutineBuilder) "Discard routine?" else "Discard workout?",
            message = "This removes the active session from this device. Completed workout history is not affected.",
            confirmText = "Discard",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                confirmDiscard = false
                onDiscard()
            },
            onDismiss = { confirmDiscard = false },
        )
    }

    val totalExercises = workout.exercises.size
    val displayExerciseNum = if (totalExercises > 0) (activeExerciseIndex + 1).coerceIn(1, totalExercises) else 0
    val activeExercise = workout.exercises.getOrNull(activeExerciseIndex)
    val activeCompletedSets = activeExercise?.sets?.count { it.completed } ?: metrics.completedSets
    val activeTotalSets = activeExercise?.sets?.size ?: workout.exercises.sumOf { it.sets.size }
    val cleanTitle = cleanRoutineName(workout.routineName).ifBlank {
        if (isRoutineBuilder) "Routine" else "Workout"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onNavigateBack,
                        onLongClick = { confirmDiscard = true },
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Minimize workout",
                    tint = HevyntTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = cleanTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HevyntTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 88.dp),
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(1.dp, HevyntBorderSubtle),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onFinish),
            ) {
                Text(
                    text = if (isRoutineBuilder) "Save" else "Finish",
                    color = HevyntTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        if (!isRoutineBuilder) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ElapsedChronometer(startedAtMillis = workout.startedAtMillis)

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Exercise $displayExerciseNum of $totalExercises",
                        color = HevyntTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↑ ",
                            color = HevyntAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${formatWeight(metrics.volumeKg)} kg · $activeCompletedSets/$activeTotalSets sets",
                            color = HevyntTextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ElapsedChronometer(startedAtMillis: Long) {
    var nowMillis by remember(startedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((nowMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Text(
        text = timeText,
        fontFamily = HevyntDisplayFont,
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        color = HevyntTextPrimary,
        letterSpacing = 1.sp,
    )
}
