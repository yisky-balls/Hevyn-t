package com.yifeng.commandcenter.data

object RoutineFolderOperations {
    fun saveRoutine(
        folders: List<RoutineFolder>,
        folderId: String,
        routine: Routine,
        existingRoutineId: String? = null,
    ): List<RoutineFolder> = folders.map { folder ->
        if (folder.id != folderId) return@map folder
        folder.copy(
            routines = if (existingRoutineId == null) {
                folder.routines + routine
            } else {
                folder.routines.map { existing ->
                    if (existing.id == existingRoutineId) routine else existing
                }
            }
        )
    }

    fun deleteRoutine(
        folders: List<RoutineFolder>,
        folderId: String,
        routineId: String,
    ): List<RoutineFolder> = folders.map { folder ->
        if (folder.id == folderId) {
            folder.copy(routines = folder.routines.filterNot { it.id == routineId })
        } else {
            folder
        }
    }
}