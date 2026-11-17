package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.timer.TimerMath
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.Locale
import kotlinx.coroutines.delay

private val RestTrackColor = Color(0xFF28282B)

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
        shape = RoundedCornerShape(22.dp),
        color = HevyntSurfaceRaised,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rest",
                color = HevyntTextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatTimer(remaining),
                fontFamily = HevyntDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = HevyntRestBlue,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RestTrackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(HevyntRestBlue),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "+15 s",
                color = HevyntTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAdd()
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Skip",
                color = HevyntTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkip()
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}

private fun formatTimer(remainingSeconds: Int): String {
    val bounded = remainingSeconds.coerceAtLeast(0)
    val minutes = bounded / 60
    val seconds = bounded % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
