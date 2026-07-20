package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.timer.TimerMath
import kotlinx.coroutines.delay

@Composable
fun RestTimerBar(
    state: RestTimerState,
    onSubtract: () -> Unit,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(250)
        }
    }
    val remaining = TimerMath.remainingSeconds(state.endMillis, nowMillis)
    val progress = if (state.durationSeconds <= 0) 0f else {
        (remaining.toFloat() / state.durationSeconds.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
            state.nextTarget?.let { target ->
                Text(
                    text = buildString {
                        append("NEXT  •  ${target.exerciseName}  •  SET ${target.setNumber}")
                        if (target.weight.isNotBlank()) append("  •  ${target.weight} KG")
                        if (target.reps.isNotBlank()) append(" × ${target.reps}")
                    },
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TimerButton(label = "−15", onClick = onSubtract)
                Text(
                    formatTimer(remaining),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                )
                TimerButton(label = "+15", onClick = onAdd)
                Button(
                    onClick = onSkip,
                    modifier = Modifier.height(58.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Skip", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimerButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(64.dp)
            .height(58.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(label, fontSize = 17.sp)
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}