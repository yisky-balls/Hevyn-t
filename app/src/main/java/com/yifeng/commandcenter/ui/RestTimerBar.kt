package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.timer.TimerMath
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import kotlinx.coroutines.delay

@Composable
fun RestTimerBar(
    state: RestTimerState,
    onSubtract: () -> Unit = {},
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
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
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rest",
                color = HevyntTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatTimer(remaining),
                fontFamily = HevyntDisplayFont,
                fontSize = 18.sp,
                color = HevyntRestBlue,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp),
                color = HevyntRestBlue,
                trackColor = HevyntOutline,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "+15 s",
                color = HevyntTextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAdd()
                }),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Skip",
                color = HevyntTextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkip()
                }),
            )
        }
    }
}

private fun formatTimer(remainingSeconds: Int): String {
    val bounded = remainingSeconds.coerceAtLeast(0)
    val minutes = bounded / 60
    val seconds = bounded % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
