package com.yifeng.commandcenter.data

class WorkoutPlanImporter(
    private val dao: RoutineLibraryDao,
) {
    suspend fun hasProgram(programId: String): Boolean {
        val folderId = WorkoutPlanIdentity.programFolderId(programId)
        return dao.folderById(folderId) != null
    }

    suspend fun importWorkoutPlan(
        plan: WorkoutPlanDto,
        replaceExisting: Boolean = false,
    ): Boolean {
        val folderId = WorkoutPlanIdentity.programFolderId(plan.programId)
        val existingFolder = dao.folderById(folderId)
        val position = existingFolder?.position ?: ((dao.maxFolderPosition() ?: -1) + 1)

        val folderEntity = RoutineFolderEntity(
            id = folderId,
            name = plan.folder.name.ifBlank { plan.programName },
            position = position,
        )

        val routineEntities = mutableListOf<RoutineEntity>()
        val exerciseEntities = mutableListOf<RoutineExerciseEntity>()
        val setEntities = mutableListOf<RoutineSetEntity>()

        plan.folder.routines.forEachIndexed { routineIndex, routineDto ->
            val routineId = WorkoutPlanIdentity.programRoutineId(plan.programId, routineDto.id)
            routineEntities += RoutineEntity(
                id = routineId,
                folderId = folderId,
                name = routineDto.name,
                position = routineIndex,
            )

            routineDto.exercises.forEachIndexed { exerciseIndex, exerciseDto ->
                val exerciseId = WorkoutPlanIdentity.programExerciseId(
                    plan.programId,
                    routineDto.id,
                    exerciseIndex,
                )
                exerciseEntities += RoutineExerciseEntity(
                    id = exerciseId,
                    routineId = routineId,
                    name = exerciseDto.name,
                    notes = exerciseDto.notes,
                    restSeconds = exerciseDto.restSeconds,
                    position = exerciseIndex,
                )

                repeat(exerciseDto.sets) { setIndex ->
                    val setId = WorkoutPlanIdentity.programSetId(
                        plan.programId,
                        routineDto.id,
                        exerciseIndex,
                        setIndex,
                    )
                    setEntities += RoutineSetEntity(
                        id = setId,
                        exerciseId = exerciseId,
                        weight = "",
                        repetitions = "",
                        position = setIndex,
                    )
                }
            }
        }

        return dao.importProgram(
            folder = folderEntity,
            routines = routineEntities,
            exercises = exerciseEntities,
            sets = setEntities,
            metadataKey = WorkoutPlanIdentity.programMetadataKey(plan.programId),
            replaceExisting = replaceExisting,
        )
    }
}
