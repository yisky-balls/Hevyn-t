package com.yifeng.commandcenter

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineLibraryDatabase
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.widget.WorkoutWidgetState
import com.yifeng.commandcenter.widget.WorkoutWidgetStateLoader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutWidgetStateInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearState()
    }

    @After
    fun tearDown() = clearState()

    @Test
    fun activeWorkoutOverridesSharedLikelyNextStateAndClearReturnsToIdle() = runBlocking {
        RoutineRepository(context).replaceFolders(
            listOf(
                RoutineFolder(
                    id = "folder",
                    name = "Training",
                    routines = listOf(
                        Routine(
                            id = "lower-2",
                            name = "LOWER 2",
                            exercises = List(4) { index ->
                                RoutineExercise(
                                    id = "exercise-$index",
                                    name = "Exercise $index",
                                    sets = List(3) { setIndex ->
                                        WorkoutSet(id = "template-$index-$setIndex")
                                    },
                                )
                            },
                        ),
                    ),
                ),
            ),
        )

        val idle = WorkoutWidgetStateLoader.load(context)
        assertTrue(idle is WorkoutWidgetState.Idle)
        idle as WorkoutWidgetState.Idle
        assertEquals("lower-2", idle.recommendation?.routine?.id)
        assertEquals(4, idle.recommendation?.routine?.exerciseCount)

        val activeRepository = ActiveWorkoutRepository(context)
        activeRepository.save(
            ActiveWorkoutSession(
                ActiveWorkout(
                    id = "active",
                    sourceRoutineId = "lower-2",
                    routineName = "LOWER 2",
                    startedAtMillis = System.currentTimeMillis() - 60_000L,
                    exercises = listOf(
                        ActiveExercise(
                            id = "exercise-0",
                            sourceRoutineExerciseId = "exercise-0",
                            name = "RDL",
                            sets = listOf(
                                WorkoutSet(id = "set-1", completed = true),
                                WorkoutSet(id = "set-2", completed = true),
                                WorkoutSet(id = "set-3"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val active = WorkoutWidgetStateLoader.load(context)
        assertTrue(active is WorkoutWidgetState.Active)
        active as WorkoutWidgetState.Active
        assertEquals("LOWER 2", active.workout.routineName)
        assertEquals(2, active.completedSets)
        assertEquals(3, active.totalSets)

        activeRepository.clear()
        val idleAgain = WorkoutWidgetStateLoader.load(context)
        assertTrue(idleAgain is WorkoutWidgetState.Idle)
        assertEquals("lower-2", (idleAgain as WorkoutWidgetState.Idle).recommendation?.routine?.id)
    }

    private fun clearState() {
        RoutineLibraryDatabase.resetForTesting(context)
        context.getSharedPreferences("active_workout", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("lift_programs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}