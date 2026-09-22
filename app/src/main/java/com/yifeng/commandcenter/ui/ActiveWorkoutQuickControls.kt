package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun QuickSetControlsBar(
    set: WorkoutSet,
    previous: PreviousSetPerformance?,
    precedingSet: WorkoutSet?,
    onWeightAdjust: (Double) -> Unit,
    onRepsAdjust: (Int) -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = HevyntSurfaceInteractive,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Quick Weight Steppers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                QuickStepButton(
                    label = "−",
                    contentDescription = "Decrease weight by 2.5 kg",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeightAdjust(-2.5)
                    },
                )
                Text(
                    "KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = HevyntTextSecondary,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                QuickStepButton(
                    label = "+",
                    contentDescription = "Increase weight by 2.5 kg",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeightAdjust(2.5)
                    },
                )
            }

            // Quick Reps Steppers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                QuickStepButton(
                    label = "−1",
                    contentDescription = "Decrease reps by 1",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRepsAdjust(-1)
                    },
                )
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = HevyntTextSecondary,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                QuickStepButton(
                    label = "+1",
                    contentDescription = "Increase reps by 1",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRepsAdjust(1)
                    },
                )
            }

            // Actions: COPY and DUPLICATE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val canCopy = previous != null || precedingSet != null
                QuickActionButton(
                    label = "COPY",
                    contentDescription = "Copy previous set values",
                    enabled = canCopy,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCopy()
                    },
                )
                QuickActionButton(
                    label = "DUPLICATE",
                    contentDescription = "Duplicate this set",
                    enabled = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDuplicate()
                    },
                )
            }
        }
    }
}

@Composable
internal fun QuickStepButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = HevyntSurfaceRaised,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .size(width = 34.dp, height = 32.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HevyntTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun QuickActionButton(
    label: String,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) HevyntSurfaceRaised else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (enabled) HevyntOutline else Color.Transparent),
        modifier = Modifier
            .height(32.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) MaterialTheme.colorScheme.primary else HevyntTextSecondary.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}
