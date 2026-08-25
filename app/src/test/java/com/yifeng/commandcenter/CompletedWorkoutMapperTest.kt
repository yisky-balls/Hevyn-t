package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.CompletedWorkoutMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletedWorkoutMapperTest {
    @Test
    fun savesCompleteSnapshotAndPreservesOriginalPositions() {
        val workout = ActiveWorkout(
            id = "session",
            sourceRoutineId = "routine",
            routineName = "Upper",
            startedAtMillis = 1_000L,
            exercises = listOf(
                ActiveExercise(
                    id = "bench",
                    sourceRoutineExerciseId = "routine-bench",
                    name = "Bench",
                    notes = "Pause",
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet(id = "b0", weight = "80", reps = "5", completed = false),
                        WorkoutSet(id = "b1", weight = "82.5", reps = "4", completed = true),
                    ),
                ),
                ActiveExercise(
                    id = "row",
                    name = "Row",
                    sets = listOf(WorkoutSet(id = "r0", weight = "70", reps = "8")),
                ),
                ActiveExercise(
                    id = "curl",
                    name = "Curl",
                    sets = listOf(WorkoutSet(id = "c0", weight = "12.25", reps = "10", completed = true)),
                ),
            ),
        )

        val draft = CompletedWorkoutMapper.fromActiveWorkout(workout, completedAtMillis = 61_000L)

        assertEquals(3, draft.graph.exercises.size)
        assertEquals(listOf(0, 1, 2), draft.graph.exercises.map { it.exercise.position })
        assertEquals(listOf(0, 1), draft.graph.exercises.first().sets.map { it.position })
        assertEquals(listOf(false, true), draft.graph.exercises.first().sets.map { it.completed })
        assertEquals(82.5, draft.graph.exercises.first().sets.last().weightKg!!, 0.0)
        assertEquals(listOf(false), draft.graph.exercises[1].sets.map { it.completed })
        assertEquals(2, draft.graph.session.completedSetCount)
        assertEquals(452.5, draft.graph.session.totalVolumeKg, 0.0)
        assertEquals(60_000L, draft.graph.session.durationMillis)
        assertEquals(2, draft.unfinishedSetCount)
    }

    @Test
    fun fourExerciseWorkoutRetainsEveryExerciseSetAndValueForReuse() {
        val workout = ActiveWorkout(
            id = "four-exercise-session",
            routineName = "Upper A",
            startedAtMillis = 1_000L,
            exercises = List(4) { exerciseIndex ->
                ActiveExercise(
                    id = "exercise-$exerciseIndex",
                    sourceRoutineExerciseId = "source-$exerciseIndex",
                    name = "Exercise ${exerciseIndex + 1}",
                    sets = List(if (exerciseIndex < 3) 4 else 3) { setIndex ->
                        WorkoutSet(
                            id = "set-$exerciseIndex-$setIndex",
                            weight = "${20 + exerciseIndex * 10 + setIndex}.5",
                            reps = "${12 - setIndex}",
                            completed = setIndex != 2,
                        )
                    },
                )
            },
        )

        val graph = CompletedWorkoutMapper.fromActiveWorkout(workout, 10_000L).graph

        assertEquals(listOf("Exercise 1", "Exercise 2", "Exercise 3", "Exercise 4"), graph.exercises.map { it.exercise.name })
        assertEquals(listOf(4, 4, 4, 3), graph.exercises.map { it.sets.size })
        assertEquals(
            workout.exercises.map { exercise -> exercise.sets.map { it.weight.toDouble() } },
            graph.exercises.map { exercise -> exercise.sets.map { it.weightKg } },
        )
        assertEquals(
            workout.exercises.map { exercise -> exercise.sets.map { it.reps.toInt() } },
            graph.exercises.map { exercise -> exercise.sets.map { it.repetitions } },
        )
        assertEquals(
            workout.exercises.map { exercise -> exercise.sets.map { it.completed } },
            graph.exercises.map { exercise -> exercise.sets.map { it.completed } },
        )
    }

    @Test
    fun blankCompletedValuesRemainNullableWithoutFabricatingPerformance() {
        val workout = ActiveWorkout(
            id = "session",
            routineName = "Empty values",
            exercises = listOf(
                ActiveExercise(
                    id = "exercise",
                    name = "Bench",
                    sets = listOf(WorkoutSet(id = "set", completed = true)),
                ),
            ),
        )

        val set = CompletedWorkoutMapper.fromActiveWorkout(workout, workout.startedAtMillis).graph
            .exercises.single().sets.single()

        assertNull(set.weightKg)
        assertNull(set.repetitions)
    }
}