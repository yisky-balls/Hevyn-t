package com.yifeng.commandcenter.data

import com.yifeng.commandcenter.history.normalizeExerciseName

object SavedExercisesOperations {

    /**
     * Extracts and aggregates all distinct saved exercises across the routine library.
     */
    fun extractSavedExercises(folders: List<RoutineFolder>): List<SavedExerciseSummary> {
        val usageMap = mutableMapOf<String, MutableList<ExerciseRoutineUsage>>()

        folders.forEach { folder ->
            folder.routines.forEach { routine ->
                routine.exercises.forEach { exercise ->
                    val trimmedName = exercise.name.trim()
                    val key = normalizeExerciseName(trimmedName)
                    if (key.isNotEmpty()) {
                        val usage = ExerciseRoutineUsage(
                            folderId = folder.id,
                            folderName = folder.name,
                            routineId = routine.id,
                            routineName = routine.name,
                            exerciseId = exercise.id,
                            exerciseName = trimmedName,
                            notes = exercise.notes,
                            restSeconds = exercise.restSeconds,
                            setCount = exercise.sets.size,
                        )
                        usageMap.getOrPut(key) { mutableListOf() }.add(usage)
                    }
                }
            }
        }

        return usageMap.values.map { usages ->
            val distinctRawNames = usages.map { it.exerciseName }.distinct()
            // Pick canonical name: the most common raw name, or the one with best capitalization
            val canonicalName = distinctRawNames
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: usages.first().exerciseName

            val hasDuplicateInSameRoutine = usages
                .groupBy { it.routineId }
                .any { (_, usagesInRoutine) -> usagesInRoutine.size > 1 }

            val hasCasingOrWhitespaceVariants = distinctRawNames.size > 1

            val firstNotes = usages.firstOrNull { it.notes.isNotBlank() }?.notes.orEmpty()
            val preferredRest = usages.firstOrNull()?.restSeconds ?: 180

            SavedExerciseSummary(
                canonicalName = canonicalName,
                normalizedKey = normalizeExerciseName(canonicalName),
                notes = firstNotes,
                restSeconds = preferredRest,
                usages = usages,
                hasDuplicateInSameRoutine = hasDuplicateInSameRoutine,
                hasCasingOrWhitespaceVariants = hasCasingOrWhitespaceVariants,
                variantNames = distinctRawNames.filter { it != canonicalName },
            )
        }.sortedWith(
            compareByDescending<SavedExerciseSummary> { it.isDuplicate }
                .thenBy { it.canonicalName.lowercase() }
        )
    }

    /**
     * Renames an exercise and optionally updates its notes/rest duration across all routines.
     */
    fun renameExercise(
        folders: List<RoutineFolder>,
        oldName: String,
        newName: String,
        newNotes: String? = null,
        newRestSeconds: Int? = null,
    ): List<RoutineFolder> {
        val oldKey = normalizeExerciseName(oldName)
        val cleanNewName = newName.trim()
        if (oldKey.isEmpty() || cleanNewName.isEmpty()) return folders

        return folders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    routine.copy(
                        exercises = routine.exercises.map { exercise ->
                            if (normalizeExerciseName(exercise.name) == oldKey) {
                                exercise.copy(
                                    name = cleanNewName,
                                    notes = newNotes ?: exercise.notes,
                                    restSeconds = newRestSeconds ?: exercise.restSeconds,
                                )
                            } else {
                                exercise
                            }
                        }
                    )
                }
            )
        }
    }

    /**
     * Merges a source exercise into a target exercise across all routines.
     * If a routine ends up with multiple instances of the target exercise,
     * they are combined into a single entry with merged sets.
     */
    fun mergeExercises(
        folders: List<RoutineFolder>,
        sourceName: String,
        targetName: String,
    ): List<RoutineFolder> {
        val sourceKey = normalizeExerciseName(sourceName)
        val targetKey = normalizeExerciseName(targetName)
        val cleanTargetName = targetName.trim()
        if (sourceKey.isEmpty() || targetKey.isEmpty() || sourceKey == targetKey) return folders

        // First rename source to target
        val renamedFolders = renameExercise(folders, sourceName, cleanTargetName)

        // Then deduplicate any routine that now contains duplicate target exercises
        return deduplicateRoutinesForExercise(renamedFolders, cleanTargetName)
    }

    /**
     * Deletes all occurrences of an exercise across all routines.
     */
    fun deleteExercise(
        folders: List<RoutineFolder>,
        name: String,
    ): List<RoutineFolder> {
        val key = normalizeExerciseName(name)
        if (key.isEmpty()) return folders

        return folders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    routine.copy(
                        exercises = routine.exercises.filterNot {
                            normalizeExerciseName(it.name) == key
                        }
                    )
                }
            )
        }
    }

    /**
     * Deletes a specific exercise instance from a specific routine.
     */
    fun deleteExerciseInstance(
        folders: List<RoutineFolder>,
        routineId: String,
        exerciseId: String,
    ): List<RoutineFolder> {
        return folders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    if (routine.id == routineId) {
                        routine.copy(
                            exercises = routine.exercises.filterNot { it.id == exerciseId }
                        )
                    } else {
                        routine
                    }
                }
            )
        }
    }

    /**
     * Deduplicates all exercises across the entire routine library:
     * 1. Unifies casing and spacing to the canonical name.
     * 2. Merges duplicate exercise entries within the same routine (combining their sets).
     */
    fun deduplicateAll(folders: List<RoutineFolder>): List<RoutineFolder> {
        val summaries = extractSavedExercises(folders)
        val canonicalMap = summaries.associate { it.normalizedKey to it.canonicalName }

        // Step 1: Unify names to canonical
        val normalizedFolders = folders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    routine.copy(
                        exercises = routine.exercises.map { exercise ->
                            val key = normalizeExerciseName(exercise.name)
                            val canonical = canonicalMap[key] ?: exercise.name.trim()
                            exercise.copy(name = canonical)
                        }
                    )
                }
            )
        }

        // Step 2: Combine in-routine duplicate exercises
        return normalizedFolders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    deduplicateRoutineExercises(routine)
                }
            )
        }
    }

    /**
     * Counts the total number of duplicate exercise occurrences:
     * - Extra occurrences within the same routine
     * - Occurrences with casing/spacing variants differing from the canonical name
     */
    fun countDuplicates(folders: List<RoutineFolder>): Int {
        val summaries = extractSavedExercises(folders)
        var count = 0
        summaries.forEach { summary ->
            if (summary.hasCasingOrWhitespaceVariants) {
                count += summary.variantNames.size
            }
            if (summary.hasDuplicateInSameRoutine) {
                summary.usages.groupBy { it.routineId }.values.forEach { usagesInRoutine ->
                    if (usagesInRoutine.size > 1) {
                        count += (usagesInRoutine.size - 1)
                    }
                }
            }
        }
        return count
    }

    private fun deduplicateRoutinesForExercise(
        folders: List<RoutineFolder>,
        exerciseName: String,
    ): List<RoutineFolder> {
        val key = normalizeExerciseName(exerciseName)
        return folders.map { folder ->
            folder.copy(
                routines = folder.routines.map { routine ->
                    val matchingCount = routine.exercises.count { normalizeExerciseName(it.name) == key }
                    if (matchingCount > 1) {
                        deduplicateRoutineExercises(routine)
                    } else {
                        routine
                    }
                }
            )
        }
    }

    private fun deduplicateRoutineExercises(routine: Routine): Routine {
        val seenKeys = mutableSetOf<String>()
        val deduplicatedExercises = mutableListOf<RoutineExercise>()

        routine.exercises.forEach { exercise ->
            val key = normalizeExerciseName(exercise.name)
            val existing = deduplicatedExercises.firstOrNull { normalizeExerciseName(it.name) == key }
            if (existing == null) {
                seenKeys.add(key)
                deduplicatedExercises.add(exercise)
            } else {
                // Merge sets of duplicate into the first instance
                val updatedIndex = deduplicatedExercises.indexOf(existing)
                val combinedSets = existing.sets + exercise.sets
                deduplicatedExercises[updatedIndex] = existing.copy(sets = combinedSets)
            }
        }

        return routine.copy(exercises = deduplicatedExercises)
    }
}
