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

/** Lightweight launcher data. Full exercise/set templates are loaded only when needed. */
data class RoutineSummary(
    val id: String,
    val folderId: String,
    val name: String,
    val exerciseCount: Int,
    val lastPerformedAtMillis: Long? = null,
)

data class RoutineFolderSummary(
    val id: String,
    val name: String,
    val routines: List<RoutineSummary> = emptyList(),
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

private val DAY_PREFIX_REGEX = Regex(
    "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY|MON|TUE|WED|THU|FRI|SAT|SUN|DAY\\s*\\d+)\\s*[-—–:.]\\s*",
    RegexOption.IGNORE_CASE,
)

fun cleanRoutineName(name: String): String =
    name.replace(DAY_PREFIX_REGEX, "").trim()