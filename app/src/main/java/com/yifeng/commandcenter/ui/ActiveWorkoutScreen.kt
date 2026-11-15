package com.yifeng.commandcenter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary

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
    onAddRestTime: () -> Unit = {},
    onSkipRestTime: () -> Unit = {},
) {
    var restExerciseId by remember { mutableStateOf<String?>(null) }
    var isAddingExercise by rememberSaveable { mutableStateOf(false) }
    val metrics = remember(workout) { WorkoutMetricsCalculator.calculate(workout) }

    // Active exercise index is the first uncompleted exercise
    val autoActiveIndex = remember(workout) {
        val idx = workout.exercises.indexOfFirst { ex -> ex.sets.any { !it.completed } }
        if (idx >= 0) idx else 0
    }
    var userActiveIndex by rememberSaveable(workout.id) { mutableIntStateOf(-1) }
    val activeExerciseIndex = if (userActiveIndex in workout.exercises.indices) userActiveIndex else autoActiveIndex
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
    ) {
        // Active Workout Header
        item(key = "active-workout-header") {
            ActiveWorkoutHeader(
                workout = workout,
                metrics = metrics,
                isRoutineBuilder = isRoutineBuilder,
                isGlyphSupported = isGlyphSupported,
                isGlyphTimerEnabled = isGlyphTimerEnabled,
                isGlyphPulseEnabled = isGlyphPulseEnabled,
                onToggleGlyphTimer = onToggleGlyphTimer,
                onToggleGlyphPulse = onToggleGlyphPulse,
                onNavigateBack = if (isRoutineBuilder) onCancel else onMinimize,
                onFinish = onFinish,
                onDiscard = onCancel,
            )
            Spacer(Modifier.height(4.dp))
        }

        // Floating Rest Timer Bar (if active)
        if (timerState != null) {
            item(key = "rest-timer-bar") {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    RestTimerBar(
                        state = timerState,
                        onAdd = onAddRestTime,
                        onSkip = onSkipRestTime,
                    )
                }
                Spacer(Modifier.height(6.dp))
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

        // Active Exercise Logger
        if (workout.exercises.isNotEmpty() && activeExerciseIndex in workout.exercises.indices) {
            val activeExercise = workout.exercises[activeExerciseIndex]
            item(key = "active-exercise-${activeExercise.id}") {
                ExerciseLogger(
                    index = activeExerciseIndex,
                    exercise = activeExercise,
                    previousSets = previousSets,
                    exerciseHistory = exerciseHistory[activeExercise.id],
                    defaultExpanded = true,
                    onNameChanged = { onExerciseNameChanged(activeExercise.id, it) },
                    onNotesChanged = { onExerciseNotesChanged(activeExercise.id, it) },
                    onDelete = { onExerciseRemoved(activeExercise.id) },
                    canMoveUp = activeExerciseIndex > 0,
                    canMoveDown = activeExerciseIndex < workout.exercises.lastIndex,
                    onMoveUp = { onExerciseMoved(activeExercise.id, -1) },
                    onMoveDown = { onExerciseMoved(activeExercise.id, 1) },
                    onEditRest = { restExerciseId = activeExercise.id },
                    onSetChanged = { setId, updated -> onSetChanged(activeExercise.id, setId, updated) },
                    onSetCompletionChanged = { setId, completed ->
                        onSetCompletionChanged(activeExercise.id, setId, completed)
                    },
                    onRemoveSet = { setId -> onSetRemoved(activeExercise.id, setId) },
                    onAddSet = { onSetAdded(activeExercise.id) },
                    onDuplicateSet = { setId -> onDuplicateSet(activeExercise.id, setId) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Up Next Card (Next upcoming exercise)
        val upcomingIndices = (0 until workout.exercises.size).filter { it != activeExerciseIndex }
        if (upcomingIndices.isNotEmpty() && !showAllUpcoming) {
            val nextIndex = upcomingIndices.firstOrNull { it > activeExerciseIndex } ?: upcomingIndices.first()
            val nextExercise = workout.exercises[nextIndex]
            val completedSets = nextExercise.sets.count { it.completed }
            val totalSets = nextExercise.sets.size
            val moreCount = (workout.exercises.size - 2).coerceAtLeast(0)

            item(key = "up-next-card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = HevyntSurfaceRaised,
                    border = BorderStroke(1.dp, HevyntOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { userActiveIndex = nextIndex },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        Text(
                            text = "Up next",
                            color = HevyntTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = nextExercise.name.ifBlank { "Next exercise" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HevyntTextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$completedSets / $totalSets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HevyntTextSecondary,
                            )
                        }
                        collapsedPreviousSummary(nextExercise, previousSets, exerciseHistory[nextExercise.id])?.let { summary ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Last: $summary",
                                color = HevyntTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        if (moreCount > 0) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAllUpcoming = true },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Show $moreCount more",
                                    color = HevyntTextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Expand upcoming",
                                    tint = HevyntTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        } else if (showAllUpcoming) {
            // Expanded upcoming exercises
            item(key = "collapse-upcoming-btn") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clickable { showAllUpcoming = false },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Collapse upcoming",
                        color = HevyntTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Collapse upcoming",
                        tint = HevyntTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            itemsIndexed(workout.exercises, key = { _, exercise -> "all-${exercise.id}" }) { index, exercise ->
                if (index != activeExerciseIndex) {
                    ExerciseLogger(
                        index = index,
                        exercise = exercise,
                        previousSets = previousSets,
                        exerciseHistory = exerciseHistory[exercise.id],
                        defaultExpanded = false,
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
                    )
                    Spacer(Modifier.height(6.dp))
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
                    },
                    onDismiss = { isAddingExercise = false },
                )
            }
        } else if (workout.exercises.isNotEmpty()) {
            item(key = "add-exercise") {
                TextButton(
                    onClick = {
                        onRequestExerciseSuggestions()
                        isAddingExercise = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(44.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "  ADD EXERCISE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
