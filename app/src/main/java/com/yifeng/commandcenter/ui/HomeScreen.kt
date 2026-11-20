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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.yifeng.commandcenter.history.ExerciseProgressMetric
import com.yifeng.commandcenter.history.ExerciseProgressOption
import com.yifeng.commandcenter.history.HistoricalExerciseSetRow
import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.metrics.BodyMetricsCalculator
import com.yifeng.commandcenter.metrics.BodyMetricsPeriod
import com.yifeng.commandcenter.metrics.BodyMetricsSyncState
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntBorderSubtle
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntRestBlue
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HollowDotColor = Color(0xFF48474C)

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
    isSoftLockSupported: Boolean = false,
    isSoftLockEnabled: Boolean = false,
    isSoftLockServiceEnabled: Boolean = false,
    bodyMetrics: BodyMetricsSyncState = BodyMetricsSyncState(),
    exerciseProgressRows: List<HistoricalExerciseSetRow> = emptyList(),
    exerciseProgressOptions: List<ExerciseProgressOption> = emptyList(),
    selectedProgressExerciseKey: String? = null,
    exerciseProgressMetric: ExerciseProgressMetric = ExerciseProgressMetric.TOP_WEIGHT,
    onContinueWorkout: () -> Unit = {},
    onOpenPrograms: () -> Unit,
    onOpenJournal: () -> Unit = {},
    onStartRoutine: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenDay: (WorkoutDateRange) -> Unit,
    onRepeatLastWorkout: () -> Unit = {},
    onImportPlan: () -> Unit = {},
    onManageSavedExercises: () -> Unit = {},
    onOpenBodyMetrics: () -> Unit = {},
    onConfigureBodyMetrics: () -> Unit = {},
    onSelectExerciseProgressMetric: (ExerciseProgressMetric) -> Unit = {},
    onOpenExerciseProgressPicker: () -> Unit = {},
    onOpenExerciseProgress: () -> Unit = {},
    onToggleGlyphTimer: (Boolean) -> Unit = {},
    onToggleGlyphPulse: (Boolean) -> Unit = {},
    onToggleSoftLock: (Boolean) -> Unit = {},
    onOpenSoftLockSettings: () -> Unit = {},
) {
    val routine = likelyNext?.routine
        ?: routines.firstOrNull { it.exerciseCount > 0 }
        ?: routines.firstOrNull()
    val todayMillis = remember(workoutHistory.firstOrNull()?.completedAtMillis) {
        System.currentTimeMillis()
    }
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
            isSoftLockSupported = isSoftLockSupported,
            isSoftLockEnabled = isSoftLockEnabled,
            isSoftLockServiceEnabled = isSoftLockServiceEnabled,
            onToggleGlyphTimer = onToggleGlyphTimer,
            onToggleGlyphPulse = onToggleGlyphPulse,
            onToggleSoftLock = onToggleSoftLock,
            onOpenSoftLockSettings = onOpenSoftLockSettings,
            onImportPlan = {
                showSettingsDialog = false
                onImportPlan()
            },
            onManageSavedExercises = {
                showSettingsDialog = false
                onManageSavedExercises()
            },
            isBodyMetricsConfigured = bodyMetrics.isConfigured,
            onConfigureBodyMetrics = {
                showSettingsDialog = false
                onConfigureBodyMetrics()
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = HevyntPageHorizontalPadding)
            .padding(top = 10.dp, bottom = 124.dp),
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

        BodyMetricsHomeCard(
            syncState = bodyMetrics,
            onClick = onOpenBodyMetrics,
        )

        Spacer(Modifier.height(10.dp))

        ExerciseProgressHomeCard(
            rows = exerciseProgressRows,
            options = exerciseProgressOptions,
            selectedExerciseKey = selectedProgressExerciseKey,
            metric = exerciseProgressMetric,
            onSelectMetric = onSelectExerciseProgressMetric,
            onOpenPicker = onOpenExerciseProgressPicker,
            onOpenDetail = onOpenExerciseProgress,
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

    if (menuExpanded) {
        HevyntActionSheet(
            title = "Library",
            actions = listOf(
                HevyntSheetAction(
                    label = "Browse routines",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = onOpenPrograms,
                ),
                HevyntSheetAction(
                    label = "Manage saved exercises",
                    icon = Icons.AutoMirrored.Outlined.List,
                    onClick = onManageSavedExercises,
                ),
                HevyntSheetAction(
                    label = "Import workout plan",
                    icon = Icons.Outlined.KeyboardArrowDown,
                    onClick = onImportPlan,
                ),
            ),
            onDismiss = { menuExpanded = false },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                            .clickable { onOpenDay(day.range) }
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
                    text = "$dateStr · ${session.completedSetCount} set${if (session.completedSetCount == 1) "" else "s"} · $volumeStr kg",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
            }
            if (!session.isLegacySummaryOnly) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, HevyntBorderSubtle),
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
private fun BodyMetricsHomeCard(
    syncState: BodyMetricsSyncState,
    onClick: () -> Unit,
) {
    val summary = remember(syncState.entries) {
        BodyMetricsCalculator.summarize(syncState.entries, BodyMetricsPeriod.DAYS_30)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HevyntSignalDot(color = HevyntAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Body metrics",
                        color = HevyntTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = if (!syncState.isConfigured) {
                        "Connect ›"
                    } else if (syncState.isSyncing) {
                        "Syncing…"
                    } else {
                        "30D ›"
                    },
                    color = if (!syncState.isConfigured) HevyntAccent else HevyntTextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (!syncState.isConfigured) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Track weight & calories from your Google Sheet",
                    color = HevyntTextSecondary,
                    fontSize = 13.5.sp,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weight",
                            color = HevyntTextSecondary,
                            fontSize = 12.sp,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = summary.latestWeightKg?.let { "${formatWeightKg(it)} kg" } ?: "—",
                                fontFamily = HevyntDisplayFont,
                                fontSize = 24.sp,
                                color = HevyntTextPrimary,
                            )
                            summary.weightDeltaKg?.let { delta ->
                                val sign = if (delta >= 0) "+" else ""
                                Text(
                                    text = "$sign${formatWeightKg(delta)}",
                                    color = if (delta <= 0) HevyntRestBlue else HevyntAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                        }
                    }

                    if (summary.weightPoints.size >= 2) {
                        WeightMiniSparkline(
                            points = summary.weightPoints,
                            modifier = Modifier
                                .width(72.dp)
                                .height(36.dp)
                                .padding(horizontal = 4.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = "Avg kcal",
                            color = HevyntTextSecondary,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = summary.avgCalories?.let { formatCaloriesCount(it) } ?: "—",
                            fontFamily = HevyntDisplayFont,
                            fontSize = 24.sp,
                            color = HevyntTextPrimary,
                        )
                    }
                }

                if (syncState.lastError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = syncState.lastError,
                        color = HevyntAccent,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
    isSoftLockSupported: Boolean,
    isSoftLockEnabled: Boolean,
    isSoftLockServiceEnabled: Boolean,
    onToggleGlyphTimer: (Boolean) -> Unit,
    onToggleGlyphPulse: (Boolean) -> Unit,
    onToggleSoftLock: (Boolean) -> Unit,
    onOpenSoftLockSettings: () -> Unit,
    onImportPlan: () -> Unit,
    onManageSavedExercises: () -> Unit,
    isBodyMetricsConfigured: Boolean,
    onConfigureBodyMetrics: () -> Unit,
    onDismiss: () -> Unit,
) {
    HevyntSheet(
        onDismiss = onDismiss,
        title = "Settings & library",
    ) {
        if (isSoftLockSupported) {
            Text(
                text = "Rest timer",
                color = HevyntTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            HevyntSheetSection {
                HevyntSheetRow(
                    label = "Soft lock on timer end",
                    subtitle = "Lock screen when rest finishes (keeps biometrics)",
                    onClick = { onToggleSoftLock(!isSoftLockEnabled) },
                    trailing = {
                        Switch(
                            checked = isSoftLockEnabled,
                            onCheckedChange = onToggleSoftLock,
                            colors = hevyntSwitchColors(),
                        )
                    },
                )
                if (isSoftLockEnabled || isSoftLockServiceEnabled) {
                    HevyntDottedRule(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = HevyntOutline.copy(alpha = 0.7f),
                    )
                    HevyntSheetRow(
                        label = "Accessibility service",
                        subtitle = if (isSoftLockServiceEnabled) {
                            "Active · tap to manage in Android settings"
                        } else {
                            "Required · tap to enable Hevyn’t Soft Lock (allow restricted settings if sideloaded)"
                        },
                        icon = Icons.Outlined.Lock,
                        onClick = onOpenSoftLockSettings,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (isGlyphSupported) {
            Text(
                text = "Glyph interface",
                color = HevyntTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            HevyntSheetSection {
                HevyntSheetRow(
                    label = "Glyph rest timer",
                    subtitle = "Rear LED countdown during rest",
                    onClick = { onToggleGlyphTimer(!isGlyphTimerEnabled) },
                    trailing = {
                        Switch(
                            checked = isGlyphTimerEnabled,
                            onCheckedChange = onToggleGlyphTimer,
                            colors = hevyntSwitchColors(),
                        )
                    },
                )
                HevyntDottedRule(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = HevyntOutline.copy(alpha = 0.7f),
                )
                HevyntSheetRow(
                    label = "Pulse on completion",
                    subtitle = "Flash all zones when rest finishes",
                    enabled = isGlyphTimerEnabled,
                    onClick = { onToggleGlyphPulse(!isGlyphPulseEnabled) },
                    trailing = {
                        Switch(
                            checked = isGlyphPulseEnabled,
                            onCheckedChange = onToggleGlyphPulse,
                            enabled = isGlyphTimerEnabled,
                            colors = hevyntSwitchColors(),
                        )
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "Library",
            color = HevyntTextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        HevyntSheetSection {
            HevyntSheetRow(
                label = "Manage saved exercises",
                subtitle = "Rename, merge, or clean up duplicates",
                icon = Icons.AutoMirrored.Outlined.List,
                onClick = onManageSavedExercises,
            )
            HevyntDottedRule(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = HevyntOutline.copy(alpha = 0.7f),
            )
            HevyntSheetRow(
                label = "Import workout plan",
                subtitle = "Load routines from a JSON file",
                icon = Icons.Outlined.KeyboardArrowDown,
                onClick = onImportPlan,
            )
            HevyntDottedRule(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = HevyntOutline.copy(alpha = 0.7f),
            )
            HevyntSheetRow(
                label = "Body metrics (Google Sheet)",
                subtitle = if (isBodyMetricsConfigured) {
                    "Connected · tap to update link"
                } else {
                    "Connect a Google Sheet for weight & calories"
                },
                icon = Icons.Outlined.Refresh,
                onClick = onConfigureBodyMetrics,
            )
        }
    }
}

private fun formatLastDone(lastPerformedMillis: Long?): String {
    if (lastPerformedMillis == null) return "not performed yet"
    val days = WorkoutCalendar.daysBetweenLocalDates(lastPerformedMillis)
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
