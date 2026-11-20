package com.yifeng.commandcenter.program

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.data.cleanRoutineName
import com.yifeng.commandcenter.history.normalizeExerciseName
import java.util.UUID

object ProgramSheetWorkoutMapper {
    /** Builds a full pre-filled [ActiveWorkout] directly from a synced Google Sheet day. */
    fun fromSheetDay(
        week: ProgramSheetWeek,
        day: ProgramSheetDay,
        startedAtMillis: Long = System.currentTimeMillis(),
        workoutId: String = UUID.randomUUID().toString(),
        nextId: () -> String = { UUID.randomUUID().toString() },
    ): ActiveWorkout {
        val weekSuffix = if (week.isDeload) "W${week.weekNumber} Scarico" else "W${week.weekNumber}"
        return ActiveWorkout(
            id = workoutId,
            sourceRoutineId = null,
            routineName = "${day.dayName} · $weekSuffix",
            startedAtMillis = startedAtMillis,
            exercises = day.exercises.map { sheetEx ->
                ActiveExercise(
                    id = nextId(),
                    sourceRoutineExerciseId = null,
                    name = sheetEx.name,
                    notes = sheetEx.notes,
                    restSeconds = sheetEx.restSeconds,
                    sets = List(sheetEx.sets.coerceAtLeast(1)) {
                        WorkoutSet(
                            id = nextId(),
                            weight = sheetEx.weightKg,
                            reps = sheetEx.reps,
                            completed = false,
                        )
                    },
                )
            },
        )
    }

    /**
     * Pre-fills matching exercises in an existing local routine workout using the active week's
     * Google Sheet targets (weight, reps, set count, and optional sheet note).
     */
    fun applySheetTargetsToWorkout(
        workout: ActiveWorkout,
        week: ProgramSheetWeek?,
        nextId: () -> String = { UUID.randomUUID().toString() },
    ): ActiveWorkout {
        if (week == null || week.days.isEmpty()) return workout

        val matchedDay = findMatchingDay(workout, week.days) ?: return workout
        val usedIndices = mutableSetOf<Int>()

        val updatedExercises = workout.exercises.map { exercise ->
            val targetName = normalizeExerciseName(exercise.name)
            val sheetIndex = matchedDay.exercises.indexOfFirstIndexed { idx, candidate ->
                idx !in usedIndices && namesMatch(targetName, normalizeExerciseName(candidate.name))
            }
            if (sheetIndex < 0) return@map exercise
            usedIndices.add(sheetIndex)
            val sheetEx = matchedDay.exercises[sheetIndex]

            val targetSetCount = sheetEx.sets.coerceAtLeast(1)
            val updatedSets = List(targetSetCount) { setIdx ->
                val existingSet = exercise.sets.getOrNull(setIdx)
                WorkoutSet(
                    id = existingSet?.id ?: nextId(),
                    weight = sheetEx.weightKg.ifBlank { existingSet?.weight.orEmpty() },
                    reps = sheetEx.reps.ifBlank { existingSet?.reps.orEmpty() },
                    completed = false,
                )
            }

            val weekTag = if (week.isDeload) "W${week.weekNumber} Scarico" else "W${week.weekNumber}"
            val mergedNotes = when {
                sheetEx.notes.isBlank() -> exercise.notes
                exercise.notes.isBlank() -> "[$weekTag] ${sheetEx.notes}"
                exercise.notes.contains(sheetEx.notes, ignoreCase = true) -> exercise.notes
                else -> "[$weekTag] ${sheetEx.notes}\n${exercise.notes}"
            }

            exercise.copy(
                notes = mergedNotes,
                sets = updatedSets,
            )
        }

        return workout.copy(exercises = updatedExercises)
    }

    /**
     * Returns the next week's number if [workout] matches the last scheduled day of the active
     * week and a subsequent week exists; returns `null` otherwise (including on the final week).
     */
    fun nextWeekAfterFinishing(
        workout: ActiveWorkout,
        state: ProgramSheetState,
    ): Int? {
        val activeWeek = state.activeWeek ?: return null
        val lastDay = activeWeek.days.lastOrNull() ?: return null
        val matchedDay = findMatchingDay(workout, activeWeek.days) ?: return null
        if (matchedDay.dayName != lastDay.dayName) return null

        val currentIndex = state.weeks.indexOfFirst { it.weekNumber == activeWeek.weekNumber }
        if (currentIndex < 0 || currentIndex + 1 >= state.weeks.size) return null
        return state.weeks[currentIndex + 1].weekNumber
    }

    private fun findMatchingDay(
        workout: ActiveWorkout,
        days: List<ProgramSheetDay>,
    ): ProgramSheetDay? {
        val strippedRoutineName = workout.routineName.substringBefore(" · W").trim()
        val workoutFullNorm = normalizeExerciseName(strippedRoutineName)
        val workoutCleanNorm = normalizeExerciseName(cleanRoutineName(strippedRoutineName))

        days.firstOrNull { day ->
            val dayFullNorm = normalizeExerciseName(day.dayName)
            val dayCleanNorm = normalizeExerciseName(cleanRoutineName(day.dayName))
            (workoutFullNorm.isNotEmpty() && workoutFullNorm == dayFullNorm) ||
                (workoutCleanNorm.isNotEmpty() && workoutCleanNorm == dayCleanNorm)
        }?.let { return it }

        val workoutExerciseNames = workout.exercises.map { normalizeExerciseName(it.name) }.filter { it.isNotEmpty() }
        if (workoutExerciseNames.isEmpty()) return null

        return days.maxByOrNull { day ->
            day.exercises.count { sheetEx ->
                val sheetNorm = normalizeExerciseName(sheetEx.name)
                workoutExerciseNames.any { namesMatch(it, sheetNorm) }
            }
        }?.takeIf { bestDay ->
            bestDay.exercises.any { sheetEx ->
                val sheetNorm = normalizeExerciseName(sheetEx.name)
                workoutExerciseNames.any { namesMatch(it, sheetNorm) }
            }
        }
    }

    private fun namesMatch(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        return a == b || a.contains(b) || b.contains(a)
    }

    private inline fun <T> List<T>.indexOfFirstIndexed(predicate: (Int, T) -> Boolean): Int {
        for (i in indices) {
            if (predicate(i, this[i])) return i
        }
        return -1
    }
}
