package com.yifeng.commandcenter.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_routine_id") val sourceRoutineId: String?,
    @ColumnInfo(name = "routine_name") val routineName: String,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    @ColumnInfo(name = "completed_at_millis", index = true) val completedAtMillis: Long,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    val note: String,
    @ColumnInfo(name = "completed_set_count") val completedSetCount: Int,
    @ColumnInfo(name = "total_volume_kg") val totalVolumeKg: Double,
    @ColumnInfo(name = "is_legacy_summary_only") val isLegacySummaryOnly: Boolean,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "source_routine_exercise_id") val sourceRoutineExerciseId: String?,
    val name: String,
    val note: String,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    val position: Int,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workout_exercise_id")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: String,
    val position: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val repetitions: Int?,
)

@Entity(
    tableName = "workout_photos",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class WorkoutPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    val position: Int,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
)

@Entity(tableName = "workout_history_metadata")
data class WorkoutHistoryMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)