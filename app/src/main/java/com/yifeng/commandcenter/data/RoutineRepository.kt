package com.yifeng.commandcenter.data

import android.content.Context
import android.content.SharedPreferences
import com.yifeng.commandcenter.widget.WorkoutWidgetUpdateDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID

sealed interface RoutineLibraryImportResult {
    data object Imported : RoutineLibraryImportResult
    data object AlreadyImported : RoutineLibraryImportResult
    data class CorruptLegacyData(val cause: Throwable) : RoutineLibraryImportResult
}

class RoutineRepository private constructor(
    private val dao: RoutineLibraryDao,
    private val legacySource: LegacyRoutineLibrarySource,
    private val legacyHistoryStorage: LegacyWorkoutHistoryStorage,
    private val planImporter: WorkoutPlanImporter = WorkoutPlanImporter(dao),
    private val onLibraryChanged: () -> Unit,
) {
    constructor(context: Context) : this(
        dao = RoutineLibraryDatabase.getInstance(context.applicationContext ?: context)
            .routineLibraryDao(),
        legacySource = SharedPreferencesRoutineLibrarySource(context),
        legacyHistoryStorage = LegacyWorkoutHistoryStorage(
            (context.applicationContext ?: context)
                .getSharedPreferences(LegacyWorkoutHistoryStorage.PREFERENCES, Context.MODE_PRIVATE),
        ),
        onLibraryChanged = { WorkoutWidgetUpdateDispatcher.request(context.applicationContext ?: context) },
    )

    /** Lightweight reactive projection used by Home and the Workout launcher. */
    fun observeSummaries(): Flow<List<RoutineFolderSummary>> =
        dao.observeSummaries().map(List<RoutineLibrarySummaryRow>::toSummaryModels)

    suspend fun summaries(): List<RoutineFolderSummary> =
        dao.summaries().toSummaryModels()

    /** Complete hierarchy for editors, mutations, and exercise selection only. */
    fun observeFolders(): Flow<List<RoutineFolder>> = dao.observeLibrary().map { folders ->
        folders
            .sortedWith(compareBy({ it.folder.position }, { it.folder.id }))
            .map(RoutineFolderWithRoutines::toModel)
    }

    /**
     * Imports SharedPreferences exactly once. A corrupt payload is left untouched and the Room
     * completion marker is not written, so a repaired payload can be retried on the next launch.
     */
    suspend fun initialize(): RoutineLibraryImportResult {
        if (dao.isLegacyImportComplete()) return RoutineLibraryImportResult.AlreadyImported
        return when (val legacy = legacySource.read()) {
            is LegacyRoutineLibraryResult.Corrupt ->
                RoutineLibraryImportResult.CorruptLegacyData(legacy.cause)

            is LegacyRoutineLibraryResult.Ready -> {
                if (dao.importLegacyOnce(legacy.folders)) {
                    notifyLibraryChanged()
                    RoutineLibraryImportResult.Imported
                } else {
                    RoutineLibraryImportResult.AlreadyImported
                }
            }
        }
    }

    suspend fun folders(): List<RoutineFolder> = dao.library()
        .sortedWith(compareBy({ it.folder.position }, { it.folder.id }))
        .map(RoutineFolderWithRoutines::toModel)

    suspend fun routine(routineId: String): Routine? = dao.routine(routineId)?.toModel()

    suspend fun createFolder(name: String, id: String = UUID.randomUUID().toString()) {
        val nextPosition = (dao.maxFolderPosition() ?: -1) + 1
        dao.upsertFolder(RoutineFolderEntity(id = id, name = name, position = nextPosition))
        notifyLibraryChanged()
    }

    suspend fun renameFolder(folderId: String, name: String) {
        dao.updateFolderName(folderId, name)
        notifyLibraryChanged()
    }

    suspend fun deleteFolder(folderId: String) {
        dao.deleteFolderById(folderId)
        notifyLibraryChanged()
    }

    suspend fun deleteRoutine(routineId: String) {
        dao.deleteRoutineById(routineId)
        notifyLibraryChanged()
    }

    suspend fun saveRoutine(folderId: String, routine: Routine, existingRoutineId: String? = null) {
        dao.upsertRoutine(folderId, routine, existingRoutineId)
        notifyLibraryChanged()
    }

    suspend fun replaceFolders(folders: List<RoutineFolder>) {
        dao.replaceLibrary(folders)
        notifyLibraryChanged()
    }

    suspend fun hasProgram(programId: String): Boolean =
        planImporter.hasProgram(programId)

    suspend fun importWorkoutPlan(
        plan: WorkoutPlanDto,
        replaceExisting: Boolean = false,
    ): Boolean {
        val success = planImporter.importWorkoutPlan(plan, replaceExisting)
        if (success) {
            notifyLibraryChanged()
        }
        return success
    }

    /** Compatibility for repository instrumentation; production orchestration uses suspend APIs. */
    fun loadFolders(): List<RoutineFolder> = runBlocking(Dispatchers.IO) {
        when (val result = initialize()) {
            is RoutineLibraryImportResult.CorruptLegacyData -> throw result.cause
            RoutineLibraryImportResult.AlreadyImported,
            RoutineLibraryImportResult.Imported,
            -> folders()
        }
    }

    /** Compatibility for repository instrumentation; production orchestration uses suspend APIs. */
    fun saveFolders(folders: List<RoutineFolder>) = runBlocking(Dispatchers.IO) {
        replaceFolders(folders)
    }

    /** Legacy history is read-only input for the independent history-v2 transactional import. */
    fun loadWorkoutHistory(): List<LastWorkoutSummary> =
        legacyHistoryStorage.loadWorkoutHistory()

    fun saveLastWorkout(summary: LastWorkoutSummary) =
        legacyHistoryStorage.saveLastWorkout(summary)

    private fun notifyLibraryChanged() {
        try {
            onLibraryChanged()
        } catch (_: Exception) {
            // Room is authoritative; launcher refresh failures never undo routine changes.
        }
    }

    internal companion object {
        fun forTesting(
            dao: RoutineLibraryDao,
            legacySource: LegacyRoutineLibrarySource,
            onLibraryChanged: () -> Unit = {},
        ) = RoutineRepository(
            dao = dao,
            legacySource = legacySource,
            legacyHistoryStorage = LegacyWorkoutHistoryStorage(null),
            onLibraryChanged = onLibraryChanged,
        )
    }
}