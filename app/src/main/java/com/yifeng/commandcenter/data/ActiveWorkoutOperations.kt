package com.yifeng.commandcenter.data

data class SetCompletionResult(
    val workout: ActiveWorkout,
    val completedExerciseName: String,
    val nextTarget: NextSetTarget?,
)

object ActiveWorkoutOperations {
    const val MIN_REST_SECONDS = 15
    const val MAX_REST_SECONDS = 15 * 60

    fun updateRestSeconds(workout: ActiveWorkout, seconds: Int): ActiveWorkout =
        workout.copy(restSeconds = seconds.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS))

    fun removeSet(workout: ActiveWorkout, exerciseId: String, setId: String): ActiveWorkout? {
        val exerciseIndex = workout.exercises.indexOfFirst { it.id == exerciseId }
        if (exerciseIndex < 0) return null
        val exercise = workout.exercises[exerciseIndex]
        if (exercise.sets.none { it.id == setId }) return null
        val updatedExercise = exercise.copy(sets = exercise.sets.filterNot { it.id == setId })
        return workout.copy(
            exercises = workout.exercises.mapIndexed { index, current ->
                if (index == exerciseIndex) updatedExercise else current
            }
        )
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
            completedExerciseName = exercise.name,
            nextTarget = WorkoutProgress.nextSetAfter(updatedWorkout, exerciseIndex, setIndex),
        )
    }
}