package com.yifeng.commandcenter.data

data class WorkoutPlanExerciseDto(
    val name: String,
    val notes: String = "",
    val restSeconds: Int = 180,
    val sets: Int = 3,
)

data class WorkoutPlanRoutineDto(
    val id: String,
    val name: String,
    val exercises: List<WorkoutPlanExerciseDto>,
)

data class WorkoutPlanFolderDto(
    val name: String,
    val routines: List<WorkoutPlanRoutineDto>,
)

data class WorkoutPlanDto(
    val format: String,
    val version: Int,
    val programId: String,
    val programName: String,
    val folder: WorkoutPlanFolderDto,
) {
    val totalRoutines: Int get() = folder.routines.size
    val totalExercises: Int get() = folder.routines.sumOf { it.exercises.size }
    val totalSets: Int get() = folder.routines.sumOf { routine -> routine.exercises.sumOf { it.sets } }
}

data class WorkoutPlanPreview(
    val plan: WorkoutPlanDto,
    val alreadyExists: Boolean,
) {
    val programName: String get() = plan.programName
    val routineCount: Int get() = plan.totalRoutines
    val exerciseCount: Int get() = plan.totalExercises
    val totalSets: Int get() = plan.totalSets
    val routineNames: List<String> get() = plan.folder.routines.map { it.name }
}

sealed interface WorkoutPlanParseResult {
    data class Success(val plan: WorkoutPlanDto) : WorkoutPlanParseResult
    data class Failure(val error: String) : WorkoutPlanParseResult
}

object WorkoutPlanIdentity {
    fun programFolderId(programId: String): String = stableId("program-folder:$programId")

    fun programRoutineId(programId: String, routineId: String): String =
        stableId("program-routine:$programId:$routineId")

    fun programExerciseId(programId: String, routineId: String, exerciseIndex: Int): String =
        stableId("program-exercise:$programId:$routineId:$exerciseIndex")

    fun programSetId(programId: String, routineId: String, exerciseIndex: Int, setIndex: Int): String =
        stableId("program-set:$programId:$routineId:$exerciseIndex:$setIndex")

    fun programMetadataKey(programId: String): String = "imported_program:$programId"

    private fun stableId(value: String): String = java.util.UUID.nameUUIDFromBytes(
        "hevynt-workout-plan:$value".toByteArray(java.nio.charset.StandardCharsets.UTF_8),
    ).toString()
}
