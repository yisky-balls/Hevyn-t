package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutWeekDay
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceInteractive
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HollowDotColor = Color(0xFF48474C)
private val OutlinedPillBorderColor = Color(0xFF3A3A3E)

@Composable
fun HomeScreen(
    routines: List<RoutineSummary>,
    workoutHistory: List<WorkoutHistorySession>,
    likelyNext: RoutineRecommendation? = null,
    activeWorkout: ActiveWorkout? = null,
    scrollState: ScrollState = rememberScrollState(),
    isGlyphSupported: Boolean = false,
    isGlyphTimerEnabled: Boolean = true,
    isGlyphPulseEnabled: Boolean = true,
    onContinueWorkout: () -> Unit = {},
    onOpenPrograms: () -> Unit,
    onOpenJournal: () -> Unit = {},
    onStartRoutine: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
    onRepeatLastWorkout: () -> Unit = {},
    onImportPlan: () -> Unit = {},
    onManageSavedExercises: () -> Unit = {},
    onToggleGlyphTimer: (Boolean) -> Unit = {},
    onToggleGlyphPulse: (Boolean) -> Unit = {},
) {
    val routine = likelyNext?.routine ?: routines.firstOrNull()
    val todayMillis = remember { System.currentTimeMillis() }
    val todayDateString = remember(todayMillis) {
        SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(Date(todayMillis))
    }

    val weekDays = remember(todayMillis, workoutHistory) {
        WorkoutCalendar.currentWeek(
            nowMillis = todayMillis,
            completedAtMillis = workoutHistory.map { it.completedAtMillis },
        )
    }

    val monthRange = remember(todayMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = todayMillis }
        WorkoutCalendar.monthRange(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
        )
    }
    val monthlyAggregate = remember(monthRange, workoutHistory) {
        WorkoutCalendar.aggregate(workoutHistory, monthRange)
    }

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    if (showSettingsDialog) {
        HomeSettingsDialog(
            isGlyphSupported = isGlyphSupported,
            isGlyphTimerEnabled = isGlyphTimerEnabled,
            isGlyphPulseEnabled = isGlyphPulseEnabled,
            onToggleGlyphTimer = onToggleGlyphTimer,
            onToggleGlyphPulse = onToggleGlyphPulse,
            onImportPlan = {
                showSettingsDialog = false
                onImportPlan()
            },
            onManageSavedExercises = {
                showSettingsDialog = false
                onManageSavedExercises()
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = HevyntPageHorizontalPadding)
            .padding(top = 10.dp, bottom = 20.dp),
    ) {
        HomeTopBar(
            dateText = todayDateString,
            onOpenPrograms = onOpenPrograms,
            onManageSavedExercises = onManageSavedExercises,
            onImportPlan = onImportPlan,
            onTuneClick = { showSettingsDialog = true },
        )

        Spacer(Modifier.height(14.dp))

        HomeHeroHeader(routine = routine)

        Spacer(Modifier.height(18.dp))

        StartWorkoutCard(
            routine = routine,
            activeWorkout = activeWorkout,
            onStart = {
                if (activeWorkout != null) {
                    onContinueWorkout()
                } else {
                    routine?.id?.let(onStartRoutine) ?: onOpenPrograms()
                }
            },
            onBrowsePrograms = onOpenPrograms,
        )

        Spacer(Modifier.height(10.dp))

        ThisWeekCard(
            weekDays = weekDays,
            onOpenJournal = onOpenJournal,
            onOpenDay = onOpenDay,
        )

        Spacer(Modifier.height(10.dp))

        ThisMonthCard(
            aggregate = monthlyAggregate,
            onOpenMonthHistory = { onOpenDay(monthRange) },
        )

        workoutHistory.firstOrNull()?.let { lastSession ->
            Spacer(Modifier.height(10.dp))
            LastSessionCard(
                session = lastSession,
                onClick = { onOpenSession(lastSession.id) },
                onRepeat = onRepeatLastWorkout,
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    dateText: String,
    onOpenPrograms: () -> Unit,
    onManageSavedExercises: () -> Unit,
    onImportPlan: () -> Unit,
    onTuneClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = "Library menu",
                    tint = HevyntTextPrimary,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = HevyntSurfaceInteractive,
            ) {
                DropdownMenuItem(
                    text = { Text("Browse routines") },
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenPrograms()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Manage saved exercises") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onManageSavedExercises()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Import workout plan") },
                    leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onImportPlan()
                    },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = dateText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = HevyntTextPrimary,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onTuneClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = "Settings",
                tint = HevyntTextPrimary,
            )
        }
    }
}

@Composable
private fun HomeHeroHeader(
    routine: RoutineSummary?,
) {
    val rawName = routine?.name ?: "START WORKOUT"
    val routineName = cleanRoutineName(rawName).uppercase(Locale.getDefault())
    val lastDoneText = formatLastDone(routine?.lastPerformedAtMillis)
    val exerciseCount = routine?.exerciseCount ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        Text(
            text = routineName,
            fontFamily = HevyntDisplayFont,
            fontSize = 46.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Bold,
            color = HevyntTextPrimary,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Next workout · $exerciseCount exercise${if (exerciseCount == 1) "" else "s"} · $lastDoneText",
            color = HevyntTextSecondary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun StartWorkoutCard(
    routine: RoutineSummary?,
    activeWorkout: ActiveWorkout?,
    onStart: () -> Unit,
    onBrowsePrograms: () -> Unit,
) {
    val title = if (activeWorkout != null) "Continue workout" else "Start workout"
    val subtitle = if (activeWorkout != null) {
        val activeName = cleanRoutineName(activeWorkout.routineName).ifBlank { "Workout" }
        "$activeName · in progress"
    } else {
        val count = routine?.exerciseCount ?: 0
        "${cleanRoutineName(routine?.name ?: "Routine")} · $count exercise${if (count == 1) "" else "s"}"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onStart),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(HevyntAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HevyntTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBrowsePrograms),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_diagonal),
                    contentDescription = "Browse routines",
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ThisWeekCard(
    weekDays: List<WorkoutWeekDay>,
    onOpenJournal: () -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenJournal),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This week",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_diagonal),
                    contentDescription = "Open journal",
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val range = WorkoutDateRange(day.timestampMillis, day.timestampMillis + 86_400_000L)
                                onOpenDay(range)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = day.dayOfWeekName,
                            color = if (day.isToday) HevyntTextPrimary else HevyntTextSecondary,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${day.dayOfMonth}",
                            color = HevyntTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        when {
                            day.isToday -> {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(HevyntAccent, CircleShape),
                                )
                            }
                            day.hasWorkout -> {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(HevyntTextPrimary, CircleShape),
                                )
                            }
                            else -> {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .border(1.2.dp, HollowDotColor, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThisMonthCard(
    aggregate: MonthlyWorkoutAggregate,
    onOpenMonthHistory: () -> Unit,
) {
    val (volumeNumber, volumeUnit) = remember(aggregate.totalVolumeKg) {
        if (aggregate.totalVolumeKg >= 1000.0) {
            String.format(Locale.US, "%.1f", aggregate.totalVolumeKg / 1000.0) to "t"
        } else {
            formatWeight(aggregate.totalVolumeKg) to " kg"
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenMonthHistory),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This month",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_diagonal),
                    contentDescription = "Open monthly journal",
                    tint = HevyntTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${aggregate.workoutCount}",
                        fontFamily = HevyntDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = HevyntTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Workouts",
                        color = HevyntTextSecondary,
                        fontSize = 13.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${aggregate.completedSetCount}",
                        fontFamily = HevyntDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = HevyntTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sets",
                        color = HevyntTextSecondary,
                        fontSize = 13.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = volumeNumber,
                            fontFamily = HevyntDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = HevyntTextPrimary,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text = volumeUnit,
                            fontFamily = HevyntDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = HevyntTextPrimary,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Volume",
                        color = HevyntTextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LastSessionCard(
    session: WorkoutHistorySession,
    onClick: () -> Unit,
    onRepeat: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = "Last session",
                    color = HevyntTextSecondary,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = cleanRoutineName(session.routineName).ifBlank { session.routineName },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HevyntTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val dateStr = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(session.completedAtMillis))
                val volumeStr = formatWeightWithCommas(session.totalVolumeKg)
                Text(
                    text = "$dateStr · ${session.completedSetCount} sets · $volumeStr kg",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
            }
            if (!session.isLegacySummaryOnly) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, OutlinedPillBorderColor),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onRepeat),
                ) {
                    Text(
                        text = "Repeat",
                        color = HevyntTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSettingsDialog(
    isGlyphSupported: Boolean,
    isGlyphTimerEnabled: Boolean,
    isGlyphPulseEnabled: Boolean,
    onToggleGlyphTimer: (Boolean) -> Unit,
    onToggleGlyphPulse: (Boolean) -> Unit,
    onImportPlan: () -> Unit,
    onManageSavedExercises: () -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SETTINGS & LIBRARY",
                fontFamily = HevyntDisplayFont,
                fontSize = 20.sp,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isGlyphSupported) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Glyph rest timer", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Rear LED countdown during rest",
                                style = MaterialTheme.typography.labelSmall,
                                color = HevyntTextSecondary,
                            )
                        }
                        Switch(
                            checked = isGlyphTimerEnabled,
                            onCheckedChange = onToggleGlyphTimer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Pulse on completion", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Flash all zones when rest finishes",
                                style = MaterialTheme.typography.labelSmall,
                                color = HevyntTextSecondary,
                            )
                        }
                        Switch(
                            checked = isGlyphPulseEnabled,
                            onCheckedChange = onToggleGlyphPulse,
                            enabled = isGlyphTimerEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                    Spacer(Modifier.height(10.dp))
                }
                TextButton(
                    onClick = onManageSavedExercises,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Manage saved exercises",
                        modifier = Modifier.fillMaxWidth(),
                        color = HevyntTextPrimary,
                    )
                }
                TextButton(
                    onClick = onImportPlan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Import workout plan",
                        modifier = Modifier.fillMaxWidth(),
                        color = HevyntTextPrimary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = HevyntAccent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

private fun formatLastDone(lastPerformedMillis: Long?): String {
    if (lastPerformedMillis == null) return "not performed yet"
    val diffMillis = System.currentTimeMillis() - lastPerformedMillis
    val days = (diffMillis / 86_400_000L).toInt()
    return when {
        days == 0 -> "last done today"
        days == 1 -> "last done yesterday"
        days < 7 -> "last done " + SimpleDateFormat("EEE", Locale.getDefault()).format(Date(lastPerformedMillis))
        else -> "last done ${days}d ago"
    }
}

private fun formatWeightWithCommas(value: Double): String {
    val rounded = Math.round(value)
    return NumberFormat.getNumberInstance(Locale.US).format(rounded)
}

internal fun formatRoutineExercisePreview(exercises: List<com.yifeng.commandcenter.data.RoutineExercise>): String =
    exercises.asSequence()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .take(3)
        .joinToString("  •  ")
        .ifBlank { "Add exercises to build your first program." }
