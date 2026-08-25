package com.yifeng.commandcenter.data

import java.util.UUID

/** Creates session state from a reusable routine template without copying history values. */
object RoutineWorkoutMapper {
    fun fromRoutine(
        routine: Routine,
        startedAtMillis: Long = System.currentTimeMillis(),
        workoutId: String = UUID.randomUUID().toString(),
    ): ActiveWorkout = ActiveWorkout(
        id = workoutId,
        sourceRoutineId = routine.id,
        routineName = routine.name,
        startedAtMillis = startedAtMillis,
        exercises = routine.exercises.map { exercise ->
            ActiveExercise(
                id = exercise.id,
                sourceRoutineExerciseId = exercise.id,
                name = exercise.name,
                notes = exercise.notes,
                restSeconds = exercise.restSeconds,
                sets = exercise.sets.map { set -> set.copy(completed = false) },
            )
        },
    )
}