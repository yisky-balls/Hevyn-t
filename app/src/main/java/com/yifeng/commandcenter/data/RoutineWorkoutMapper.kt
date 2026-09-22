package com.yifeng.commandcenter.data

import java.util.UUID

/** Creates live session state from a reusable routine without copying performance values. */
object RoutineWorkoutMapper {
    fun fromRoutine(
        routine: Routine,
        startedAtMillis: Long = System.currentTimeMillis(),
        workoutId: String = UUID.randomUUID().toString(),
    ): ActiveWorkout = mapRoutine(
        routine = routine,
        startedAtMillis = startedAtMillis,
        workoutId = workoutId,
        preserveSetValues = false,
    )

    /** Routine editing must preserve existing template fields until the user explicitly changes them. */
    fun forEditing(
        routine: Routine,
        startedAtMillis: Long = System.currentTimeMillis(),
        workoutId: String = UUID.randomUUID().toString(),
    ): ActiveWorkout = mapRoutine(
        routine = routine,
        startedAtMillis = startedAtMillis,
        workoutId = workoutId,
        preserveSetValues = true,
    )

    private fun mapRoutine(
        routine: Routine,
        startedAtMillis: Long,
        workoutId: String,
        preserveSetValues: Boolean,
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
                sets = exercise.sets.map { set ->
                    set.copy(
                        weight = set.weight.takeIf { preserveSetValues }.orEmpty(),
                        reps = set.reps.takeIf { preserveSetValues }.orEmpty(),
                        completed = false,
                    )
                },
            )
        },
    )
}
