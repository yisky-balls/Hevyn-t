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

    /**
     * Propagates exercise-level template edits (name, notes, rest timer, set count, additions,
     * removals, and ordering) from a finished workout back to its source routine for subsequent
     * sessions. Returns `null` if no structural/template fields changed or if no valid exercises
     * remain.
     */
    fun syncRoutineFromWorkout(
        existingRoutine: Routine,
        workout: ActiveWorkout,
        nextId: () -> String = { UUID.randomUUID().toString() },
    ): Routine? {
        val existingById = existingRoutine.exercises.associateBy(RoutineExercise::id)
        val claimedExerciseIds = mutableSetOf<String>()

        val updatedExercises = workout.exercises.mapNotNull { activeExercise ->
            val cleanName = activeExercise.name.trim()
            if (cleanName.isEmpty()) return@mapNotNull null

            val reusedExerciseId = listOfNotNull(
                activeExercise.sourceRoutineExerciseId,
                activeExercise.id,
            ).firstOrNull { candidateId ->
                candidateId in existingById && candidateId !in claimedExerciseIds
            }
            val finalExerciseId = reusedExerciseId ?: nextId()
            claimedExerciseIds.add(finalExerciseId)

            val previousTemplate = reusedExerciseId?.let(existingById::get)
            val templateSets = List(activeExercise.sets.size) { setIndex ->
                val existingSet = previousTemplate?.sets?.getOrNull(setIndex)
                if (existingSet != null) {
                    existingSet.copy(completed = false)
                } else {
                    WorkoutSet(
                        id = nextId(),
                        weight = "",
                        reps = "",
                        completed = false,
                    )
                }
            }

            RoutineExercise(
                id = finalExerciseId,
                name = cleanName,
                notes = activeExercise.notes,
                restSeconds = activeExercise.restSeconds,
                sets = templateSets,
            )
        }

        if (updatedExercises.isEmpty() && existingRoutine.exercises.isNotEmpty()) {
            return null
        }

        val hasTemplateChange = existingRoutine.exercises.size != updatedExercises.size ||
            existingRoutine.exercises.zip(updatedExercises).any { (old, new) ->
                old.id != new.id ||
                    old.name != new.name ||
                    old.notes != new.notes ||
                    old.restSeconds != new.restSeconds ||
                    old.sets.size != new.sets.size
            }

        if (!hasTemplateChange) return null
        return existingRoutine.copy(exercises = updatedExercises)
    }

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
