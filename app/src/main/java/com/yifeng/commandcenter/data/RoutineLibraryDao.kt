package com.yifeng.commandcenter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoutineLibraryDao {
    @Query(
        """
        SELECT
            folders.id AS folder_id,
            folders.name AS folder_name,
            routines.id AS routine_id,
            routines.name AS routine_name,
            COUNT(routine_exercises.id) AS exercise_count
        FROM routine_folders AS folders
        LEFT JOIN routines ON routines.folder_id = folders.id
        LEFT JOIN routine_exercises ON routine_exercises.routine_id = routines.id
        GROUP BY folders.id, folders.name, folders.position,
                 routines.id, routines.name, routines.position
        ORDER BY folders.position ASC, folders.id ASC,
                 routines.position ASC, routines.id ASC
        """,
    )
    abstract fun observeSummaries(): Flow<List<RoutineLibrarySummaryRow>>

    @Query(
        """
        SELECT
            folders.id AS folder_id,
            folders.name AS folder_name,
            routines.id AS routine_id,
            routines.name AS routine_name,
            COUNT(routine_exercises.id) AS exercise_count
        FROM routine_folders AS folders
        LEFT JOIN routines ON routines.folder_id = folders.id
        LEFT JOIN routine_exercises ON routine_exercises.routine_id = routines.id
        GROUP BY folders.id, folders.name, folders.position,
                 routines.id, routines.name, routines.position
        ORDER BY folders.position ASC, folders.id ASC,
                 routines.position ASC, routines.id ASC
        """,
    )
    abstract suspend fun summaries(): List<RoutineLibrarySummaryRow>

    @Transaction
    @Query("SELECT * FROM routine_folders ORDER BY position ASC, id ASC")
    abstract fun observeLibrary(): Flow<List<RoutineFolderWithRoutines>>

    @Transaction
    @Query("SELECT * FROM routine_folders ORDER BY position ASC, id ASC")
    abstract suspend fun library(): List<RoutineFolderWithRoutines>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    abstract suspend fun routine(routineId: String): RoutineWithExercises?

    @Insert
    protected abstract suspend fun insertFolders(folders: List<RoutineFolderEntity>)

    @Insert
    protected abstract suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Insert
    protected abstract suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Insert
    protected abstract suspend fun insertSets(sets: List<RoutineSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putMetadata(metadata: RoutineLibraryMetadataEntity)

    @Query("SELECT value FROM routine_library_metadata WHERE `key` = :key")
    protected abstract suspend fun metadataValue(key: String): String?

    suspend fun isLegacyImportComplete(): Boolean =
        metadataValue(LEGACY_IMPORT_KEY) == IMPORT_COMPLETE

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertFolder(folder: RoutineFolderEntity)

    @Query("UPDATE routine_folders SET name = :name WHERE id = :folderId")
    abstract suspend fun updateFolderName(folderId: String, name: String): Int

    @Query("DELETE FROM routine_folders WHERE id = :folderId")
    abstract suspend fun deleteFolderById(folderId: String): Int

    @Query("DELETE FROM routines WHERE id = :routineId")
    abstract suspend fun deleteRoutineById(routineId: String): Int

    @Query("SELECT MAX(position) FROM routine_folders")
    abstract suspend fun maxFolderPosition(): Int?

    @Query("SELECT MAX(position) FROM routines WHERE folder_id = :folderId")
    abstract suspend fun maxRoutinePosition(folderId: String): Int?

    @Query("SELECT position FROM routines WHERE id = :routineId")
    abstract suspend fun routinePosition(routineId: String): Int?

    @Transaction
    open suspend fun upsertRoutine(
        folderId: String,
        routine: Routine,
        existingRoutineId: String? = null,
    ) {
        val targetRoutineId = existingRoutineId ?: routine.id
        val position = routinePosition(targetRoutineId) ?: ((maxRoutinePosition(folderId) ?: -1) + 1)
        deleteRoutineById(targetRoutineId)
        insertRoutines(listOf(RoutineEntity(routine.id, folderId, routine.name, position)))
        val exerciseRows = mutableListOf<RoutineExerciseEntity>()
        val setRows = mutableListOf<RoutineSetEntity>()
        routine.exercises.forEachIndexed { exercisePosition, exercise ->
            exerciseRows += RoutineExerciseEntity(
                id = exercise.id,
                routineId = routine.id,
                name = exercise.name,
                notes = exercise.notes,
                restSeconds = exercise.restSeconds,
                position = exercisePosition,
            )
            exercise.sets.forEachIndexed { setPosition, set ->
                setRows += RoutineSetEntity(
                    id = set.id,
                    exerciseId = exercise.id,
                    weight = set.weight,
                    repetitions = set.reps,
                    position = setPosition,
                )
            }
        }
        if (exerciseRows.isNotEmpty()) insertExercises(exerciseRows)
        if (setRows.isNotEmpty()) insertSets(setRows)
    }

    @Query("DELETE FROM routine_folders")
    protected abstract suspend fun deleteFolders()

    @Transaction
    open suspend fun importLegacyOnce(folders: List<RoutineFolder>): Boolean {
        if (isLegacyImportComplete()) return false
        replaceLibraryRows(folders)
        putMetadata(RoutineLibraryMetadataEntity(LEGACY_IMPORT_KEY, IMPORT_COMPLETE))
        return true
    }

    @Transaction
    open suspend fun replaceLibrary(folders: List<RoutineFolder>) {
        replaceLibraryRows(folders)
        putMetadata(RoutineLibraryMetadataEntity(LEGACY_IMPORT_KEY, IMPORT_COMPLETE))
    }

    private suspend fun replaceLibraryRows(folders: List<RoutineFolder>) {
        deleteFolders()
        val folderRows = mutableListOf<RoutineFolderEntity>()
        val routineRows = mutableListOf<RoutineEntity>()
        val exerciseRows = mutableListOf<RoutineExerciseEntity>()
        val setRows = mutableListOf<RoutineSetEntity>()
        folders.forEachIndexed { folderPosition, folder ->
            folderRows += RoutineFolderEntity(folder.id, folder.name, folderPosition)
            folder.routines.forEachIndexed { routinePosition, routine ->
                routineRows += RoutineEntity(routine.id, folder.id, routine.name, routinePosition)
                routine.exercises.forEachIndexed { exercisePosition, exercise ->
                    exerciseRows += RoutineExerciseEntity(
                        id = exercise.id,
                        routineId = routine.id,
                        name = exercise.name,
                        notes = exercise.notes,
                        restSeconds = exercise.restSeconds,
                        position = exercisePosition,
                    )
                    exercise.sets.forEachIndexed { setPosition, set ->
                        setRows += RoutineSetEntity(
                            id = set.id,
                            exerciseId = exercise.id,
                            weight = set.weight,
                            repetitions = set.reps,
                            position = setPosition,
                        )
                    }
                }
            }
        }
        if (folderRows.isNotEmpty()) insertFolders(folderRows)
        if (routineRows.isNotEmpty()) insertRoutines(routineRows)
        if (exerciseRows.isNotEmpty()) insertExercises(exerciseRows)
        if (setRows.isNotEmpty()) insertSets(setRows)
    }

    private companion object {
        const val LEGACY_IMPORT_KEY = "shared_preferences_import_v1"
        const val IMPORT_COMPLETE = "complete"
    }
}