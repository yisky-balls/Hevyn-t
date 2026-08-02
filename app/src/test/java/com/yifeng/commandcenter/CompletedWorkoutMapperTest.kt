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
    fun savesOnlyCompletedSetsAndPreservesOriginalPositions() {
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

        assertEquals(2, draft.graph.exercises.size)
        assertEquals(listOf(0, 2), draft.graph.exercises.map { it.exercise.position })
        assertEquals(1, draft.graph.exercises.first().sets.single().position)
        assertEquals(82.5, draft.graph.exercises.first().sets.single().weightKg!!, 0.0)
        assertEquals(2, draft.graph.session.completedSetCount)
        assertEquals(452.5, draft.graph.session.totalVolumeKg, 0.0)
        assertEquals(60_000L, draft.graph.session.durationMillis)
        assertEquals(2, draft.unfinishedSetCount)
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