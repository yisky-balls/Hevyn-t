package com.yifeng.commandcenter.data

import org.json.JSONObject

object WorkoutPlanParser {
    const val SUPPORTED_FORMAT = "hevynt-workout-plan"
    const val SUPPORTED_VERSION = 1
    const val MIN_REST_SECONDS = 0
    const val MAX_REST_SECONDS = 1800

    fun parse(jsonString: String): WorkoutPlanParseResult {
        if (jsonString.isBlank()) {
            return WorkoutPlanParseResult.Failure("Workout plan file is empty.")
        }
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            return WorkoutPlanParseResult.Failure("Malformed JSON: ${e.message ?: "Invalid JSON syntax"}")
        }

        val format = root.optString("format")
        if (format != SUPPORTED_FORMAT) {
            return WorkoutPlanParseResult.Failure(
                if (format.isBlank()) "Missing 'format' field in JSON."
                else "Unsupported format '$format'. Expected '$SUPPORTED_FORMAT'."
            )
        }

        if (!root.has("version")) {
            return WorkoutPlanParseResult.Failure("Missing 'version' field in JSON.")
        }
        val version = root.optInt("version", -1)
        if (version != SUPPORTED_VERSION) {
            return WorkoutPlanParseResult.Failure(
                "Unsupported version $version. Only version $SUPPORTED_VERSION is supported."
            )
        }

        val programId = root.optString("programId").trim()
        if (programId.isEmpty()) {
            return WorkoutPlanParseResult.Failure("Program ID cannot be empty.")
        }

        val programName = root.optString("programName").trim()
        if (programName.isEmpty()) {
            return WorkoutPlanParseResult.Failure("Program name cannot be empty.")
        }

        val folderJson = root.optJSONObject("folder")
            ?: return WorkoutPlanParseResult.Failure("Missing 'folder' object in JSON.")

        val folderName = folderJson.optString("name").trim().ifEmpty { programName }

        val routinesArray = folderJson.optJSONArray("routines")
            ?: return WorkoutPlanParseResult.Failure("Missing 'routines' list in folder.")

        if (routinesArray.length() == 0) {
            return WorkoutPlanParseResult.Failure("Workout plan must contain at least one routine.")
        }

        val routineIds = mutableSetOf<String>()
        val routines = mutableListOf<WorkoutPlanRoutineDto>()

        for (i in 0 until routinesArray.length()) {
            val routineObj = routinesArray.optJSONObject(i)
                ?: return WorkoutPlanParseResult.Failure("Invalid routine object at index $i.")

            val routineId = routineObj.optString("id").trim()
            if (routineId.isEmpty()) {
                return WorkoutPlanParseResult.Failure("Routine at index $i is missing an 'id'.")
            }
            if (!routineIds.add(routineId)) {
                return WorkoutPlanParseResult.Failure("Duplicate routine ID found: '$routineId'.")
            }

            val routineName = routineObj.optString("name").trim()
            if (routineName.isEmpty()) {
                return WorkoutPlanParseResult.Failure("Routine '$routineId' is missing a 'name'.")
            }

            val exercisesArray = routineObj.optJSONArray("exercises")
            val exercises = mutableListOf<WorkoutPlanExerciseDto>()
            if (exercisesArray != null) {
                for (j in 0 until exercisesArray.length()) {
                    val exerciseObj = exercisesArray.optJSONObject(j)
                        ?: return WorkoutPlanParseResult.Failure("Invalid exercise object at index $j in routine '$routineName'.")

                    val exerciseName = exerciseObj.optString("name").trim()
                    if (exerciseName.isEmpty()) {
                        return WorkoutPlanParseResult.Failure("Exercise at index $j in routine '$routineName' is missing a 'name'.")
                    }

                    val notes = exerciseObj.optString("notes", "")
                    val restSeconds = exerciseObj.optInt("restSeconds", 180)
                    if (restSeconds < MIN_REST_SECONDS || restSeconds > MAX_REST_SECONDS) {
                        return WorkoutPlanParseResult.Failure(
                            "Invalid rest duration ($restSeconds s) for exercise '$exerciseName' in routine '$routineName'. Must be between $MIN_REST_SECONDS and $MAX_REST_SECONDS seconds."
                        )
                    }

                    val sets = if (exerciseObj.has("sets")) {
                        exerciseObj.optInt("sets", -1)
                    } else {
                        3
                    }
                    if (sets < 0) {
                        return WorkoutPlanParseResult.Failure(
                            "Invalid set count ($sets) for exercise '$exerciseName' in routine '$routineName'. Must be non-negative."
                        )
                    }

                    exercises.add(
                        WorkoutPlanExerciseDto(
                            name = exerciseName,
                            notes = notes,
                            restSeconds = restSeconds,
                            sets = sets,
                        )
                    )
                }
            }

            routines.add(
                WorkoutPlanRoutineDto(
                    id = routineId,
                    name = routineName,
                    exercises = exercises,
                )
            )
        }

        return WorkoutPlanParseResult.Success(
            WorkoutPlanDto(
                format = format,
                version = version,
                programId = programId,
                programName = programName,
                folder = WorkoutPlanFolderDto(
                    name = folderName,
                    routines = routines,
                ),
            )
        )
    }
}
