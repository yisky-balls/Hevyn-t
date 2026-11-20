package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineWorkoutMapperTest {
    @Test
    fun `started workout preserves routine exercise order and intended set structure`() {
        val routine = Routine(
            id = "routine",
            name = "Long routine name",
            exercises = listOf(
                RoutineExercise(
                    id = "press",
                    name = "Incline DB Press",
                    notes = "Controlled",
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet("press-1", "30", "10", completed = true),
                        WorkoutSet("press-2", "28", "12"),
                    ),
                ),
                RoutineExercise(
                    id = "row",
                    name = "Row",
                    sets = listOf(WorkoutSet("row-1")),
                ),
            ),
        )

        val workout = RoutineWorkoutMapper.fromRoutine(routine, 123L, "workout")

        assertEquals("workout", workout.id)
        assertEquals("routine", workout.sourceRoutineId)
        assertEquals(listOf("press", "row"), workout.exercises.map { it.id })
        assertEquals(listOf("press-1", "press-2"), workout.exercises[0].sets.map { it.id })
        assertEquals(listOf("", ""), workout.exercises[0].sets.map { it.weight })
        assertEquals(listOf("", ""), workout.exercises[0].sets.map { it.reps })
        assertEquals(120, workout.exercises[0].restSeconds)
        assertFalse(workout.exercises.flatMap { it.sets }.any { it.completed })
    }

    @Test
    fun `started workout immediately contains all planned exercises and rows`() {
        val routine = Routine(
            id = "lower-2",
            name = "LOWER 2",
            exercises = listOf("rdl", "extension", "squat", "curl").map { id ->
                RoutineExercise(
                    id = id,
                    name = id,
                    sets = List(3) { index ->
                        WorkoutSet("$id-$index", "80", "8", completed = true)
                    },
                )
            },
        )

        val workout = RoutineWorkoutMapper.fromRoutine(routine, 123L, "workout")

        assertEquals(listOf("rdl", "extension", "squat", "curl"), workout.exercises.map { it.id })
        assertEquals(listOf(3, 3, 3, 3), workout.exercises.map { it.sets.size })
        assertTrue(workout.exercises.flatMap { it.sets }.all { set ->
            set.weight.isEmpty() && set.reps.isEmpty() && !set.completed
        })
    }

    @Test
    fun `routine editor preserves template performance fields without completion`() {
        val routine = Routine(
            id = "routine",
            name = "Routine",
            exercises = listOf(
                RoutineExercise(
                    id = "rdl",
                    name = "RDL",
                    sets = listOf(WorkoutSet("set", "80", "8", completed = true)),
                ),
            ),
        )

        val draft = RoutineWorkoutMapper.forEditing(routine, 123L, "draft")
        val set = draft.exercises.single().sets.single()

        assertEquals("80", set.weight)
        assertEquals("8", set.reps)
        assertFalse(set.completed)
    }

    @Test
    fun `syncRoutineFromWorkout returns null when only weights reps and completion changed`() {
        val routine = Routine(
            id = "upper",
            name = "Upper",
            exercises = listOf(
                RoutineExercise(
                    id = "bench",
                    name = "Bench Press",
                    notes = "Pause",
                    restSeconds = 180,
                    sets = listOf(WorkoutSet("s1"), WorkoutSet("s2")),
                ),
            ),
        )
        val started = RoutineWorkoutMapper.fromRoutine(routine, 100L, "w1")
        val logged = started.copy(
            exercises = started.exercises.map { ex ->
                ex.copy(sets = ex.sets.map { it.copy(weight = "80", reps = "8", completed = true) })
            },
        )

        assertNull(RoutineWorkoutMapper.syncRoutineFromWorkout(routine, logged))
    }

    @Test
    fun `syncRoutineFromWorkout persists exercise rename notes rest and set count for next workout`() {
        val routine = Routine(
            id = "upper",
            name = "Upper",
            exercises = listOf(
                RoutineExercise(
                    id = "bench",
                    name = "Bench Press",
                    notes = "Old note",
                    restSeconds = 180,
                    sets = listOf(WorkoutSet("s1"), WorkoutSet("s2")),
                ),
            ),
        )
        val started = RoutineWorkoutMapper.fromRoutine(routine, 100L, "w1")
        val edited = started.copy(
            exercises = listOf(
                started.exercises.single().copy(
                    name = "  Incline Barbell Press ",
                    notes = "30 deg incline",
                    restSeconds = 150,
                    sets = listOf(
                        WorkoutSet("s1", "70", "8", completed = true),
                        WorkoutSet("s2", "70", "8", completed = true),
                        WorkoutSet("s3-added", "70", "7", completed = true),
                    ),
                ),
            ),
        )

        var counter = 0
        val synced = RoutineWorkoutMapper.syncRoutineFromWorkout(routine, edited) { "gen-${++counter}" }

        assertNotNull(synced)
        val syncedEx = synced!!.exercises.single()
        assertEquals("bench", syncedEx.id)
        assertEquals("Incline Barbell Press", syncedEx.name)
        assertEquals("30 deg incline", syncedEx.notes)
        assertEquals(150, syncedEx.restSeconds)
        assertEquals(3, syncedEx.sets.size)
        assertEquals(listOf("s1", "s2", "gen-1"), syncedEx.sets.map { it.id })
        assertFalse(syncedEx.sets.any { it.completed })
    }

    @Test
    fun `syncRoutineFromWorkout saves added removed reordered exercises without foreign PK collisions`() {
        val routine = Routine(
            id = "upper",
            name = "Upper",
            exercises = listOf(
                RoutineExercise(id = "bench", name = "Bench Press", sets = listOf(WorkoutSet("b1"))),
                RoutineExercise(id = "row", name = "Row", sets = listOf(WorkoutSet("r1"))),
            ),
        )
        val addedFromOtherRoutine = ActiveExercise(
            id = "active-new-1",
            sourceRoutineExerciseId = "other-routine-exercise-pk",
            name = "Lateral Raise",
            notes = "Slow",
            restSeconds = 90,
            sets = listOf(WorkoutSet("lr-1", "12", "15", completed = true)),
        )
        val workout = ActiveWorkout(
            id = "w2",
            sourceRoutineId = "upper",
            routineName = "Upper",
            exercises = listOf(
                addedFromOtherRoutine,
                ActiveExercise(
                    id = "repeated-row-uuid",
                    sourceRoutineExerciseId = "row",
                    name = "Chest-Supported Row",
                    sets = listOf(WorkoutSet("rep-r1", "60", "10", completed = true)),
                ),
            ),
        )

        var counter = 0
        val synced = RoutineWorkoutMapper.syncRoutineFromWorkout(routine, workout) { "fresh-${++counter}" }

        assertNotNull(synced)
        assertEquals(listOf("Lateral Raise", "Chest-Supported Row"), synced!!.exercises.map { it.name })
        // Newly added exercise must NOT reuse another routine's PK
        assertEquals("fresh-1", synced.exercises[0].id)
        // Repeated exercise matched by sourceRoutineExerciseId preserves its original routine exercise ID
        assertEquals("row", synced.exercises[1].id)
    }

    @Test
    fun `syncRoutineFromWorkout does not wipe non-empty routine when all exercises are blank`() {
        val routine = Routine(
            id = "upper",
            name = "Upper",
            exercises = listOf(RoutineExercise(id = "bench", name = "Bench Press")),
        )
        val blankWorkout = ActiveWorkout(
            id = "w3",
            sourceRoutineId = "upper",
            routineName = "Upper",
            exercises = listOf(ActiveExercise(id = "bench", sourceRoutineExerciseId = "bench", name = "   ")),
        )

        assertNull(RoutineWorkoutMapper.syncRoutineFromWorkout(routine, blankWorkout))
    }
}