package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.LegacyRoutineLibraryResult
import com.yifeng.commandcenter.data.LegacyRoutineLibrarySource
import com.yifeng.commandcenter.data.RoutineEntity
import com.yifeng.commandcenter.data.RoutineExerciseEntity
import com.yifeng.commandcenter.data.RoutineFolderEntity
import com.yifeng.commandcenter.data.RoutineFolderWithRoutines
import com.yifeng.commandcenter.data.RoutineLibraryDao
import com.yifeng.commandcenter.data.RoutineLibraryMetadataEntity
import com.yifeng.commandcenter.data.RoutineLibrarySummaryRow
import com.yifeng.commandcenter.data.RoutineExerciseWithSets
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.RoutineSetEntity
import com.yifeng.commandcenter.data.RoutineWithExercises
import com.yifeng.commandcenter.data.toModel
import com.yifeng.commandcenter.data.WorkoutPlanDto
import com.yifeng.commandcenter.data.WorkoutPlanExerciseDto
import com.yifeng.commandcenter.data.WorkoutPlanFolderDto
import com.yifeng.commandcenter.data.WorkoutPlanIdentity
import com.yifeng.commandcenter.data.WorkoutPlanParser
import com.yifeng.commandcenter.data.WorkoutPlanRoutineDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutPlanImportTest {

    private lateinit var fakeDao: FakeRoutineLibraryDao
    private lateinit var repository: RoutineRepository
    private var libraryChangedNotifications = 0

    @Before
    fun setUp() {
        fakeDao = FakeRoutineLibraryDao()
        libraryChangedNotifications = 0
        repository = RoutineRepository.forTesting(
            dao = fakeDao,
            legacySource = LegacyRoutineLibrarySource { LegacyRoutineLibraryResult.Ready(emptyList()) },
            onLibraryChanged = { libraryChangedNotifications++ },
        )
    }

    private fun samplePlan(
        programId: String = "test-program",
        programName: String = "Test Program",
        notes: String = "HEAVY: 1×4–6 @ RPE 8\nBACKOFF: 3×5–8 @ RPE 7.5",
    ): WorkoutPlanDto = WorkoutPlanDto(
        format = "hevynt-workout-plan",
        version = 1,
        programId = programId,
        programName = programName,
        folder = WorkoutPlanFolderDto(
            name = programName,
            routines = listOf(
                WorkoutPlanRoutineDto(
                    id = "routine-1",
                    name = "Day 1",
                    exercises = listOf(
                        WorkoutPlanExerciseDto(
                            name = "Bench Press",
                            notes = notes,
                            restSeconds = 180,
                            sets = 4,
                        ),
                        WorkoutPlanExerciseDto(
                            name = "Barbell Row",
                            notes = "3×8",
                            restSeconds = 120,
                            sets = 3,
                        ),
                    ),
                ),
                WorkoutPlanRoutineDto(
                    id = "routine-2",
                    name = "Day 2",
                    exercises = listOf(
                        WorkoutPlanExerciseDto(
                            name = "Squat",
                            notes = "Top set @ RPE 8",
                            restSeconds = 240,
                            sets = 3,
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun import_creates_correct_folder_routines_exercises_and_sets() = runBlocking {
        val plan = samplePlan()
        val success = repository.importWorkoutPlan(plan, replaceExisting = false)

        assertTrue(success)
        assertEquals(1, libraryChangedNotifications)

        val folderId = WorkoutPlanIdentity.programFolderId(plan.programId)
        val folder = fakeDao.folderById(folderId)
        assertNotNull(folder)
        assertEquals("Test Program", folder!!.name)

        val routines = fakeDao.routinesForFolder(folderId)
        assertEquals(2, routines.size)
        assertEquals("Day 1", routines[0].name)
        assertEquals("Day 2", routines[1].name)

        val day1Exercises = fakeDao.exercisesForRoutine(routines[0].id)
        assertEquals(2, day1Exercises.size)
        assertEquals("Bench Press", day1Exercises[0].name)
        assertEquals(
            "HEAVY: 1×4–6 @ RPE 8\nBACKOFF: 3×5–8 @ RPE 7.5",
            day1Exercises[0].notes,
        )
        assertEquals(180, day1Exercises[0].restSeconds)

        val benchSets = fakeDao.setsForExercise(day1Exercises[0].id)
        assertEquals(4, benchSets.size)
        assertTrue(benchSets.all { it.weight.isEmpty() && it.repetitions.isEmpty() })

        assertEquals(folderId, fakeDao.getMetadataValue(WorkoutPlanIdentity.programMetadataKey(plan.programId)))
    }

    @Test
    fun import_preserves_existing_unrelated_folders_and_routines() = runBlocking {
        fakeDao.insertFolders(
            listOf(
                RoutineFolderEntity(id = "unrelated-folder", name = "My Manual Routines", position = 0)
            )
        )
        fakeDao.insertRoutines(
            listOf(
                RoutineEntity(id = "unrelated-routine", folderId = "unrelated-folder", name = "Arms", position = 0)
            )
        )

        val plan = samplePlan()
        val success = repository.importWorkoutPlan(plan, replaceExisting = false)

        assertTrue(success)
        assertNotNull(fakeDao.folderById("unrelated-folder"))
        val unrelatedRoutines = fakeDao.routinesForFolder("unrelated-folder")
        assertEquals(1, unrelatedRoutines.size)
        assertEquals("Arms", unrelatedRoutines[0].name)

        val importedFolderId = WorkoutPlanIdentity.programFolderId(plan.programId)
        val importedFolder = fakeDao.folderById(importedFolderId)
        assertNotNull(importedFolder)
        assertEquals(1, importedFolder!!.position)
    }

    @Test
    fun has_program_detects_already_imported_program() = runBlocking {
        val plan = samplePlan()
        assertFalse(repository.hasProgram(plan.programId))

        repository.importWorkoutPlan(plan, replaceExisting = false)
        assertTrue(repository.hasProgram(plan.programId))
    }

    @Test
    fun importing_duplicate_with_replace_false_does_not_modify_existing() = runBlocking {
        val originalPlan = samplePlan(notes = "Original notes")
        repository.importWorkoutPlan(originalPlan, replaceExisting = false)

        val folderId = WorkoutPlanIdentity.programFolderId(originalPlan.programId)
        val day1Id = fakeDao.routinesForFolder(folderId)[0].id
        val benchExercise = fakeDao.exercisesForRoutine(day1Id)[0]
        assertEquals("Original notes", benchExercise.notes)

        val updatedPlan = samplePlan(notes = "Updated notes")
        val success = repository.importWorkoutPlan(updatedPlan, replaceExisting = false)

        assertFalse(success)
        val benchAfterAttempt = fakeDao.exercisesForRoutine(day1Id)[0]
        assertEquals("Original notes", benchAfterAttempt.notes)
    }

    @Test
    fun importing_duplicate_with_replace_true_replaces_program_content() = runBlocking {
        val originalPlan = samplePlan(notes = "Original notes")
        repository.importWorkoutPlan(originalPlan, replaceExisting = false)

        val folderId = WorkoutPlanIdentity.programFolderId(originalPlan.programId)
        val originalRoutines = fakeDao.routinesForFolder(folderId)
        assertEquals("Original notes", fakeDao.exercisesForRoutine(originalRoutines[0].id)[0].notes)

        val updatedPlan = samplePlan(notes = "Updated notes")
        val success = repository.importWorkoutPlan(updatedPlan, replaceExisting = true)

        assertTrue(success)
        val replacedRoutines = fakeDao.routinesForFolder(folderId)
        assertEquals("Updated notes", fakeDao.exercisesForRoutine(replacedRoutines[0].id)[0].notes)
    }

    @Test
    fun transaction_rolls_back_completely_on_failure() = runBlocking {
        val plan = samplePlan()
        fakeDao.failOnInsertRoutines = true

        val failed = runCatching {
            repository.importWorkoutPlan(plan, replaceExisting = false)
        }.isFailure

        assertTrue(failed)
        val folderId = WorkoutPlanIdentity.programFolderId(plan.programId)
        assertFalse(fakeDao.hasFolder(folderId))
        assertTrue(fakeDao.routinesForFolder(folderId).isEmpty())
    }

    @Test
    fun imported_routine_maps_to_active_workout_with_empty_sets_and_preserved_notes() = runBlocking {
        val file = java.io.File("../sample-plans/powerbuilding-4-day.json")
            .takeIf { it.exists() }
            ?: java.io.File("sample-plans/powerbuilding-4-day.json")
        val plan = (WorkoutPlanParser.parse(file.readText()) as com.yifeng.commandcenter.data.WorkoutPlanParseResult.Success).plan
        val success = repository.importWorkoutPlan(plan, replaceExisting = false)
        assertTrue(success)

        val upperARoutineId = WorkoutPlanIdentity.programRoutineId(plan.programId, "tue-upper-a")
        val routineWithExercises = fakeDao.routine(upperARoutineId)
        assertNotNull(routineWithExercises)

        val routineModel = routineWithExercises!!.toModel()
        assertEquals("TUE — Upper A", routineModel.name)
        assertEquals(7, routineModel.exercises.size)

        val activeWorkout = com.yifeng.commandcenter.data.RoutineWorkoutMapper.fromRoutine(routineModel)
        assertEquals(upperARoutineId, activeWorkout.sourceRoutineId)
        assertEquals("TUE — Upper A", activeWorkout.routineName)
        assertEquals(7, activeWorkout.exercises.size)

        val bench = activeWorkout.exercises.first { it.name == "Bench Press" }
        assertEquals("HEAVY: 1×4–6 @ RPE 8\nBACKOFF: 3×5–8 @ RPE 7–7.5", bench.notes)
        assertEquals(180, bench.restSeconds)
        assertEquals(4, bench.sets.size)
        assertTrue(bench.sets.all { it.weight.isEmpty() && it.reps.isEmpty() && !it.completed })
    }
}

class FakeRoutineLibraryDao : RoutineLibraryDao() {
    private val folders = mutableMapOf<String, RoutineFolderEntity>()
    private val routines = mutableMapOf<String, RoutineEntity>()
    private val exercises = mutableMapOf<String, RoutineExerciseEntity>()
    private val sets = mutableMapOf<String, RoutineSetEntity>()
    private val metadata = mutableMapOf<String, String>()

    var failOnInsertRoutines = false

    fun hasFolder(id: String): Boolean = folders.containsKey(id)
    fun routinesForFolder(folderId: String): List<RoutineEntity> =
        routines.values.filter { it.folderId == folderId }.sortedBy { it.position }
    fun exercisesForRoutine(routineId: String): List<RoutineExerciseEntity> =
        exercises.values.filter { it.routineId == routineId }.sortedBy { it.position }
    fun setsForExercise(exerciseId: String): List<RoutineSetEntity> =
        sets.values.filter { it.exerciseId == exerciseId }.sortedBy { it.position }

    override fun observeSummaries(): Flow<List<RoutineLibrarySummaryRow>> = flowOf(emptyList())
    override suspend fun summaries(): List<RoutineLibrarySummaryRow> = emptyList()
    override fun observeLibrary(): Flow<List<RoutineFolderWithRoutines>> = flowOf(emptyList())
    override suspend fun library(): List<RoutineFolderWithRoutines> = emptyList()
    override suspend fun routine(routineId: String): RoutineWithExercises? {
        val routine = routines[routineId] ?: return null
        val relatedExercises = exercises.values.filter { it.routineId == routineId }
            .sortedBy { it.position }
            .map { exercise ->
                val relatedSets = sets.values.filter { it.exerciseId == exercise.id }
                    .sortedBy { it.position }
                RoutineExerciseWithSets(exercise, relatedSets)
            }
        return RoutineWithExercises(routine, relatedExercises)
    }

    public override suspend fun insertFolders(folders: List<RoutineFolderEntity>) {
        folders.forEach { this.folders[it.id] = it }
    }

    public override suspend fun insertRoutines(routines: List<RoutineEntity>) {
        if (failOnInsertRoutines) throw IllegalStateException("Simulated database failure during routine insert")
        routines.forEach { this.routines[it.id] = it }
    }

    public override suspend fun insertExercises(exercises: List<RoutineExerciseEntity>) {
        exercises.forEach { this.exercises[it.id] = it }
    }

    public override suspend fun insertSets(sets: List<RoutineSetEntity>) {
        sets.forEach { this.sets[it.id] = it }
    }

    public override suspend fun putMetadata(metadata: RoutineLibraryMetadataEntity) {
        this.metadata[metadata.key] = metadata.value
    }

    public override suspend fun metadataValue(key: String): String? = metadata[key]
    fun getMetadataValue(key: String): String? = metadata[key]

    override suspend fun upsertFolder(folder: RoutineFolderEntity) {
        folders[folder.id] = folder
    }

    override suspend fun updateFolderName(folderId: String, name: String): Int {
        val existing = folders[folderId] ?: return 0
        folders[folderId] = existing.copy(name = name)
        return 1
    }

    override suspend fun deleteFolderById(folderId: String): Int {
        if (!folders.containsKey(folderId)) return 0
        folders.remove(folderId)
        val routineIds = routines.values.filter { it.folderId == folderId }.map { it.id }
        routineIds.forEach { deleteRoutineById(it) }
        return 1
    }

    override suspend fun deleteRoutineById(routineId: String): Int {
        if (!routines.containsKey(routineId)) return 0
        routines.remove(routineId)
        val exerciseIds = exercises.values.filter { it.routineId == routineId }.map { it.id }
        exerciseIds.forEach { exerciseId ->
            exercises.remove(exerciseId)
            sets.values.filter { it.exerciseId == exerciseId }.forEach { sets.remove(it.id) }
        }
        return 1
    }

    override suspend fun updateRoutinePosition(folderId: String, routineId: String, position: Int): Int {
        val current = routines[routineId]?.takeIf { it.folderId == folderId } ?: return 0
        routines[routineId] = current.copy(position = position)
        return 1
    }

    override suspend fun maxFolderPosition(): Int? = folders.values.maxOfOrNull { it.position }
    override suspend fun maxRoutinePosition(folderId: String): Int? =
        routines.values.filter { it.folderId == folderId }.maxOfOrNull { it.position }
    override suspend fun routinePosition(routineId: String): Int? = routines[routineId]?.position

    override suspend fun deleteFolders() {
        folders.clear()
        routines.clear()
        exercises.clear()
        sets.clear()
    }

    override suspend fun folderById(folderId: String): RoutineFolderEntity? = folders[folderId]

    override suspend fun importProgram(
        folder: RoutineFolderEntity,
        routines: List<RoutineEntity>,
        exercises: List<RoutineExerciseEntity>,
        sets: List<RoutineSetEntity>,
        metadataKey: String,
        replaceExisting: Boolean,
    ): Boolean {
        // Snapshot for transaction simulation
        val snapFolders = folders.toMap()
        val snapRoutines = routinesForFolder(folder.id).associateBy { it.id }
        val snapExercises = this.exercises.toMap()
        val snapSets = this.sets.toMap()
        val snapMetadata = metadata.toMap()

        try {
            return super.importProgram(folder, routines, exercises, sets, metadataKey, replaceExisting)
        } catch (e: Exception) {
            // Roll back
            folders.clear()
            folders.putAll(snapFolders)
            this.routines.clear()
            this.routines.putAll(snapRoutines)
            this.exercises.clear()
            this.exercises.putAll(snapExercises)
            this.sets.clear()
            this.sets.putAll(snapSets)
            metadata.clear()
            metadata.putAll(snapMetadata)
            throw e
        }
    }
}
