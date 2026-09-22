package com.yifeng.commandcenter.data

import com.yifeng.commandcenter.history.PreviousSetPerformance

data class SetCompletionResult(
    val workout: ActiveWorkout,
    val completedExerciseId: String,
    val completedSetId: String,
    val completedExerciseName: String,
    val restSeconds: Int,
    val nextTarget: NextSetTarget?,
)

object ActiveWorkoutOperations {
    const val MIN_REST_SECONDS = 15
    const val MAX_REST_SECONDS = 15 * 60

    fun addExercise(workout: ActiveWorkout, exercise: ActiveExercise): ActiveWorkout =
        workout.copy(exercises = workout.exercises + exercise)

    fun removeExercise(workout: ActiveWorkout, exerciseId: String): ActiveWorkout? {
        if (workout.exercises.none { it.id == exerciseId }) return null
        return workout.copy(exercises = workout.exercises.filterNot { it.id == exerciseId })
    }

    fun moveExercise(workout: ActiveWorkout, exerciseId: String, offset: Int): ActiveWorkout? {
        val sourceIndex = workout.exercises.indexOfFirst { it.id == exerciseId }
        val targetIndex = sourceIndex + offset
        if (sourceIndex < 0 || targetIndex !in workout.exercises.indices) return null
        val reordered = workout.exercises.toMutableList()
        val exercise = reordered.removeAt(sourceIndex)
        reordered.add(targetIndex, exercise)
        return workout.copy(exercises = reordered)
    }

    fun updateExerciseName(workout: ActiveWorkout, exerciseId: String, name: String): ActiveWorkout? =
        updateExercise(workout, exerciseId) { it.copy(name = name) }

    fun updateExerciseNotes(workout: ActiveWorkout, exerciseId: String, notes: String): ActiveWorkout? =
        updateExercise(workout, exerciseId) { it.copy(notes = notes) }

    fun updateRestSeconds(
        workout: ActiveWorkout,
        exerciseId: String,
        seconds: Int,
    ): ActiveWorkout? = updateExercise(workout, exerciseId) {
        it.copy(restSeconds = seconds.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS))
    }

    fun updateSet(
        workout: ActiveWorkout,
        exerciseId: String,
        setId: String,
        transform: (WorkoutSet) -> WorkoutSet,
    ): ActiveWorkout? = updateExercise(workout, exerciseId) { exercise ->
        if (exercise.sets.none { it.id == setId }) return@updateExercise null
        exercise.copy(sets = exercise.sets.map { set ->
            if (set.id == setId) transform(set) else set
        })
    }

    fun copyPreviousSet(
        set: WorkoutSet,
        previous: PreviousSetPerformance,
    ): WorkoutSet = set.copy(
        weight = previous.weightKg?.let(::formatWeight) ?: set.weight,
        reps = previous.repetitions?.toString() ?: set.reps,
    )

    fun copyPrecedingSet(
        set: WorkoutSet,
        preceding: WorkoutSet,
    ): WorkoutSet = set.copy(
        weight = preceding.weight,
        reps = preceding.reps,
    )

    fun adjustWeight(set: WorkoutSet, deltaKg: Double): WorkoutSet {
        val current = set.weight.toDoubleOrNull() ?: 0.0
        val updated = (current + deltaKg).coerceAtLeast(0.0)
        return set.copy(weight = formatWeight(updated))
    }

    fun adjustReps(set: WorkoutSet, deltaReps: Int): WorkoutSet {
        val current = set.reps.toIntOrNull() ?: 0
        val updated = (current + deltaReps).coerceAtLeast(0)
        return set.copy(reps = updated.toString())
    }

    fun duplicateSet(workout: ActiveWorkout, exerciseId: String, setId: String): ActiveWorkout? =
        updateExercise(workout, exerciseId) { exercise ->
            val setIndex = exercise.sets.indexOfFirst { it.id == setId }
            if (setIndex < 0) return@updateExercise null
            val sourceSet = exercise.sets[setIndex]
            val newSet = WorkoutSet(
                weight = sourceSet.weight,
                reps = sourceSet.reps,
                completed = false,
            )
            val updatedSets = exercise.sets.toMutableList().apply {
                add(setIndex + 1, newSet)
            }
            exercise.copy(sets = updatedSets)
        }

    fun addSet(workout: ActiveWorkout, exerciseId: String): ActiveWorkout? =
        updateExercise(workout, exerciseId) { exercise ->
            val previous = exercise.sets.lastOrNull()
            exercise.copy(
                sets = exercise.sets + WorkoutSet(
                    weight = previous?.weight.orEmpty(),
                    reps = previous?.reps.orEmpty(),
                )
            )
        }

    fun removeSet(workout: ActiveWorkout, exerciseId: String, setId: String): ActiveWorkout? {
        return updateExercise(workout, exerciseId) { exercise ->
            if (exercise.sets.none { it.id == setId }) return@updateExercise null
            exercise.copy(sets = exercise.sets.filterNot { it.id == setId })
        }
    }

    fun uncompleteSet(workout: ActiveWorkout, exerciseId: String, setId: String): ActiveWorkout? {
        val set = workout.exercises.firstOrNull { it.id == exerciseId }
            ?.sets?.firstOrNull { it.id == setId }
            ?: return null
        if (!set.completed) return null
        return updateSet(workout, exerciseId, setId) { it.copy(completed = false) }
    }

    fun completeSet(
        workout: ActiveWorkout,
        exerciseId: String,
        setId: String,
    ): SetCompletionResult? {
        val exerciseIndex = workout.exercises.indexOfFirst { it.id == exerciseId }
        if (exerciseIndex < 0) return null
        val exercise = workout.exercises[exerciseIndex]
        val setIndex = exercise.sets.indexOfFirst { it.id == setId }
        if (setIndex < 0 || exercise.sets[setIndex].completed) return null

        val updatedExercise = exercise.copy(
            sets = exercise.sets.mapIndexed { index, set ->
                if (index == setIndex) set.copy(completed = true) else set
            }
        )
        val updatedWorkout = workout.copy(
            exercises = workout.exercises.mapIndexed { index, current ->
                if (index == exerciseIndex) updatedExercise else current
            }
        )
        return SetCompletionResult(
            workout = updatedWorkout,
            completedExerciseId = exercise.id,
            completedSetId = exercise.sets[setIndex].id,
            completedExerciseName = exercise.name,
            restSeconds = exercise.restSeconds,
            nextTarget = WorkoutProgress.nextSetAfter(updatedWorkout, exercise.id, setId),
        )
    }

    private fun updateExercise(
        workout: ActiveWorkout,
        exerciseId: String,
        transform: (ActiveExercise) -> ActiveExercise?,
    ): ActiveWorkout? {
        val exerciseIndex = workout.exercises.indexOfFirst { it.id == exerciseId }
        if (exerciseIndex < 0) return null
        val updatedExercise = transform(workout.exercises[exerciseIndex]) ?: return null
        return workout.copy(exercises = workout.exercises.toMutableList().apply {
            this[exerciseIndex] = updatedExercise
        })
    }
}