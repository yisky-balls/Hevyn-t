package com.yifeng.commandcenter.data

/**
 * Details of a single routine where an exercise is used.
 */
data class ExerciseRoutineUsage(
    val folderId: String,
    val folderName: String,
    val routineId: String,
    val routineName: String,
    val exerciseId: String,
    val exerciseName: String,
    val notes: String,
    val restSeconds: Int,
    val setCount: Int,
)

/**
 * Aggregated summary of a saved exercise across all routines in the library.
 */
data class SavedExerciseSummary(
    val canonicalName: String,
    val normalizedKey: String,
    val notes: String,
    val restSeconds: Int,
    val usages: List<ExerciseRoutineUsage>,
    val hasDuplicateInSameRoutine: Boolean,
    val hasCasingOrWhitespaceVariants: Boolean,
    val variantNames: List<String>,
) {
    val totalRoutinesCount: Int get() = usages.map { it.routineId }.distinct().size
    val totalInstancesCount: Int get() = usages.size
    val isDuplicate: Boolean get() = hasDuplicateInSameRoutine || hasCasingOrWhitespaceVariants
}

/**
 * Working draft state for managing saved exercises before committing changes to Room.
 */
data class SavedExercisesDraft(
    val initialFolders: List<RoutineFolder>,
    val currentFolders: List<RoutineFolder>,
) {
    val hasChanges: Boolean get() = initialFolders != currentFolders
    val exercises: List<SavedExerciseSummary> get() = SavedExercisesOperations.extractSavedExercises(currentFolders)
    val duplicateCount: Int get() = SavedExercisesOperations.countDuplicates(currentFolders)
}
