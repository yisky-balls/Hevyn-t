package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun ExerciseHistoryPanel(
    history: ExerciseHistorySummary?,
) {
    Surface(
        color = HevyntSurfaceInteractive,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, HevyntOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val lastSession = history?.lastSession
            if (lastSession == null) {
                Text(
                    text = "NO PREVIOUS PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = HevyntTextSecondary,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "No recorded history for this exercise yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HevyntTextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                val dateStr = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
                    .format(java.util.Date(lastSession.completedAtMillis))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LAST SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = HevyntTextSecondary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                lastSession.sets.forEachIndexed { setIdx, setPerf ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Set ${setIdx + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HevyntTextSecondary,
                        )
                        Text(
                            text = PreviousSetResolver.format(setPerf),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = HevyntTextPrimary,
                        )
                    }
                }

                val recent = history.recentSessions.drop(1)
                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "RECENT SESSIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = HevyntTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    recent.forEach { sess ->
                        val sessDate = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
                            .format(java.util.Date(sess.completedAtMillis))
                        val best = sess.bestSet?.let(PreviousSetResolver::format) ?: "—"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = sessDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = HevyntTextSecondary,
                            )
                            Text(
                                text = "${sess.sets.size} sets · best: $best",
                                style = MaterialTheme.typography.bodySmall,
                                color = HevyntTextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun collapsedPreviousSummary(
    exercise: ActiveExercise,
    previousSets: Map<String, PreviousSetPerformance>,
    history: ExerciseHistorySummary? = null,
): String? {
    history?.lastSession?.takeIf { it.sets.isNotEmpty() }?.let { session ->
        return session.sets.joinToString { PreviousSetResolver.format(it) }
    }
    val values = exercise.sets.mapNotNull { set ->
        previousSets[set.id]?.let(PreviousSetResolver::format)
    }
    return values.takeIf { it.isNotEmpty() }?.joinToString()
}
