package com.yifeng.commandcenter.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkoutPlanCoordinator(
    private val context: Context,
    private val routineRepository: RoutineRepository,
) {
    suspend fun loadPlanFromUri(uri: Uri): WorkoutPlanParseResult = withContext(Dispatchers.IO) {
        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw java.io.IOException("Unable to open selected file.")
        }.getOrElse { error ->
            return@withContext WorkoutPlanParseResult.Failure("Unable to read file: ${error.message}")
        }
        WorkoutPlanParser.parse(content)
    }

    suspend fun checkPlanExists(programId: String): Boolean =
        routineRepository.hasProgram(programId)

    suspend fun importPlan(plan: WorkoutPlanDto, replaceExisting: Boolean): Boolean =
        routineRepository.importWorkoutPlan(plan, replaceExisting = replaceExisting)
}
