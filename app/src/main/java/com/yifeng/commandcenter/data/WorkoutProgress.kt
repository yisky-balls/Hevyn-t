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
        completedExerciseId: String,
        completedSetId: String,
    ): NextSetTarget? {
        val completedExerciseIndex = workout.exercises.indexOfFirst { it.id == completedExerciseId }
        if (completedExerciseIndex < 0) return firstUnfinishedSet(workout)
        val currentExercise = workout.exercises[completedExerciseIndex]
        val completedSetIndex = currentExercise.sets.indexOfFirst { it.id == completedSetId }

        if (completedSetIndex >= 0) {
            currentExercise.sets.drop(completedSetIndex + 1).forEachIndexed { offset, set ->
                if (!set.completed) {
                    val setIndex = completedSetIndex + offset + 1
                    return target(currentExercise, completedExerciseIndex, set, setIndex)
                }
            }

            currentExercise.sets.take(completedSetIndex).forEachIndexed { setIndex, set ->
                if (!set.completed) return target(currentExercise, completedExerciseIndex, set, setIndex)
            }
        } else {
            currentExercise.sets.forEachIndexed { setIndex, set ->
                if (!set.completed) return target(currentExercise, completedExerciseIndex, set, setIndex)
            }
        }

        val followingExercises = workout.exercises.drop(completedExerciseIndex + 1)
        val earlierExercises = workout.exercises.take(completedExerciseIndex)
        (followingExercises + earlierExercises).forEach { exercise ->
            val exerciseIndex = workout.exercises.indexOfFirst { it.id == exercise.id }
            exercise.sets.forEachIndexed { setIndex, set ->
                if (!set.completed) {
                    return target(exercise, exerciseIndex, set, setIndex)
                }
            }
        }
        return null
    }

    fun firstUnfinishedSet(workout: ActiveWorkout): NextSetTarget? {
        workout.exercises.forEachIndexed { exerciseIndex, exercise ->
            exercise.sets.forEachIndexed { setIndex, set ->
                if (!set.completed) return target(exercise, exerciseIndex, set, setIndex)
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