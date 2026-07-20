package com.yifeng.commandcenter.data

data class NextSetTarget(
    val exerciseId: String,
    val setId: String,
    val exerciseIndex: Int,
    val setIndex: Int,
    val exerciseName: String,
    val weight: String,
    val reps: String,
) {
    val setNumber: Int get() = setIndex + 1
}

object WorkoutProgress {
    fun nextSetAfter(
        workout: ActiveWorkout,
        completedExerciseIndex: Int,
        completedSetIndex: Int,
    ): NextSetTarget? {
        val currentExercise = workout.exercises.getOrNull(completedExerciseIndex) ?: return null

        currentExercise.sets.drop(completedSetIndex + 1).forEachIndexed { offset, set ->
            if (!set.completed) {
                val setIndex = completedSetIndex + offset + 1
                return target(currentExercise, completedExerciseIndex, set, setIndex)
            }
        }

        workout.exercises.drop(completedExerciseIndex + 1).forEachIndexed { offset, exercise ->
            exercise.sets.forEachIndexed { setIndex, set ->
                if (!set.completed) {
                    return target(exercise, completedExerciseIndex + offset + 1, set, setIndex)
                }
            }
        }
        return null
    }

    private fun target(
        exercise: ActiveExercise,
        exerciseIndex: Int,
        set: WorkoutSet,
        setIndex: Int,
    ) = NextSetTarget(
        exerciseId = exercise.id,
        setId = set.id,
        exerciseIndex = exerciseIndex,
        setIndex = setIndex,
        exerciseName = exercise.name,
        weight = set.weight,
        reps = set.reps,
    )
}