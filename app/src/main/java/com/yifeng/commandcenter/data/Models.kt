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
    val restSeconds: Int = 180,
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
    val name: String,
    val notes: String = "",
    val restSeconds: Int = 180,
    val sets: List<WorkoutSet> = defaultSets(),
)

data class ActiveWorkout(
    val routineName: String,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val exercises: List<ActiveExercise>,
)

data class LastWorkoutSummary(
    val name: String,
    val completedAtMillis: Long,
    val completedSets: Int,
    val volumeKg: Int,
)

fun defaultSets(): List<WorkoutSet> = listOf(
    WorkoutSet(weight = "60", reps = "8"),
    WorkoutSet(weight = "65", reps = "6"),
    WorkoutSet(weight = "70", reps = "4"),
)