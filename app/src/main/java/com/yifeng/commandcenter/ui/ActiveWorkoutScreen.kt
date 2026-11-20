package com.yifeng.commandcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.ui.theme.HevyntDivider
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    previousSets: Map<String, PreviousSetPerformance> = emptyMap(),
    exerciseHistory: Map<String, ExerciseHistorySummary> = emptyMap(),
    exerciseSuggestions: List<ExerciseSuggestion> = emptyList(),
    timerState: RestTimerState? = null,
    isRoutineBuilder: Boolean = false,
    onRequestExerciseSuggestions: () -> Unit,
    onAddExercise: (ExerciseSuggestion) -> Unit,
    onExerciseNameChanged: (String, String) -> Unit,
    onExerciseNotesChanged: (String, String) -> Unit,
    onExerciseRestChanged: (String, Int) -> Unit,
    onExerciseRemoved: (String) -> Unit,
    onExerciseMoved: (String, Int) -> Unit,
    onSetChanged: (String, String, WorkoutSet) -> Unit,
    onSetCompletionChanged: (String, String, Boolean) -> Unit,
    onSetRemoved: (String, String) -> Unit,
    onSetAdded: (String) -> Unit,
    onDuplicateSet: (String, String) -> Unit = { _, _ -> },
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
    isGlyphSupported: Boolean = false,
    isGlyphTimerEnabled: Boolean = true,
    isGlyphPulseEnabled: Boolean = true,
    onToggleGlyphTimer: (Boolean) -> Unit = {},
    onToggleGlyphPulse: (Boolean) -> Unit = {},
    onStartRestTimer: (String) -> Unit = {},
    onSubtractRestTime: () -> Unit = {},
    onAddRestTime: () -> Unit = {},
    onSkipRestTime: () -> Unit = {},
) {
    var restExerciseId by remember { mutableStateOf<String?>(null) }
    var isAddingExercise by rememberSaveable { mutableStateOf(false) }
    val metrics = remember(workout) { WorkoutMetricsCalculator.calculate(workout) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val autoActiveIndex = remember(workout) {
        val idx = workout.exercises.indexOfFirst { ex -> ex.sets.any { !it.completed } }
        if (idx >= 0) idx else 0
    }
    var userActiveExerciseId by rememberSaveable(workout.id) { mutableStateOf<String?>(null) }
    var selectNewestExercise by remember { mutableStateOf(false) }
    LaunchedEffect(workout.exercises.size, selectNewestExercise) {
        if (selectNewestExercise) {
            userActiveExerciseId = workout.exercises.lastOrNull()?.id
            selectNewestExercise = false
        }
    }
    val userIndex = userActiveExerciseId?.let { id ->
        workout.exercises.indexOfFirst { it.id == id }
    } ?: -1
    val activeExerciseIndex = if (userIndex in workout.exercises.indices) userIndex else autoActiveIndex
    var showAllUpcoming by rememberSaveable(workout.id) { mutableStateOf(false) }

    restExerciseId?.let { exerciseId ->
        val exercise = workout.exercises.firstOrNull { it.id == exerciseId }
        RestDurationDialog(
            initialSeconds = exercise?.restSeconds ?: 180,
            onDismiss = { restExerciseId = null },
            onSave = { seconds ->
                restExerciseId = null
                onExerciseRestChanged(exerciseId, seconds)
            },
            onStart = { seconds ->
                restExerciseId = null
                onExerciseRestChanged(exerciseId, seconds)
                onStartRestTimer(exerciseId)
            },
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item(key = "active-workout-header") {
            ActiveWorkoutHeader(
                workout = workout,
                metrics = metrics,
                activeExerciseIndex = activeExerciseIndex,
                isRoutineBuilder = isRoutineBuilder,
                onNavigateBack = if (isRoutineBuilder) onCancel else onMinimize,
                onFinish = onFinish,
                onDiscard = onCancel,
            )
        }

        if (timerState != null) {
            item(key = "rest-timer-bar") {
                Box(
                    modifier = Modifier.padding(
                        horizontal = HevyntPageHorizontalPadding,
                        vertical = 5.dp,
                    ),
                ) {
                    RestTimerBar(
                        state = timerState,
                        onSubtract = onSubtractRestTime,
                        onAdd = onAddRestTime,
                        onSkip = onSkipRestTime,
                    )
                }
            }
        }

        if (workout.exercises.isEmpty() && !isAddingExercise) {
            item(key = "empty-workout") {
                EmptyWorkoutState(
                    isRoutineBuilder = isRoutineBuilder,
                    onAddExercise = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    onDiscard = onCancel,
                )
            }
        }

        if (isRoutineBuilder) {
            itemsIndexed(workout.exercises, key = { _, ex -> "builder-${ex.id}" }) { index, exercise ->
                ExerciseLogger(
                    exercise = exercise,
                    previousSets = previousSets,
                    exerciseHistory = exerciseHistory[exercise.id],
                    isRoutineBuilder = true,
                    onNameChanged = { onExerciseNameChanged(exercise.id, it) },
                    onNotesChanged = { onExerciseNotesChanged(exercise.id, it) },
                    onDelete = { onExerciseRemoved(exercise.id) },
                    canMoveUp = index > 0,
                    canMoveDown = index < workout.exercises.lastIndex,
                    onMoveUp = { onExerciseMoved(exercise.id, -1) },
                    onMoveDown = { onExerciseMoved(exercise.id, 1) },
                    onEditRest = { restExerciseId = exercise.id },
                    onSetChanged = { setId, updated -> onSetChanged(exercise.id, setId, updated) },
                    onSetCompletionChanged = { setId, completed ->
                        onSetCompletionChanged(exercise.id, setId, completed)
                    },
                    onRemoveSet = { setId -> onSetRemoved(exercise.id, setId) },
                    onAddSet = { onSetAdded(exercise.id) },
                    onDuplicateSet = { setId -> onDuplicateSet(exercise.id, setId) },
                    onRequestAddExercise = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    onDiscardWorkout = onCancel,
                )
            }
        } else if (workout.exercises.isNotEmpty() && activeExerciseIndex in workout.exercises.indices) {
            val activeExercise = workout.exercises[activeExerciseIndex]
            item(key = "active-exercise-${activeExercise.id}") {
                ExerciseLogger(
                    exercise = activeExercise,
                    previousSets = previousSets,
                    exerciseHistory = exerciseHistory[activeExercise.id],
                    isRoutineBuilder = false,
                    onNameChanged = { onExerciseNameChanged(activeExercise.id, it) },
                    onNotesChanged = { onExerciseNotesChanged(activeExercise.id, it) },
                    onDelete = {
                        userActiveExerciseId = null
                        onExerciseRemoved(activeExercise.id)
                    },
                    canMoveUp = activeExerciseIndex > 0,
                    canMoveDown = activeExerciseIndex < workout.exercises.lastIndex,
                    onMoveUp = {
                        userActiveExerciseId = activeExercise.id
                        onExerciseMoved(activeExercise.id, -1)
                    },
                    onMoveDown = {
                        userActiveExerciseId = activeExercise.id
                        onExerciseMoved(activeExercise.id, 1)
                    },
                    onEditRest = { restExerciseId = activeExercise.id },
                    onSetChanged = { setId, updated -> onSetChanged(activeExercise.id, setId, updated) },
                    onSetCompletionChanged = { setId, completed ->
                        onSetCompletionChanged(activeExercise.id, setId, completed)
                    },
                    onRemoveSet = { setId -> onSetRemoved(activeExercise.id, setId) },
                    onAddSet = { onSetAdded(activeExercise.id) },
                    onDuplicateSet = { setId -> onDuplicateSet(activeExercise.id, setId) },
                    onRequestAddExercise = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    onDiscardWorkout = onCancel,
                    isGlyphSupported = isGlyphSupported,
                    isGlyphTimerEnabled = isGlyphTimerEnabled,
                    isGlyphPulseEnabled = isGlyphPulseEnabled,
                    onToggleGlyphTimer = onToggleGlyphTimer,
                    onToggleGlyphPulse = onToggleGlyphPulse,
                )
            }

            val upcomingIndices = (0 until workout.exercises.size).filter { it != activeExerciseIndex }
            if (upcomingIndices.isNotEmpty()) {
                val orderedUpcoming = upcomingIndices.filter { it > activeExerciseIndex } +
                    upcomingIndices.filter { it < activeExerciseIndex }
                val nextIndex = orderedUpcoming.first()
                val moreCount = (orderedUpcoming.size - 1).coerceAtLeast(0)
                val visibleUpcomingIndices = if (showAllUpcoming) orderedUpcoming else listOf(nextIndex)

                item(key = "up-next-card") {
                    UpNextCard(
                        workout = workout,
                        upcomingIndices = visibleUpcomingIndices,
                        moreCount = moreCount,
                        expanded = showAllUpcoming,
                        previousSets = previousSets,
                        exerciseHistory = exerciseHistory,
                        onSelectExercise = { targetIndex ->
                            userActiveExerciseId = workout.exercises.getOrNull(targetIndex)?.id
                            showAllUpcoming = false
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        onToggleExpanded = { showAllUpcoming = !showAllUpcoming },
                        onAddExercise = {
                            onRequestExerciseSuggestions()
                            isAddingExercise = true
                        },
                    )
                }
            }
        }

        if (isAddingExercise) {
            item(key = "exercise-search") {
                ExerciseAutocompletePanel(
                    suggestions = exerciseSuggestions,
                    onSelect = { suggestion ->
                        onAddExercise(suggestion)
                        isAddingExercise = false
                        if (!isRoutineBuilder) {
                            selectNewestExercise = true
                        }
                    },
                    onDismiss = { isAddingExercise = false },
                )
            }
        } else if (workout.exercises.isNotEmpty() && isRoutineBuilder) {
            item(key = "add-exercise") {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = HevyntSurfaceRaised,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HevyntPageHorizontalPadding, vertical = 8.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable {
                            onRequestExerciseSuggestions()
                            isAddingExercise = true
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = HevyntTextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "  Add exercise",
                            color = HevyntTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpNextCard(
    workout: ActiveWorkout,
    upcomingIndices: List<Int>,
    moreCount: Int,
    expanded: Boolean,
    previousSets: Map<String, PreviousSetPerformance>,
    exerciseHistory: Map<String, ExerciseHistorySummary>,
    onSelectExercise: (Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onAddExercise: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HevyntSurfaceRaised,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HevyntPageHorizontalPadding, vertical = 5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Up next",
                color = HevyntTextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )

            upcomingIndices.forEachIndexed { idx, exerciseIndex ->
                val exercise = workout.exercises.getOrNull(exerciseIndex) ?: return@forEachIndexed
                if (idx == 0) {
                    Spacer(Modifier.height(14.dp))
                } else {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                    Spacer(Modifier.height(12.dp))
                }
                UpNextExerciseRow(
                    exercise = exercise,
                    summary = collapsedPreviousSummary(
                        exercise = exercise,
                        previousSets = previousSets,
                        history = exerciseHistory[exercise.id],
                    ),
                    onClick = { onSelectExercise(exerciseIndex) },
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAddExercise)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "  Add exercise",
                        color = HevyntTextSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (moreCount > 0) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(HevyntDivider))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleExpanded)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Show $moreCount more",
                        color = HevyntTextSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse upcoming" else "Expand upcoming",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextExerciseRow(
    exercise: ActiveExercise,
    summary: String?,
    onClick: () -> Unit,
) {
    val completedSets = exercise.sets.count { it.completed }
    val totalSets = exercise.sets.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = exercise.name.ifBlank { "Next exercise" },
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = HevyntTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            summary?.let { prev ->
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Last: $prev",
                    color = HevyntTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "$completedSets / $totalSets",
            color = HevyntTextSecondary,
            fontSize = 14.5.sp,
        )
    }
}
