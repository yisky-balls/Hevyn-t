package com.yifeng.commandcenter

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RepeatedWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.CompletedWorkoutMapper
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
import com.yifeng.commandcenter.history.PreviousSetPerformance
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
    fun previousSetsComeFromLastWorkoutContainingThatExercise() = runBlocking {
        val benchWorkout = repeatableGraph("bench-workout", 10_000L).let { graph ->
            graph.copy(
                exercises = listOf(
                    WorkoutHistoryExerciseGraph(
                        WorkoutHistoryExercise(
                            id = "historic-bench",
                            sessionId = graph.session.id,
                            sourceRoutineExerciseId = "old-routine-bench",
                            name = "Incline DB Press",
                            note = "",
                            restSeconds = 90,
                            position = 0,
                        ),
                        listOf(
                            WorkoutHistorySet("bench-0", "historic-bench", 0, 30.0, 10),
                            WorkoutHistorySet("bench-1", "historic-bench", 1, 30.0, 9),
                            WorkoutHistorySet("bench-2", "historic-bench", 2, 28.0, 10),
                        ),
                    ),
                ),
            )
        }
        val interveningWorkout = repeatableGraph("lower-workout", 20_000L).let { graph ->
            graph.copy(
                exercises = listOf(
                    WorkoutHistoryExerciseGraph(
                        WorkoutHistoryExercise(
                            id = "historic-squat",
                            sessionId = graph.session.id,
                            sourceRoutineExerciseId = "squat",
                            name = "Squat",
                            note = "",
                            restSeconds = 120,
                            position = 0,
                        ),
                        listOf(WorkoutHistorySet("squat-0", "historic-squat", 0, 100.0, 5)),
                    ),
                ),
            )
        }
        repository.saveCompletedWorkout(benchWorkout)
        repository.saveCompletedWorkout(interveningWorkout)
        val active = ActiveWorkout(
            id = "active",
            routineName = "Different routine",
            startedAtMillis = 30_000L,
            exercises = listOf(
                ActiveExercise(
                    id = "active-bench",
                    sourceRoutineExerciseId = "new-routine-bench",
                    name = "  incline db press ",
                    sets = listOf(
                        WorkoutSet("active-0"),
                        WorkoutSet("active-1"),
                        WorkoutSet("active-2"),
                    ),
                ),
            ),
        )

        val previous = repository.previousSets(active)

        assertEquals(PreviousSetPerformance(30.0, 10), previous["active-0"])
        assertEquals(PreviousSetPerformance(30.0, 9), previous["active-1"])
        assertEquals(PreviousSetPerformance(28.0, 10), previous["active-2"])
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
    fun failedReplacementDoesNotChangeLastWorkoutPointer() = runBlocking {
        repository.saveCompletedWorkout(repeatableGraph("previous", 10_000L))
        val invalid = repeatableGraph("failed", 20_000L).let { graph ->
            graph.copy(
                exercises = graph.exercises.map { exercise ->
                    exercise.copy(
                        sets = listOf(
                            WorkoutHistorySet("duplicate", exercise.exercise.id, 0, 60.0, 8),
                            WorkoutHistorySet("duplicate", exercise.exercise.id, 1, 60.0, 8),
                        ),
                    )
                },
            )
        }

        assertTrue(runCatching { repository.saveCompletedWorkout(invalid) }.isFailure)

        assertNull(repository.graph("failed"))
        assertEquals("previous", repository.latestRepeatableGraph()?.session?.id)
    }

    @Test
    fun successfulSaveReplacesSingleLastWorkoutPointer() = runBlocking {
        repository.saveCompletedWorkout(repeatableGraph("first", 10_000L))
        repository.saveCompletedWorkout(repeatableGraph("second", 20_000L))

        assertEquals("second", repository.latestRepeatableGraph()?.session?.id)
        assertEquals(2, repository.sessions().size)

        repository.deleteSession("second")

        assertEquals("first", repository.latestRepeatableGraph()?.session?.id)
    }

    @Test
    fun fourExerciseSnapshotRoundTripsIntoLastWorkoutWithEveryOrderedSet() = runBlocking {
        val active = ActiveWorkout(
            id = "complete-four",
            routineName = "Upper A",
            startedAtMillis = 1_000L,
            exercises = List(4) { exerciseIndex ->
                ActiveExercise(
                    id = "active-$exerciseIndex",
                    sourceRoutineExerciseId = "source-$exerciseIndex",
                    name = "Exercise ${exerciseIndex + 1}",
                    sets = List(if (exerciseIndex < 3) 4 else 3) { setIndex ->
                        WorkoutSet(
                            id = "active-$exerciseIndex-$setIndex",
                            weight = "${30 + exerciseIndex * 10 + setIndex}.5",
                            reps = "${10 - setIndex}",
                            completed = setIndex != 2,
                        )
                    },
                )
            },
        )
        repository.saveCompletedWorkout(
            CompletedWorkoutMapper.fromActiveWorkout(active, 10_000L).graph,
        )

        val repeated = RepeatedWorkoutMapper.fromHistory(repository.latestRepeatableGraph()!!)

        assertEquals(active.exercises.map { it.name }, repeated.exercises.map { it.name })
        assertEquals(active.exercises.map { it.sets.size }, repeated.exercises.map { it.sets.size })
        assertEquals(
            active.exercises.map { exercise -> exercise.sets.map { it.weight } },
            repeated.exercises.map { exercise -> exercise.sets.map { it.weight } },
        )
        assertEquals(
            active.exercises.map { exercise -> exercise.sets.map { it.reps } },
            repeated.exercises.map { exercise -> exercise.sets.map { it.reps } },
        )
        assertFalse(repeated.exercises.flatMap { it.sets }.any { it.completed })
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

    @Test
    fun latestRepeatableGraphSkipsNewerLegacyRowsAndSelectsNewestDetailedSession() = runBlocking {
        repository.saveCompletedWorkout(repeatableGraph("older", 10_000L))
        repository.saveCompletedWorkout(repeatableGraph("newest", 20_000L))
        repository.saveCompletedWorkout(
            WorkoutHistoryGraph(
                session = WorkoutHistorySession(
                    id = "legacy",
                    sourceRoutineId = null,
                    routineName = "Legacy summary",
                    startedAtMillis = 25_000L,
                    completedAtMillis = 30_000L,
                    durationMillis = 5_000L,
                    note = "",
                    completedSetCount = 3,
                    totalVolumeKg = 300.0,
                    isLegacySummaryOnly = true,
                ),
                exercises = emptyList(),
                photos = emptyList(),
            ),
        )

        assertEquals("newest", repository.latestRepeatableGraph()?.session?.id)
    }

    @Test
    fun completedHistoryMutationsRequestWidgetRefreshAfterCommit() = runBlocking {
        var refreshCount = 0
        repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao()) {
            refreshCount += 1
        }

        repository.saveCompletedWorkout(sampleGraph())
        assertEquals(1, refreshCount)
        assertTrue(repository.graph("session") != null)

        repository.deleteSession("session")
        assertEquals(2, refreshCount)
        assertNull(repository.graph("session"))
    }

    @Test
    fun completedWorkoutSaveIgnoresPostCommitWidgetRefreshFailure() = runBlocking {
        var refreshAttempts = 0
        repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao()) {
            refreshAttempts += 1
            error("Widget refresh failed")
        }

        val result = runCatching { repository.saveCompletedWorkout(sampleGraph()) }

        assertTrue(result.isSuccess)
        assertEquals(1, refreshAttempts)
        assertEquals("session", repository.graph("session")?.session?.id)
    }

    @Test
    fun renameExercisesUpdatesHistoricalNamesByNormalizedKey() = runBlocking {
        repository.saveCompletedWorkout(repeatableGraph("s1", 10_000L, "bench press"))
        repository.saveCompletedWorkout(repeatableGraph("s2", 20_000L, "Bench-Press!"))
        repository.saveCompletedWorkout(repeatableGraph("s3", 30_000L, "Back Squat"))

        val updatedRows = repository.renameExercises(mapOf("benchpress" to "Barbell Bench Press"))

        assertEquals(2, updatedRows)
        assertEquals("Barbell Bench Press", repository.graph("s1")?.exercises?.single()?.exercise?.name)
        assertEquals("Barbell Bench Press", repository.graph("s2")?.exercises?.single()?.exercise?.name)
        assertEquals("Back Squat", repository.graph("s3")?.exercises?.single()?.exercise?.name)
    }

    private fun repeatableGraph(
        id: String,
        completedAtMillis: Long,
        exerciseName: String = "Press",
    ) = WorkoutHistoryGraph(
        session = WorkoutHistorySession(
            id = id,
            sourceRoutineId = "routine-$id",
            routineName = id,
            startedAtMillis = completedAtMillis - 1_000L,
            completedAtMillis = completedAtMillis,
            durationMillis = 1_000L,
            note = "",
            completedSetCount = 1,
            totalVolumeKg = 50.0,
            isLegacySummaryOnly = false,
        ),
        exercises = listOf(
            WorkoutHistoryExerciseGraph(
                exercise = WorkoutHistoryExercise(
                    id = "exercise-$id",
                    sessionId = id,
                    sourceRoutineExerciseId = "source-$id",
                    name = exerciseName,
                    note = "",
                    restSeconds = 90,
                    position = 0,
                ),
                sets = listOf(
                    WorkoutHistorySet(
                        id = "set-$id",
                        workoutExerciseId = "exercise-$id",
                        position = 0,
                        weightKg = 50.0,
                        repetitions = 5,
                    ),
                ),
            ),
        ),
        photos = emptyList(),
    )

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