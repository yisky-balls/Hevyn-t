package com.yifeng.commandcenter

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.history.WorkoutHistoryDatabase
import com.yifeng.commandcenter.history.WorkoutHistoryExercise
import com.yifeng.commandcenter.history.WorkoutHistoryExerciseGraph
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistoryPhoto
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutHistorySet
import com.yifeng.commandcenter.history.WorkoutPhotoManager
import com.yifeng.commandcenter.history.WorkoutPhotoStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutHistoryRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: WorkoutHistoryDatabase
    private lateinit var repository: WorkoutHistoryRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, WorkoutHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun completeGraphRoundTripPreservesOrderDecimalsAndPhotoMetadata() = runBlocking {
        val graph = sampleGraph()
        repository.saveCompletedWorkout(
            graph.copy(
                exercises = graph.exercises.reversed().map { exercise ->
                    exercise.copy(sets = exercise.sets.reversed())
                },
            ),
        )

        val restored = repository.graph("session")!!

        assertEquals(listOf("Squat", "Bench"), restored.exercises.map { it.exercise.name })
        assertEquals(listOf(0, 2), restored.exercises.map { it.exercise.position })
        assertEquals(listOf(0, 2), restored.exercises.last().sets.map { it.position })
        assertEquals(82.5, restored.exercises.last().sets.last().weightKg!!, 0.0)
        assertEquals(1_237.5, restored.session.totalVolumeKg, 0.0)
        assertEquals("workout-photos/session.jpg", restored.photos.single().relativePath)
    }

    @Test
    fun legacyImportRunsExactlyOnceAndSurvivesReopen() = runBlocking {
        val summary = LastWorkoutSummary("Legacy", 1234L, 3, 456.75)

        assertTrue(repository.importLegacySummaries(listOf(summary)))
        assertFalse(repository.importLegacySummaries(listOf(summary)))
        assertEquals(1, repository.sessions().size)
        assertTrue(repository.sessions().single().isLegacySummaryOnly)

        database.close()
        val file = context.getDatabasePath("history-reopen-test.db")
        file.delete()
        database = Room.databaseBuilder(context, WorkoutHistoryDatabase::class.java, file.name)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao())
        repository.saveCompletedWorkout(sampleGraph())
        database.close()
        database = Room.databaseBuilder(context, WorkoutHistoryDatabase::class.java, file.name)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao())
        assertEquals("Upper", repository.graph("session")?.session?.routineName)
        database.close()
        file.delete()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun graphInsertIsAtomicWhenChildInsertFails() = runBlocking {
        val invalid = sampleGraph().copy(
            exercises = listOf(
                sampleGraph().exercises.first().copy(
                    sets = listOf(
                        WorkoutHistorySet("duplicate", "squat", 0, 100.0, 5),
                        WorkoutHistorySet("duplicate", "squat", 1, 100.0, 5),
                    ),
                ),
            ),
        )

        runCatching { repository.saveCompletedWorkout(invalid) }

        assertNull(repository.graph("session"))
    }

    @Test
    fun photoMetadataCanBeReplacedAndRemoved() = runBlocking {
        repository.saveCompletedWorkout(sampleGraph())
        val replacement = WorkoutHistoryPhoto(
            id = "replacement-photo",
            sessionId = "session",
            relativePath = "workout-photos/replacement.image",
            position = 0,
            createdAtMillis = 62_000L,
        )

        assertEquals(
            listOf("workout-photos/session.jpg"),
            repository.replaceVisiblePhoto(replacement),
        )
        assertEquals(replacement, repository.graph("session")?.photos?.single())
        assertEquals(
            listOf("workout-photos/replacement.image"),
            repository.removeVisiblePhoto("session"),
        )
        assertTrue(repository.graph("session")?.photos?.isEmpty() == true)
    }

    @Test
    fun deletingSessionRemovesGraphAndAppOwnedPhotoFile() = runBlocking {
        val graph = sampleGraph()
        repository.saveCompletedWorkout(graph)
        val photoFile = File(context.filesDir, graph.photos.single().relativePath).apply {
            parentFile!!.mkdirs()
            writeText("image")
        }
        val manager = WorkoutPhotoManager(repository, WorkoutPhotoStore(context))

        manager.deleteSession(graph.session.id)

        assertNull(repository.graph(graph.session.id))
        assertFalse(photoFile.exists())
    }

    private fun sampleGraph() = WorkoutHistoryGraph(
        session = WorkoutHistorySession(
            id = "session",
            sourceRoutineId = "routine",
            routineName = "Upper",
            startedAtMillis = 1_000L,
            completedAtMillis = 61_000L,
            durationMillis = 60_000L,
            note = "Strong session",
            completedSetCount = 3,
            totalVolumeKg = 1_237.5,
            isLegacySummaryOnly = false,
        ),
        exercises = listOf(
            WorkoutHistoryExerciseGraph(
                WorkoutHistoryExercise("squat", "session", "source-squat", "Squat", "", 120, 0),
                listOf(WorkoutHistorySet("squat-set", "squat", 0, 100.0, 5)),
            ),
            WorkoutHistoryExerciseGraph(
                WorkoutHistoryExercise("bench", "session", "source-bench", "Bench", "Pause", 90, 2),
                listOf(
                    WorkoutHistorySet("bench-set-0", "bench", 0, 80.0, 5),
                    WorkoutHistorySet("bench-set-2", "bench", 2, 82.5, 4),
                ),
            ),
        ),
        photos = listOf(
            WorkoutHistoryPhoto("photo", "session", "workout-photos/session.jpg", 0, 61_100L),
        ),
    )
}