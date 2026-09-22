package com.yifeng.commandcenter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.PreviousSetPerformance

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    previousSets: Map<String, PreviousSetPerformance> = emptyMap(),
    exerciseHistory: Map<String, ExerciseHistorySummary> = emptyMap(),
    exerciseSuggestions: List<ExerciseSuggestion> = emptyList(),
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
) {
    var restExerciseId by remember { mutableStateOf<String?>(null) }
    var isAddingExercise by rememberSaveable { mutableStateOf(false) }
    val metrics = remember(workout) { WorkoutMetricsCalculator.calculate(workout) }

    restExerciseId?.let { exerciseId ->
        val exercise = workout.exercises.firstOrNull { it.id == exerciseId }
        RestDurationDialog(
            initialSeconds = exercise?.restSeconds ?: 180,
            onDismiss = { restExerciseId = null },
            onSave = { seconds ->
                restExerciseId = null
                onExerciseRestChanged(exerciseId, seconds)
            },
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
    ) {
        item(key = "active-workout-header") {
            ActiveWorkoutHeader(
                workout = workout,
                metrics = metrics,
                isRoutineBuilder = isRoutineBuilder,
                onNavigateBack = if (isRoutineBuilder) onCancel else onMinimize,
                onFinish = onFinish,
                onDiscard = onCancel,
            )
            Spacer(Modifier.height(8.dp))
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

        itemsIndexed(workout.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
            ExerciseLogger(
                index = index,
                exercise = exercise,
                previousSets = previousSets,
                exerciseHistory = exerciseHistory[exercise.id],
                defaultExpanded = index == 0,
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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(42.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "  ADD EXERCISE",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}