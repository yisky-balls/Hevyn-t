package com.yifeng.commandcenter

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.data.LegacyRoutineLibraryResult
import com.yifeng.commandcenter.data.LegacyRoutineLibrarySource
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineLibraryDatabase
import com.yifeng.commandcenter.data.RoutineLibraryImportResult
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutSet
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineLibraryRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: RoutineLibraryDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoutineLibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun currentPayloadImportsAllFieldsAndPreservesOrder() = runBlocking {
        val expected = orderedLibrary()
        val repository = repositoryWith(LegacyRoutineLibraryResult.Ready(expected))

        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        assertEquals(expected.withoutCompletedSets(), repository.folders())
        assertEquals(expected.withoutCompletedSets(), repository.observeFolders().first())
    }

    @Test
    fun summaryProjectionPreservesFoldersAndCountsWithoutRoutineHydration() = runBlocking {
        val repository = repositoryWith(LegacyRoutineLibraryResult.Ready(orderedLibrary()))
        repository.initialize()

        val summaries = repository.observeSummaries().first()

        assertEquals(summaries, repository.summaries())
        assertEquals(listOf("folder-2", "folder-1"), summaries.map { it.id })
        assertEquals(listOf("routine-2", "routine-1"), summaries.first().routines.map { it.id })
        assertEquals(listOf(1, 0), summaries.first().routines.map { it.exerciseCount })
        assertTrue(summaries.last().routines.isEmpty())
        assertEquals("Row", repository.routine("routine-2")?.exercises?.single()?.name)
    }

    @Test
    fun successfulLibraryWritesRequestWidgetRefresh() = runBlocking {
        var refreshes = 0
        val repository = RoutineRepository.forTesting(
            database.routineLibraryDao(),
            LegacyRoutineLibrarySource {
                LegacyRoutineLibraryResult.Ready(orderedLibrary())
            },
            onLibraryChanged = { refreshes += 1 },
        )

        repository.initialize()
        assertEquals(1, refreshes)

        repository.replaceFolders(listOf(RoutineFolder(id = "replacement", name = "Replacement")))
        assertEquals(2, refreshes)
    }

    @Test
    fun legacyPayloadShapeCanImportBlankSetTemplates() = runBlocking {
        val legacy = listOf(
            RoutineFolder(
                id = "folder",
                name = "Legacy",
                routines = listOf(
                    Routine(
                        id = "routine",
                        name = "Three day",
                        exercises = listOf(
                            RoutineExercise(
                                id = "exercise",
                                name = "Squat",
                                notes = "Pause",
                                restSeconds = 90,
                                sets = List(3) { WorkoutSet(id = "set-$it") },
                            ),
                        ),
                    ),
                ),
            ),
        )
        val repository = repositoryWith(LegacyRoutineLibraryResult.Ready(legacy))

        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        val exercise = repository.folders().single().routines.single().exercises.single()
        assertEquals("Pause", exercise.notes)
        assertEquals(90, exercise.restSeconds)
        assertEquals(listOf("set-0", "set-1", "set-2"), exercise.sets.map { it.id })
    }

    @Test
    fun explicitEmptyLibraryStaysEmptyInsteadOfReceivingDefaults() = runBlocking {
        val repository = repositoryWith(LegacyRoutineLibraryResult.Ready(emptyList()))

        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        assertTrue(repository.folders().isEmpty())
        assertEquals(RoutineLibraryImportResult.AlreadyImported, repository.initialize())
    }

    @Test
    fun corruptPayloadDoesNotWriteRowsOrCompletionMarkerAndCanRetry() = runBlocking {
        var sourceResult: LegacyRoutineLibraryResult =
            LegacyRoutineLibraryResult.Corrupt(IOException("broken json"))
        val repository = RoutineRepository.forTesting(
            database.routineLibraryDao(),
            LegacyRoutineLibrarySource { sourceResult },
        )

        assertTrue(repository.initialize() is RoutineLibraryImportResult.CorruptLegacyData)
        assertTrue(repository.folders().isEmpty())
        assertFalse(database.routineLibraryDao().isLegacyImportComplete())

        sourceResult = LegacyRoutineLibraryResult.Ready(orderedLibrary())
        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        assertEquals(listOf("Second", "First"), repository.folders().map { it.name })
    }

    @Test
    fun transactionalFailureRollsBackAndFixedPayloadRetries() = runBlocking {
        val duplicateIds = listOf(
            RoutineFolder(id = "same", name = "A"),
            RoutineFolder(id = "same", name = "B"),
        )
        var sourceResult: LegacyRoutineLibraryResult = LegacyRoutineLibraryResult.Ready(duplicateIds)
        val repository = RoutineRepository.forTesting(
            database.routineLibraryDao(),
            LegacyRoutineLibrarySource { sourceResult },
        )

        assertTrue(runCatching { repository.initialize() }.isFailure)
        assertTrue(repository.folders().isEmpty())
        assertFalse(database.routineLibraryDao().isLegacyImportComplete())

        sourceResult = LegacyRoutineLibraryResult.Ready(orderedLibrary())
        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        assertEquals(orderedLibrary().withoutCompletedSets(), repository.folders())
    }

    @Test
    fun completedImportNeverReReadsOrOverwritesFromLegacySource() = runBlocking {
        var reads = 0
        var sourceResult: LegacyRoutineLibraryResult = LegacyRoutineLibraryResult.Ready(orderedLibrary())
        val repository = RoutineRepository.forTesting(
            database.routineLibraryDao(),
            LegacyRoutineLibrarySource {
                reads += 1
                sourceResult
            },
        )

        assertEquals(RoutineLibraryImportResult.Imported, repository.initialize())
        repository.replaceFolders(listOf(RoutineFolder(id = "new", name = "Room is authoritative")))
        sourceResult = LegacyRoutineLibraryResult.Corrupt(IOException("stale payload changed"))

        assertEquals(RoutineLibraryImportResult.AlreadyImported, repository.initialize())
        assertEquals(1, reads)
        assertEquals("Room is authoritative", repository.folders().single().name)
    }

    private fun repositoryWith(result: LegacyRoutineLibraryResult) = RoutineRepository.forTesting(
        database.routineLibraryDao(),
        LegacyRoutineLibrarySource { result },
    )

    private fun orderedLibrary() = listOf(
        RoutineFolder(
            id = "folder-2",
            name = "Second",
            routines = listOf(
                Routine(
                    id = "routine-2",
                    name = "B",
                    exercises = listOf(
                        RoutineExercise(
                            id = "exercise-2",
                            name = "Row",
                            notes = "Strict",
                            restSeconds = 75,
                            sets = listOf(
                                WorkoutSet("set-2", "42.5", "8", completed = true),
                                WorkoutSet("set-1", "40", "10"),
                            ),
                        ),
                    ),
                ),
                Routine(id = "routine-1", name = "A", exercises = emptyList()),
            ),
        ),
        RoutineFolder(id = "folder-1", name = "First"),
    )

    private fun List<RoutineFolder>.withoutCompletedSets() = map { folder ->
        folder.copy(routines = folder.routines.map { routine ->
            routine.copy(exercises = routine.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(completed = false) })
            })
        })
    }
}