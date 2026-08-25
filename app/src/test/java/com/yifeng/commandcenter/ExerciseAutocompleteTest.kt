package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.history.ExerciseAutocomplete
import com.yifeng.commandcenter.history.WorkoutHistoryExercise
import com.yifeng.commandcenter.history.WorkoutHistoryExerciseGraph
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutHistorySet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExerciseAutocompleteTest {
    @Test
    fun spellingVariantsCollapseToOneCanonicalSuggestionAndReuseIdentity() {
        val folders = listOf(
            RoutineFolder(
                name = "Saved",
                routines = listOf(
                    Routine(
                        name = "Pull",
                        exercises = listOf(RoutineExercise(id = "routine-lat", name = "Lat Pulldown")),
                    ),
                ),
            ),
        )
        val catalog = ExerciseAutocomplete.catalog(
            folders,
            listOf(
                graph("older", 1_000L, null, "Lat pull down", 65.0, 10),
                graph("latest", 2_000L, "routine-lat", "LAT PULLDOWN", 70.0, 8),
            ),
        )

        val suggestion = ExerciseAutocomplete.search(catalog, "lat").single()
        val active = ExerciseAutocomplete.toActiveExercise(suggestion) { "fresh" }

        assertEquals(1, catalog.size)
        assertEquals("routine-lat", suggestion.sourceExerciseId)
        assertEquals(70.0, suggestion.lastPerformance?.weightKg ?: 0.0, 0.0)
        assertEquals(8, suggestion.lastPerformance?.repetitions)
        assertEquals("routine-lat", active.sourceRoutineExerciseId)
        assertFalse(active.sets.any { it.completed })
    }

    @Test
    fun matchingPrioritizesNameMatchThenRecentUsage() {
        val catalog = ExerciseAutocomplete.catalog(
            emptyList(),
            listOf(
                graph("prayer", 3_000L, null, "Lat Prayer", 35.0, 12),
                graph("single", 2_000L, null, "Single-Arm Lat Pulldown", 30.0, 11),
                graph("pulldown", 4_000L, null, "Lat Pulldown", 70.0, 8),
            ),
        )

        assertEquals(
            listOf("Lat Pulldown", "Lat Prayer", "Single-Arm Lat Pulldown"),
            ExerciseAutocomplete.search(catalog, "lat").map { it.name },
        )
    }

    private fun graph(
        id: String,
        completedAt: Long,
        sourceId: String?,
        name: String,
        weight: Double,
        reps: Int,
    ): WorkoutHistoryGraph = WorkoutHistoryGraph(
        session = WorkoutHistorySession(
            id = id,
            sourceRoutineId = null,
            routineName = "Pull",
            startedAtMillis = completedAt - 100,
            completedAtMillis = completedAt,
            durationMillis = 100,
            note = "",
            completedSetCount = 1,
            totalVolumeKg = weight * reps,
            isLegacySummaryOnly = false,
        ),
        exercises = listOf(
            WorkoutHistoryExerciseGraph(
                WorkoutHistoryExercise("exercise-$id", id, sourceId, name, "", 90, 0),
                listOf(WorkoutHistorySet("set-$id", "exercise-$id", 0, weight, reps)),
            ),
        ),
        photos = emptyList(),
    )
}