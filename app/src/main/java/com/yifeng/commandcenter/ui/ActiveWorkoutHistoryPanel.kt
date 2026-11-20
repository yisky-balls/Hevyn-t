package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ExerciseHistoryPanel(
    history: ExerciseHistorySummary?,
) {
    Surface(
        color = HevyntSurfaceInteractive,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            val lastSession = history?.lastSession
            if (lastSession == null) {
                Text(
                    text = "No previous performance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HevyntTextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Completed sets for this exercise will appear here.",
                    fontSize = 13.sp,
                    color = HevyntTextSecondary,
                )
            } else {
                val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(Date(lastSession.completedAtMillis))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Last session",
                        fontSize = 13.sp,
                        color = HevyntAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = dateStr,
                        fontSize = 12.5.sp,
                        color = HevyntTextSecondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                lastSession.sets.forEachIndexed { setIdx, setPerf ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Set ${setIdx + 1}",
                            fontSize = 13.5.sp,
                            color = HevyntTextSecondary,
                        )
                        Text(
                            text = PreviousSetResolver.format(setPerf),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntTextPrimary,
                        )
                    }
                }

                val recent = history.recentSessions.drop(1)
                if (recent.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntOutline.copy(alpha = 0.7f)))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Recent sessions",
                        fontSize = 13.sp,
                        color = HevyntTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    recent.forEach { sess ->
                        val sessDate = DateFormat.getDateInstance(DateFormat.SHORT)
                            .format(Date(sess.completedAtMillis))
                        val best = sess.bestSet?.let(PreviousSetResolver::format) ?: "—"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = sessDate,
                                fontSize = 13.sp,
                                color = HevyntTextSecondary,
                            )
                            Text(
                                text = "${sess.sets.size} sets · best $best",
                                fontSize = 13.sp,
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
