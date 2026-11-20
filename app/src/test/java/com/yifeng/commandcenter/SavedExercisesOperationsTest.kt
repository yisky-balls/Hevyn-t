package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.SavedExercisesOperations
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedExercisesOperationsTest {

    private val bench1 = RoutineExercise(id = "b1", name = "Bench Press", restSeconds = 180, sets = listOf(WorkoutSet(id = "s1")))
    private val bench2 = RoutineExercise(id = "b2", name = "bench press", restSeconds = 150, sets = listOf(WorkoutSet(id = "s2")))
    private val bench3 = RoutineExercise(id = "b3", name = "Bench Press", restSeconds = 180, sets = listOf(WorkoutSet(id = "s3")))
    private val squat = RoutineExercise(id = "sq", name = "Barbell Squat", restSeconds = 240, sets = listOf(WorkoutSet(id = "s4")))
    private val dbPress = RoutineExercise(id = "db", name = "DB Incline Press", restSeconds = 120, sets = listOf(WorkoutSet(id = "s5")))

    @Test
    fun `extractSavedExercises aggregates exercises and detects duplicates`() {
        val routine1 = Routine(id = "r1", name = "Push A", exercises = listOf(bench1, bench3)) // Duplicate within same routine
        val routine2 = Routine(id = "r2", name = "Push B", exercises = listOf(bench2, squat)) // Casing variant
        val folder = RoutineFolder(id = "f1", name = "Hypertrophy", routines = listOf(routine1, routine2))

        val exercises = SavedExercisesOperations.extractSavedExercises(listOf(folder))

        assertEquals(2, exercises.size) // "Bench Press" and "Barbell Squat"
        val benchSummary = exercises.first { it.canonicalName == "Bench Press" }
        assertEquals(3, benchSummary.usages.size)
        assertTrue(benchSummary.hasDuplicateInSameRoutine)
        assertTrue(benchSummary.hasCasingOrWhitespaceVariants)
        assertEquals(listOf("bench press"), benchSummary.variantNames)
        assertTrue(benchSummary.isDuplicate)

        val squatSummary = exercises.first { it.canonicalName == "Barbell Squat" }
        assertFalse(squatSummary.isDuplicate)
    }

    @Test
    fun `renameExercise updates matching exercises across all routines`() {
        val routine1 = Routine(id = "r1", name = "Push", exercises = listOf(bench1))
        val routine2 = Routine(id = "r2", name = "Full Body", exercises = listOf(bench2, squat))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine1, routine2))

        val updated = SavedExercisesOperations.renameExercise(
            folders = listOf(folder),
            oldName = "Bench Press",
            newName = "Flat Barbell Bench Press",
            newRestSeconds = 200,
        )

        val r1Exercises = updated.first().routines[0].exercises
        val r2Exercises = updated.first().routines[1].exercises

        assertEquals("Flat Barbell Bench Press", r1Exercises[0].name)
        assertEquals(200, r1Exercises[0].restSeconds)
        assertEquals("Flat Barbell Bench Press", r2Exercises[0].name)
        assertEquals(200, r2Exercises[0].restSeconds)
        assertEquals("Barbell Squat", r2Exercises[1].name)
    }

    @Test
    fun `mergeExercises replaces source with target and merges sets when in same routine`() {
        val routine = Routine(id = "r1", name = "Push", exercises = listOf(bench1, dbPress))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine))

        val merged = SavedExercisesOperations.mergeExercises(
            folders = listOf(folder),
            sourceName = "DB Incline Press",
            targetName = "Bench Press",
        )

        val exercises = merged.first().routines.first().exercises
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises.first().name)
        assertEquals(2, exercises.first().sets.size) // Combined sets from bench1 and dbPress
    }

    @Test
    fun `deleteExercise removes all instances of that exercise from all routines`() {
        val routine1 = Routine(id = "r1", name = "Push", exercises = listOf(bench1, squat))
        val routine2 = Routine(id = "r2", name = "Chest", exercises = listOf(bench2))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine1, routine2))

        val updated = SavedExercisesOperations.deleteExercise(listOf(folder), "Bench Press")

        assertEquals(listOf("Barbell Squat"), updated.first().routines[0].exercises.map { it.name })
        assertTrue(updated.first().routines[1].exercises.isEmpty())
    }

    @Test
    fun `deleteExerciseInstance removes only the specific exercise from the specified routine`() {
        val routine1 = Routine(id = "r1", name = "Push", exercises = listOf(bench1, bench3))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine1))

        val updated = SavedExercisesOperations.deleteExerciseInstance(listOf(folder), "r1", "b3")

        val remaining = updated.first().routines.first().exercises
        assertEquals(1, remaining.size)
        assertEquals("b1", remaining.first().id)
    }

    @Test
    fun `deduplicateAll unifies casing and merges duplicate entries in same routines`() {
        val routine1 = Routine(id = "r1", name = "Push", exercises = listOf(bench1, bench3))
        val routine2 = Routine(id = "r2", name = "Chest", exercises = listOf(bench2))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine1, routine2))

        assertEquals(2, SavedExercisesOperations.countDuplicates(listOf(folder)))

        val deduplicated = SavedExercisesOperations.deduplicateAll(listOf(folder))

        assertEquals(0, SavedExercisesOperations.countDuplicates(deduplicated))
        val r1Exercises = deduplicated.first().routines[0].exercises
        val r2Exercises = deduplicated.first().routines[1].exercises

        assertEquals(1, r1Exercises.size)
        assertEquals("Bench Press", r1Exercises.first().name)
        assertEquals(2, r1Exercises.first().sets.size) // sets merged

        assertEquals(1, r2Exercises.size)
        assertEquals("Bench Press", r2Exercises.first().name) // cased unified
    }

    @Test
    fun `SavedExercisesDraft tracks changes before committing`() {
        val routine1 = Routine(id = "r1", name = "Push", exercises = listOf(bench1, bench3))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine1))
        val folders = listOf(folder)

        val draft = com.yifeng.commandcenter.data.SavedExercisesDraft(
            initialFolders = folders,
            currentFolders = folders,
        )
        assertFalse(draft.hasChanges)
        assertEquals(1, draft.duplicateCount)

        // Make an edit (stage changes before committing)
        val deduplicated = SavedExercisesOperations.deduplicateAll(draft.currentFolders)
        val updatedDraft = draft.copy(currentFolders = deduplicated)

        assertTrue(updatedDraft.hasChanges)
        assertEquals(0, updatedDraft.duplicateCount)

        // Discarding restores pristine state
        val discardedDraft = updatedDraft.copy(currentFolders = draft.initialFolders)
        assertFalse(discardedDraft.hasChanges)
    }

    @Test
    fun `extractSavedExercises picks most frequent raw spelling as canonical name`() {
        val lowerFirst = RoutineExercise(id = "l1", name = "bench press", sets = listOf(WorkoutSet(id = "s1")))
        val proper1 = RoutineExercise(id = "p1", name = "Bench Press", sets = listOf(WorkoutSet(id = "s2")))
        val proper2 = RoutineExercise(id = "p2", name = "Bench Press", sets = listOf(WorkoutSet(id = "s3")))
        val folder = RoutineFolder(
            id = "f1",
            name = "Main",
            routines = listOf(
                Routine(id = "r1", name = "Day 1", exercises = listOf(lowerFirst)),
                Routine(id = "r2", name = "Day 2", exercises = listOf(proper1)),
                Routine(id = "r3", name = "Day 3", exercises = listOf(proper2)),
            ),
        )

        val summary = SavedExercisesOperations.extractSavedExercises(listOf(folder)).single()
        assertEquals("Bench Press", summary.canonicalName)
        assertEquals(listOf("bench press"), summary.variantNames)
    }

    @Test
    fun `renameExercise merges in-routine duplicates when renamed to existing exercise`() {
        val routine = Routine(id = "r1", name = "Push", exercises = listOf(bench1, dbPress))
        val folder = RoutineFolder(id = "f1", name = "Main", routines = listOf(routine))

        val renamed = SavedExercisesOperations.renameExercise(
            folders = listOf(folder),
            oldName = "DB Incline Press",
            newName = "Bench Press",
            newNotes = "Unified note",
            newRestSeconds = 165,
        )

        val exercises = renamed.single().routines.single().exercises
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises.single().name)
        assertEquals(2, exercises.single().sets.size)
        assertEquals("Unified note", exercises.single().notes)
        assertEquals(165, exercises.single().restSeconds)
    }

    @Test
    fun `recordHistoryRename preserves transitive rename chains`() {
        val step1 = SavedExercisesOperations.recordHistoryRename(emptyMap(), "DB Press", "Dumbbell Press")
        val step2 = SavedExercisesOperations.recordHistoryRename(step1, "Dumbbell Press", "Flat Dumbbell Press")

        assertEquals("Flat Dumbbell Press", step2["dbpress"])
        assertEquals("Flat Dumbbell Press", step2["dumbbellpress"])
        assertEquals("Flat Dumbbell Press", step2["flatdumbbellpress"])
    }
}
