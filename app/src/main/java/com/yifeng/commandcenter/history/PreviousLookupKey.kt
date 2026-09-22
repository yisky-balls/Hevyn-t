package com.yifeng.commandcenter.history

import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet

data class PreviousLookupKey(
    val workoutId: String,
    val startedAtMillis: Long,
    val exercises: List<PreviousExerciseLookupKey>,
) {
    companion object {
        fun from(workout: ActiveWorkout?): PreviousLookupKey? = workout?.let {
            PreviousLookupKey(
                workoutId = it.id,
                startedAtMillis = it.startedAtMillis,
                exercises = it.exercises.map { exercise ->
                    PreviousExerciseLookupKey(
                        id = exercise.id,
                        sourceRoutineExerciseId = exercise.sourceRoutineExerciseId,
                        name = exercise.name,
                        setIds = exercise.sets.map(WorkoutSet::id),
                    )
                },
            )
        }
    }
}

data class PreviousExerciseLookupKey(
    val id: String,
    val sourceRoutineExerciseId: String?,
    val name: String,
    val setIds: List<String>,
)

data class PreparedRoutineStart(
    val workoutId: String,
    val previousSets: Map<String, PreviousSetPerformance>,
    val exerciseHistory: Map<String, ExerciseHistorySummary> = emptyMap(),
)
