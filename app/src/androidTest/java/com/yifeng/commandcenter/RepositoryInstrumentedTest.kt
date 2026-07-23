package com.yifeng.commandcenter

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineBuildTarget
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutSet
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        clearPreferences()
    }

    @After
    fun tearDown() = clearPreferences()

    @Test
    fun legacyActiveWorkoutJsonLoadsAsNormalSession() {
        val raw = JSONObject()
            .put("routineName", "Legacy session")
            .put("startedAtMillis", 1234L)
            .put("restSeconds", 75)
            .put("exercises", JSONArray().put(
                JSONObject()
                    .put("id", "exercise-1")
                    .put("name", "Bench")
                    .put("sets", JSONArray().put(
                        JSONObject()
                            .put("id", "set-1")
                            .put("weight", "82.5")
                            .put("reps", "5")
                            .put("completed", true)
                    ))
            ))
        activePreferences().edit().putString("workout", raw.toString()).commit()

        val session = ActiveWorkoutRepository(context).loadSession()!!

        assertNull(session.routineBuildTarget)
        assertEquals("Legacy session", session.workout.routineName)
        assertEquals(75, session.workout.exercises.single().restSeconds)
        assertEquals("82.5", session.workout.exercises.single().sets.single().weight)
    }

    @Test
    fun builderSessionRoundTripPreservesTargetAndWorkoutEdits() {
        val repository = ActiveWorkoutRepository(context)
        val expected = ActiveWorkoutSession(
            workout = ActiveWorkout(
                routineName = "Edited routine",
                startedAtMillis = 5678L,
                exercises = listOf(
                    ActiveExercise(
                        id = "exercise-1",
                        name = "Bench",
                        notes = "Pause",
                        restSeconds = 105,
                        sets = listOf(
                            WorkoutSet(
                                id = "set-1",
                                weight = "82.5",
                                reps = "5",
                                completed = true,
                            )
                        ),
                    )
                ),
            ),
            routineBuildTarget = RoutineBuildTarget(folderId = "folder-1", routineId = null),
        )

        repository.save(expected)

        assertEquals(expected, repository.loadSession())
    }

    @Test
    fun routineRoundTripPreservesNotesAndSetTemplatesWithoutCompletionState() {
        val repository = RoutineRepository(context)
        repository.saveFolders(listOf(
            RoutineFolder(
                id = "folder-1",
                name = "Folder",
                routines = listOf(
                    Routine(
                        id = "routine-1",
                        name = "Upper",
                        exercises = listOf(
                            RoutineExercise(
                                id = "exercise-1",
                                name = "Bench",
                                notes = "Two-second pause",
                                restSeconds = 135,
                                sets = listOf(
                                    WorkoutSet(
                                        id = "set-1",
                                        weight = "80.5",
                                        reps = "6",
                                        completed = true,
                                    )
                                ),
                            )
                        ),
                    )
                ),
            )
        ))

        val exercise = repository.loadFolders().single().routines.single().exercises.single()

        assertEquals("Two-second pause", exercise.notes)
        assertEquals(135, exercise.restSeconds)
        assertEquals("80.5", exercise.sets.single().weight)
        assertEquals("6", exercise.sets.single().reps)
        assertFalse(exercise.sets.single().completed)
    }

    @Test
    fun legacyRoutineWithoutSetsReceivesBlankTemplates() {
        val raw = JSONArray().put(
            JSONObject()
                .put("id", "folder-1")
                .put("name", "Folder")
                .put("routines", JSONArray().put(
                    JSONObject()
                        .put("id", "routine-1")
                        .put("name", "Legacy")
                        .put("restSeconds", 90)
                        .put("exercises", JSONArray().put(
                            JSONObject()
                                .put("id", "exercise-1")
                                .put("name", "Squat")
                                .put("notes", "Legacy note")
                        ))
                ))
        )
        routinePreferences().edit().putString("routine_folders", raw.toString()).commit()

        val exercise = RoutineRepository(context)
            .loadFolders().single().routines.single().exercises.single()

        assertEquals("Legacy note", exercise.notes)
        assertEquals(90, exercise.restSeconds)
        assertEquals(3, exercise.sets.size)
        exercise.sets.forEach { set ->
            assertEquals("", set.weight)
            assertEquals("", set.reps)
            assertFalse(set.completed)
        }
    }

    @Test
    fun workoutHistoryRoundTripPreservesDecimalVolume() {
        val repository = RoutineRepository(context)
        val expected = LastWorkoutSummary(
            name = "Upper",
            completedAtMillis = 9999L,
            completedSets = 4,
            volumeKg = 1234.5,
        )

        repository.saveLastWorkout(expected)

        assertEquals(listOf(expected), repository.loadWorkoutHistory())
    }

    private fun activePreferences() =
        context.getSharedPreferences("active_workout", Context.MODE_PRIVATE)

    private fun routinePreferences() =
        context.getSharedPreferences("lift_programs", Context.MODE_PRIVATE)

    private fun clearPreferences() {
        activePreferences().edit().clear().commit()
        routinePreferences().edit().clear().commit()
    }
}