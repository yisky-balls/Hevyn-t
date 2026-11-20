package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.metrics.GoogleSheetsUrlNormalizer
import com.yifeng.commandcenter.metrics.SheetUrlResult
import com.yifeng.commandcenter.program.ProgramSheetDay
import com.yifeng.commandcenter.program.ProgramSheetState
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceElevated
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

@Composable
internal fun ProgramSheetSection(
    state: ProgramSheetState,
    canStartWorkout: Boolean,
    onOpenConfig: () -> Unit,
    onRefresh: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onStartDay: (weekNumber: Int, dayName: String) -> Unit,
) {
    if (!state.isConfigured && !state.hasData) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = HevyntSurfaceRaised,
            border = BorderStroke(1.dp, HevyntBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onOpenConfig),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HevyntSignalDot(color = HevyntAccent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Sheet Program",
                        color = HevyntTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Connect a sheet to pre-fill weekly weights & deloads",
                        color = HevyntTextSecondary,
                        fontSize = 12.5.sp,
                    )
                }
                Text(
                    text = "Connect",
                    color = HevyntAccent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        return
    }

    val activeWeek = state.activeWeek

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HevyntSignalDot(
                    color = if (activeWeek?.isDeload == true) HevyntRestBlue else HevyntAccent,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sheet Program",
                        color = HevyntTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val subtitle = when {
                        state.isSyncing -> "Syncing Google Sheet…"
                        activeWeek != null -> {
                            val deloadTag = if (activeWeek.isDeload) " · Scarico" else ""
                            "Week ${activeWeek.weekNumber} of ${state.weeks.size}$deloadTag"
                        }
                        else -> "Connected"
                    }
                    Text(
                        text = subtitle,
                        color = if (activeWeek?.isDeload == true) HevyntRestBlue else HevyntTextSecondary,
                        fontSize = 12.5.sp,
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isSyncing,
                    modifier = Modifier.size(36.dp),
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HevyntAccent,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Sync Google Sheet",
                            tint = HevyntTextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onOpenConfig,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Google Sheet link",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            state.lastError?.let { error ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HevyntAccentContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = error,
                            color = HevyntError,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }

            if (state.weeks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.weeks.forEach { week ->
                        val selected = week.weekNumber == activeWeek?.weekNumber
                        val pillColor = when {
                            selected && week.isDeload -> HevyntRestBlue
                            selected -> HevyntAccent
                            else -> HevyntSurfaceInteractive
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pillColor,
                            border = if (selected) null else BorderStroke(1.dp, HevyntOutline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectWeek(week.weekNumber) },
                        ) {
                            Text(
                                text = week.label,
                                color = if (selected) Color.White else HevyntTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            if (activeWeek != null && activeWeek.days.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                activeWeek.days.forEachIndexed { index, day ->
                    if (index > 0) {
                        HevyntDottedRule(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = HevyntOutline.copy(alpha = 0.6f),
                        )
                    }
                    ProgramSheetDayRow(
                        day = day,
                        enabled = canStartWorkout,
                        onStart = { onStartDay(activeWeek.weekNumber, day.dayName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramSheetDayRow(
    day: ProgramSheetDay,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    val leadLift = day.exercises.firstOrNull { it.weightKg.isNotBlank() } ?: day.exercises.firstOrNull()
    val leadSummary = leadLift?.let { ex ->
        val setRep = if (ex.reps.isNotBlank()) "${ex.sets}×${ex.reps}" else "${ex.sets} sets"
        val weight = if (ex.weightKg.isNotBlank()) " @ ${ex.weightKg} kg" else ""
        "${ex.name} · $setRep$weight"
    }.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onStart)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.dayName,
                color = HevyntTextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (leadSummary.isNotBlank()) {
                Text(
                    text = leadSummary,
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = if (enabled) HevyntAccent else HevyntSurfaceInteractive,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onStart),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Start ${day.dayName}",
                    tint = if (enabled) Color.White else HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun ProgramSheetConfigSheet(
    currentUrl: String,
    isConfigured: Boolean,
    onSave: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    var urlInput by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun attemptSave() {
        when (val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(urlInput)) {
            is SheetUrlResult.Invalid -> validationError = result.reason
            is SheetUrlResult.Valid -> {
                validationError = null
                onSave(urlInput.trim())
            }
        }
    }

    HevyntSheet(
        onDismiss = onDismiss,
        title = "Google Sheet program",
        subtitle = "Pre-fill weekly weights, reps & deloads",
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HevyntSurfaceElevated,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "How to connect",
                    color = HevyntTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "1. Open your Google Sheet → Share → \"Anyone with the link can view\" (or Publish to web → CSV).\n" +
                        "2. Paste the link below.\n" +
                        "3. Expected columns: Settimana/Week, Giorno/Day, Esercizio/Exercise, Serie/Sets, Ripetizioni/Reps, Peso/Weight, Recupero/Rest, Note.",
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        HevyntTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                if (validationError != null) validationError = null
            },
            placeholder = "https://docs.google.com/spreadsheets/d/...",
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { attemptSave() }),
            modifier = Modifier.fillMaxWidth(),
        )

        validationError?.let { error ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                color = HevyntError,
                fontSize = 12.5.sp,
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isConfigured) {
                HevyntTonalButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Disconnect",
                        color = HevyntError,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                HevyntTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Cancel",
                        color = HevyntTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HevyntPrimaryButton(
                onClick = ::attemptSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isConfigured) "Save & Sync" else "Connect",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
