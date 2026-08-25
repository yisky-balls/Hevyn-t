package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
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
        color = MaterialTheme.colorScheme.background,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
            state.nextTarget?.let { target ->
                Text(
                    text = buildString {
                        append("NEXT  •  ${target.exerciseName}  •  SET ${target.setNumber}")
                        if (target.weight.isNotBlank()) append("  •  ${target.weight} KG")
                        if (target.reps.isNotBlank()) append(" × ${target.reps}")
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 9.dp),
                    color = HevyntTextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TimerButton(label = "−15", onClick = onSubtract)
                Text(
                    formatTimer(remaining),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontFamily = HevyntDisplayFont,
                    fontSize = 28.sp,
                    letterSpacing = 1.2.sp,
                )
                TimerButton(label = "+15", onClick = onAdd)
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(
                        "SKIP",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerButton(label: String, onClick: () -> Unit) {
    HevyntOutlineButton(
        onClick = onClick,
        modifier = Modifier
            .width(54.dp)
            .height(44.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}