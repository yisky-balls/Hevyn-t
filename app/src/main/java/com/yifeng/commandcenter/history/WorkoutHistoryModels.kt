package com.yifeng.commandcenter.history

import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import java.nio.charset.StandardCharsets
import java.util.UUID

data class WorkoutHistorySession(
    val id: String,
    val sourceRoutineId: String?,
    val routineName: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long,
    val durationMillis: Long,
    val note: String,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
    val isLegacySummaryOnly: Boolean,
)

data class WorkoutHistoryExercise(
    val id: String,
    val sessionId: String,
    val sourceRoutineExerciseId: String?,
    val name: String,
    val note: String,
    val restSeconds: Int,
    val position: Int,
)

data class WorkoutHistorySet(
    val id: String,
    val workoutExerciseId: String,
    val position: Int,
    val weightKg: Double?,
    val repetitions: Int?,
    val completed: Boolean = true,
)

data class WorkoutHistoryPhoto(
    val id: String,
    val sessionId: String,
    val relativePath: String,
    val position: Int,
    val createdAtMillis: Long,
)

data class WorkoutHistoryExerciseGraph(
    val exercise: WorkoutHistoryExercise,
    val sets: List<WorkoutHistorySet>,
)

data class WorkoutHistoryGraph(
    val session: WorkoutHistorySession,
    val exercises: List<WorkoutHistoryExerciseGraph>,
    val photos: List<WorkoutHistoryPhoto>,
)

data class WorkoutJournalEntry(
    val session: WorkoutHistorySession,
    val photo: WorkoutHistoryPhoto?,
)

data class MonthlyWorkoutAggregate(
    val workoutCount: Int,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class WorkoutDayCount(
    val dayStartMillis: Long,
    val count: Int,
)

data class PreviousSetPerformance(
    val weightKg: Double?,
    val repetitions: Int?,
)

/** Compact joined history row used by the active logger; no complete workout graph is hydrated. */
data class HistoricalExerciseSetRow(
    val workoutExerciseId: String,
    val sourceRoutineExerciseId: String?,
    val exerciseName: String,
    val completedAtMillis: Long,
    val setPosition: Int,
    val weightKg: Double?,
    val repetitions: Int?,
)

data class CompletedWorkoutDraft(
    val graph: WorkoutHistoryGraph,
    val unfinishedSetCount: Int,
)

object CompletedWorkoutMapper {
    fun fromActiveWorkout(
        workout: ActiveWorkout,
        completedAtMillis: Long,
        note: String = "",
    ): CompletedWorkoutDraft {
        val metrics = WorkoutMetricsCalculator.calculate(workout)
        // A finished workout is also the reusable Last Workout source. Persist the complete
        // immutable snapshot; filtering unchecked rows here used to silently remove whole
        // exercises from Repeat Last Workout.
        val exercises = workout.exercises.mapIndexed { exercisePosition, exercise ->
            val historyExerciseId = UUID.randomUUID().toString()
            WorkoutHistoryExerciseGraph(
                exercise = WorkoutHistoryExercise(
                    id = historyExerciseId,
                    sessionId = workout.id,
                    sourceRoutineExerciseId = exercise.sourceRoutineExerciseId,
                    name = exercise.name,
                    note = exercise.notes,
                    restSeconds = exercise.restSeconds,
                    position = exercisePosition,
                ),
                sets = exercise.sets.mapIndexed { setPosition, set ->
                    WorkoutHistorySet(
                        id = UUID.randomUUID().toString(),
                        workoutExerciseId = historyExerciseId,
                        position = setPosition,
                        weightKg = set.weight.toDoubleOrNull(),
                        repetitions = set.reps.toIntOrNull(),
                        completed = set.completed,
                    )
                },
            )
        }
        return CompletedWorkoutDraft(
            graph = WorkoutHistoryGraph(
                session = WorkoutHistorySession(
                    id = workout.id,
                    sourceRoutineId = workout.sourceRoutineId,
                    routineName = workout.routineName,
                    startedAtMillis = workout.startedAtMillis,
                    completedAtMillis = completedAtMillis,
                    durationMillis = (completedAtMillis - workout.startedAtMillis).coerceAtLeast(0L),
                    note = note,
                    completedSetCount = metrics.completedSets,
                    totalVolumeKg = metrics.volumeKg,
                    isLegacySummaryOnly = false,
                ),
                exercises = exercises,
                photos = emptyList(),
            ),
            unfinishedSetCount = workout.exercises.sumOf { exercise ->
                exercise.sets.count { !it.completed }
            },
        )
    }

    fun legacy(summary: LastWorkoutSummary, index: Int): WorkoutHistoryGraph {
        val identity = listOf(
            summary.name,
            summary.completedAtMillis,
            summary.completedSets,
            summary.volumeKg,
            index,
        ).joinToString("|")
        return WorkoutHistoryGraph(
            session = WorkoutHistorySession(
                id = UUID.nameUUIDFromBytes(identity.toByteArray(StandardCharsets.UTF_8)).toString(),
                sourceRoutineId = null,
                routineName = summary.name,
                startedAtMillis = summary.completedAtMillis,
                completedAtMillis = summary.completedAtMillis,
                durationMillis = 0L,
                note = "",
                completedSetCount = summary.completedSets,
                totalVolumeKg = summary.volumeKg,
                isLegacySummaryOnly = true,
            ),
            exercises = emptyList(),
            photos = emptyList(),
        )
    }
}