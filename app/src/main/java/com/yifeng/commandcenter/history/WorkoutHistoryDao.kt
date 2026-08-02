package com.yifeng.commandcenter.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutHistoryDao {
    @Insert
    protected abstract suspend fun insertSession(entity: WorkoutSessionEntity)

    @Insert
    protected abstract suspend fun insertExercises(entities: List<WorkoutExerciseEntity>)

    @Insert
    protected abstract suspend fun insertSets(entities: List<WorkoutSetEntity>)

    @Insert
    protected abstract suspend fun insertPhotos(entities: List<WorkoutPhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putMetadata(entity: WorkoutHistoryMetadataEntity)

    @Query("SELECT value FROM workout_history_metadata WHERE `key` = :key")
    protected abstract suspend fun metadataValue(key: String): String?

    @Query("SELECT * FROM workout_sessions ORDER BY completed_at_millis DESC, id DESC")
    abstract fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY completed_at_millis DESC, id DESC")
    abstract suspend fun sessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_photos ORDER BY session_id ASC, position ASC, created_at_millis ASC, id ASC")
    abstract fun observePhotos(): Flow<List<WorkoutPhotoEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    protected abstract suspend fun session(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_exercises WHERE session_id = :sessionId ORDER BY position ASC, id ASC")
    protected abstract suspend fun exercises(sessionId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE workout_exercise_id = :exerciseId ORDER BY position ASC, id ASC")
    protected abstract suspend fun sets(exerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_photos WHERE session_id = :sessionId ORDER BY position ASC, created_at_millis ASC, id ASC")
    protected abstract suspend fun photos(sessionId: String): List<WorkoutPhotoEntity>

    @Query("UPDATE workout_sessions SET note = :note WHERE id = :sessionId")
    abstract suspend fun updateNote(sessionId: String, note: String): Int

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun deleteSessionRow(sessionId: String): Int

    @Query("DELETE FROM workout_photos WHERE id = :photoId")
    abstract suspend fun deletePhotoRow(photoId: String): Int

    @Query("DELETE FROM workout_photos WHERE session_id = :sessionId")
    protected abstract suspend fun deleteSessionPhotoRows(sessionId: String): Int

    @Query("SELECT relative_path FROM workout_photos WHERE session_id = :sessionId")
    protected abstract suspend fun sessionPhotoPaths(sessionId: String): List<String>

    @Transaction
    open suspend fun insertGraph(graph: WorkoutHistoryGraph) {
        insertSession(graph.session.toEntity())
        insertExercises(graph.exercises.map { it.exercise.toEntity() })
        insertSets(graph.exercises.flatMap { exercise -> exercise.sets.map { it.toEntity() } })
        insertPhotos(graph.photos.map { it.toEntity() })
    }

    @Transaction
    open suspend fun importLegacyOnce(graphs: List<WorkoutHistoryGraph>): Boolean {
        if (metadataValue(LEGACY_IMPORT_KEY) == LEGACY_IMPORT_COMPLETE) return false
        graphs.forEach { insertGraph(it) }
        putMetadata(WorkoutHistoryMetadataEntity(LEGACY_IMPORT_KEY, LEGACY_IMPORT_COMPLETE))
        return true
    }

    @Transaction
    open suspend fun graph(sessionId: String): WorkoutHistoryGraph? {
        val session = session(sessionId) ?: return null
        val exercises = exercises(sessionId).map { exercise ->
            WorkoutHistoryExerciseGraph(
                exercise = exercise.toModel(),
                sets = sets(exercise.id).map(WorkoutSetEntity::toModel),
            )
        }
        return WorkoutHistoryGraph(
            session = session.toModel(),
            exercises = exercises,
            photos = photos(sessionId).map(WorkoutPhotoEntity::toModel),
        )
    }

    @Transaction
    open suspend fun replaceVisiblePhoto(photo: WorkoutHistoryPhoto): List<String> {
        val replacedPaths = sessionPhotoPaths(photo.sessionId)
        deleteSessionPhotoRows(photo.sessionId)
        insertPhotos(listOf(photo.toEntity()))
        return replacedPaths
    }

    @Transaction
    open suspend fun removeVisiblePhoto(sessionId: String): List<String> {
        val removedPaths = sessionPhotoPaths(sessionId)
        deleteSessionPhotoRows(sessionId)
        return removedPaths
    }

    @Transaction
    open suspend fun deleteSession(sessionId: String): List<String> {
        val removedPaths = sessionPhotoPaths(sessionId)
        deleteSessionRow(sessionId)
        return removedPaths
    }

    private companion object {
        const val LEGACY_IMPORT_KEY = "legacy_summary_import_v1"
        const val LEGACY_IMPORT_COMPLETE = "complete"
    }
}

internal fun WorkoutHistorySession.toEntity() = WorkoutSessionEntity(
    id = id,
    sourceRoutineId = sourceRoutineId,
    routineName = routineName,
    startedAtMillis = startedAtMillis,
    completedAtMillis = completedAtMillis,
    durationMillis = durationMillis,
    note = note,
    completedSetCount = completedSetCount,
    totalVolumeKg = totalVolumeKg,
    isLegacySummaryOnly = isLegacySummaryOnly,
)

internal fun WorkoutSessionEntity.toModel() = WorkoutHistorySession(
    id = id,
    sourceRoutineId = sourceRoutineId,
    routineName = routineName,
    startedAtMillis = startedAtMillis,
    completedAtMillis = completedAtMillis,
    durationMillis = durationMillis,
    note = note,
    completedSetCount = completedSetCount,
    totalVolumeKg = totalVolumeKg,
    isLegacySummaryOnly = isLegacySummaryOnly,
)

internal fun WorkoutHistoryExercise.toEntity() = WorkoutExerciseEntity(
    id = id,
    sessionId = sessionId,
    sourceRoutineExerciseId = sourceRoutineExerciseId,
    name = name,
    note = note,
    restSeconds = restSeconds,
    position = position,
)

internal fun WorkoutExerciseEntity.toModel() = WorkoutHistoryExercise(
    id = id,
    sessionId = sessionId,
    sourceRoutineExerciseId = sourceRoutineExerciseId,
    name = name,
    note = note,
    restSeconds = restSeconds,
    position = position,
)

internal fun WorkoutHistorySet.toEntity() = WorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    weightKg = weightKg,
    repetitions = repetitions,
)

internal fun WorkoutSetEntity.toModel() = WorkoutHistorySet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    weightKg = weightKg,
    repetitions = repetitions,
)

internal fun WorkoutHistoryPhoto.toEntity() = WorkoutPhotoEntity(
    id = id,
    sessionId = sessionId,
    relativePath = relativePath,
    position = position,
    createdAtMillis = createdAtMillis,
)

internal fun WorkoutPhotoEntity.toModel() = WorkoutHistoryPhoto(
    id = id,
    sessionId = sessionId,
    relativePath = relativePath,
    position = position,
    createdAtMillis = createdAtMillis,
)