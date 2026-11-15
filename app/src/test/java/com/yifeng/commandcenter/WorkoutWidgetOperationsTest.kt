package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutWidgetOperationsTest {

    @Test
    fun adjustWeight_increasesAndDecreasesCorrectly() {
        val set = WorkoutSet(weight = "100", reps = "5")

        val plus = ActiveWorkoutOperations.adjustWeight(set, 2.5)
        assertEquals("102.5", plus.weight)

        val minus = ActiveWorkoutOperations.adjustWeight(set, -2.5)
        assertEquals("97.5", minus.weight)

        val zeroFloor = ActiveWorkoutOperations.adjustWeight(set, -150.0)
        assertEquals("0", zeroFloor.weight)
    }

    @Test
    fun adjustReps_increasesAndDecreasesCorrectly() {
        val set = WorkoutSet(weight = "100", reps = "5")

        val plus = ActiveWorkoutOperations.adjustReps(set, 1)
        assertEquals("6", plus.reps)

        val minus = ActiveWorkoutOperations.adjustReps(set, -1)
        assertEquals("4", minus.reps)

        val zeroFloor = ActiveWorkoutOperations.adjustReps(set, -10)
        assertEquals("0", zeroFloor.reps)
    }

    @Test
    fun firstUnfinishedSet_findsCorrectActiveTargetForWidget() {
        val set1 = WorkoutSet(id = "s1", weight = "80", reps = "5", completed = true)
        val set2 = WorkoutSet(id = "s2", weight = "100", reps = "5", completed = false)
        val set3 = WorkoutSet(id = "s3", weight = "105", reps = "5", completed = false)

        val exercise = ActiveExercise(
            id = "e1",
            sourceRoutineExerciseId = "re1",
            name = "Back squat",
            sets = listOf(set1, set2, set3),
        )

        val workout = ActiveWorkout(
            id = "w1",
            sourceRoutineId = "r1",
            routineName = "Lower A",
            exercises = listOf(exercise),
        )

        val target = WorkoutProgress.firstUnfinishedSet(workout)
        assertNotNull(target)
        assertEquals("e1", target!!.exerciseId)
        assertEquals("s2", target.setId)
        assertEquals("Back squat", target.exerciseName)
        assertEquals(2, target.setNumber)
        assertEquals("100", target.weight)
        assertEquals("5", target.reps)
    }

    @Test
    fun firstUnfinishedSet_returnsNullWhenAllSetsCompleted() {
        val set1 = WorkoutSet(id = "s1", weight = "80", reps = "5", completed = true)
        val exercise = ActiveExercise(
            id = "e1",
            sourceRoutineExerciseId = "re1",
            name = "Back squat",
            sets = listOf(set1),
        )
        val workout = ActiveWorkout(
            id = "w1",
            sourceRoutineId = "r1",
            routineName = "Lower A",
            exercises = listOf(exercise),
        )

        assertNull(WorkoutProgress.firstUnfinishedSet(workout))
    }

    @Test
    fun cleanRoutineName_removesDayPrefixesCorrectly() {
        assertEquals("UPPER A", com.yifeng.commandcenter.widget.cleanRoutineName("TUE — UPPER A"))
        assertEquals("Lower B", com.yifeng.commandcenter.widget.cleanRoutineName("Tue - Lower B"))
        assertEquals("Lower A", com.yifeng.commandcenter.widget.cleanRoutineName("Mon – Lower A"))
        assertEquals("Chest", com.yifeng.commandcenter.widget.cleanRoutineName("Day 1: Chest"))
        assertEquals("Legs", com.yifeng.commandcenter.widget.cleanRoutineName("Day 2 - Legs"))
        assertEquals("Power Upper", com.yifeng.commandcenter.widget.cleanRoutineName("Power Upper"))
    }
}
