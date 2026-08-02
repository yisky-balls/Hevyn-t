package com.yifeng.commandcenter.data

import java.util.UUID

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<RoutineExercise>,
)

data class RoutineExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val notes: String = "",
    val restSeconds: Int = 180,
    val sets: List<WorkoutSet> = defaultSets(),
)

data class RoutineFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val routines: List<Routine> = emptyList(),
)

data class WorkoutSet(
    val id: String = UUID.randomUUID().toString(),
    val weight: String = "",
    val reps: String = "",
    val completed: Boolean = false,
)

data class ActiveExercise(
    val id: String = UUID.randomUUID().toString(),
    val sourceRoutineExerciseId: String? = null,
    val name: String,
    val notes: String = "",
    val restSeconds: Int = 180,
    val sets: List<WorkoutSet> = defaultSets(),
)

data class ActiveWorkout(
    val id: String = UUID.randomUUID().toString(),
    val sourceRoutineId: String? = null,
    val routineName: String,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val exercises: List<ActiveExercise>,
)

data class RoutineBuildTarget(
    val folderId: String,
    val routineId: String?,
)

data class ActiveWorkoutSession(
    val workout: ActiveWorkout,
    val routineBuildTarget: RoutineBuildTarget? = null,
)

data class LastWorkoutSummary(
    val name: String,
    val completedAtMillis: Long,
    val completedSets: Int,
    val volumeKg: Double,
)

fun defaultSets(): List<WorkoutSet> = listOf(
    WorkoutSet(),
    WorkoutSet(),
    WorkoutSet(),
)