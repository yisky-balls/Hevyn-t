package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.util.Locale

@Composable
internal fun RestDurationDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onStart: ((Int) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var seconds by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    val presets = listOf(60, 90, 120, 180, 300)

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Rest timer",
        subtitle = "Default countdown after completing a set",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60),
                fontFamily = HevyntDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 50.sp,
                letterSpacing = 1.sp,
                color = HevyntRestBlue,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HevyntTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        seconds = (seconds - 15).coerceAtLeast(15)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("−15 s", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                HevyntTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        seconds = (seconds + 15).coerceAtMost(900)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+15 s", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presets.forEach { preset ->
                    val selected = seconds == preset
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            seconds = preset
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selected) HevyntRestBlue.copy(alpha = 0.16f) else HevyntSurfaceInteractive,
                        border = if (selected) BorderStroke(1.dp, HevyntRestBlue.copy(alpha = 0.8f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${preset / 60}:${(preset % 60).toString().padStart(2, '0')}",
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) HevyntRestBlue else HevyntTextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onStart != null) {
                    HevyntTonalButton(
                        onClick = { onSave(seconds) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntTextPrimary,
                        )
                    }
                    HevyntPrimaryButton(
                        onClick = { onStart(seconds) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Start timer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    HevyntTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntTextSecondary,
                        )
                    }
                    HevyntPrimaryButton(
                        onClick = { onSave(seconds) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
