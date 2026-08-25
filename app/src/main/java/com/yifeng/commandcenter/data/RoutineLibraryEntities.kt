package com.yifeng.commandcenter.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routine_folders")
data class RoutineFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = RoutineFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folder_id")],
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "folder_id") val folderId: String,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routine_id")],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    val name: String,
    val notes: String,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    val position: Int,
)

@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exercise_id")],
)
data class RoutineSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val weight: String,
    val repetitions: String,
    val position: Int,
)

@Entity(tableName = "routine_library_metadata")
data class RoutineLibraryMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class RoutineExerciseWithSets(
    @Embedded val exercise: RoutineExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exercise_id",
    )
    val sets: List<RoutineSetEntity>,
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "routine_id",
    )
    val exercises: List<RoutineExerciseWithSets>,
)

/** Row returned by the launcher projection; routine columns are null for an empty folder. */
data class RoutineLibrarySummaryRow(
    @ColumnInfo(name = "folder_id") val folderId: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "routine_name") val routineName: String?,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

data class RoutineFolderWithRoutines(
    @Embedded val folder: RoutineFolderEntity,
    @Relation(
        entity = RoutineEntity::class,
        parentColumn = "id",
        entityColumn = "folder_id",
    )
    val routines: List<RoutineWithExercises>,
)

internal fun List<RoutineLibrarySummaryRow>.toSummaryModels(): List<RoutineFolderSummary> =
    groupBy(RoutineLibrarySummaryRow::folderId).map { (folderId, rows) ->
        RoutineFolderSummary(
            id = folderId,
            name = rows.first().folderName,
            routines = rows.mapNotNull { row ->
                val routineId = row.routineId ?: return@mapNotNull null
                RoutineSummary(
                    id = routineId,
                    folderId = folderId,
                    name = row.routineName.orEmpty(),
                    exerciseCount = row.exerciseCount,
                )
            },
        )
    }

internal fun RoutineWithExercises.toModel() = Routine(
    id = routine.id,
    name = routine.name,
    exercises = exercises
        .sortedWith(compareBy({ it.exercise.position }, { it.exercise.id }))
        .map { relatedExercise ->
            RoutineExercise(
                id = relatedExercise.exercise.id,
                name = relatedExercise.exercise.name,
                notes = relatedExercise.exercise.notes,
                restSeconds = relatedExercise.exercise.restSeconds,
                sets = relatedExercise.sets
                    .sortedWith(compareBy({ it.position }, { it.id }))
                    .map { set ->
                        WorkoutSet(
                            id = set.id,
                            weight = set.weight,
                            reps = set.repetitions,
                            completed = false,
                        )
                    },
            )
        },
)

internal fun RoutineFolderWithRoutines.toModel() = RoutineFolder(
    id = folder.id,
    name = folder.name,
    routines = routines
        .sortedWith(compareBy({ it.routine.position }, { it.routine.id }))
        .map(RoutineWithExercises::toModel),
)