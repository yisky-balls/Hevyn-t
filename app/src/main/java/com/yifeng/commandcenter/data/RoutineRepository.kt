package com.yifeng.commandcenter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

sealed interface RoutineLibraryImportResult {
    data object Imported : RoutineLibraryImportResult
    data object AlreadyImported : RoutineLibraryImportResult
    data class CorruptLegacyData(val cause: Throwable) : RoutineLibraryImportResult
}

class RoutineRepository private constructor(
    private val dao: RoutineLibraryDao,
    private val legacySource: LegacyRoutineLibrarySource,
    private val legacyPreferences: SharedPreferences?,
) {
    constructor(context: Context) : this(
        dao = RoutineLibraryDatabase.getInstance(context.applicationContext ?: context)
            .routineLibraryDao(),
        legacySource = SharedPreferencesRoutineLibrarySource(context),
        legacyPreferences = (context.applicationContext ?: context)
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE),
    )

    /** Lightweight reactive projection used by Home and the Workout launcher. */
    fun observeSummaries(): Flow<List<RoutineFolderSummary>> =
        dao.observeSummaries().map(List<RoutineLibrarySummaryRow>::toSummaryModels)

    /** Complete hierarchy for editors, mutations, and exercise selection only. */
    fun observeFolders(): Flow<List<RoutineFolder>> = dao.observeLibrary().map { folders ->
        folders
            .sortedWith(compareBy({ it.folder.position }, { it.folder.id }))
            .map(RoutineFolderWithRoutines::toModel)
    }

    /**
     * Imports SharedPreferences exactly once. A corrupt payload is left untouched and the Room
     * completion marker is not written, so a repaired payload can be retried on the next launch.
     */
    suspend fun initialize(): RoutineLibraryImportResult {
        if (dao.isLegacyImportComplete()) return RoutineLibraryImportResult.AlreadyImported
        return when (val legacy = legacySource.read()) {
            is LegacyRoutineLibraryResult.Corrupt ->
                RoutineLibraryImportResult.CorruptLegacyData(legacy.cause)

            is LegacyRoutineLibraryResult.Ready -> {
                if (dao.importLegacyOnce(legacy.folders)) {
                    RoutineLibraryImportResult.Imported
                } else {
                    RoutineLibraryImportResult.AlreadyImported
                }
            }
        }
    }

    suspend fun folders(): List<RoutineFolder> = dao.library()
        .sortedWith(compareBy({ it.folder.position }, { it.folder.id }))
        .map(RoutineFolderWithRoutines::toModel)

    suspend fun routine(routineId: String): Routine? = dao.routine(routineId)?.toModel()

    suspend fun replaceFolders(folders: List<RoutineFolder>) {
        dao.replaceLibrary(folders)
    }

    /** Compatibility for repository instrumentation; production orchestration uses suspend APIs. */
    fun loadFolders(): List<RoutineFolder> = runBlocking(Dispatchers.IO) {
        when (val result = initialize()) {
            is RoutineLibraryImportResult.CorruptLegacyData -> throw result.cause
            RoutineLibraryImportResult.AlreadyImported,
            RoutineLibraryImportResult.Imported,
            -> folders()
        }
    }

    /** Compatibility for repository instrumentation; production orchestration uses suspend APIs. */
    fun saveFolders(folders: List<RoutineFolder>) = runBlocking(Dispatchers.IO) {
        replaceFolders(folders)
    }

    /** Legacy history is read-only input for the independent history-v2 transactional import. */
    fun loadWorkoutHistory(): List<LastWorkoutSummary> {
        val preferences = legacyPreferences ?: return emptyList()
        preferences.getString(KEY_WORKOUT_HISTORY, null)?.let { raw ->
            return runCatching { decodeWorkoutHistory(JSONArray(raw)) }.getOrDefault(emptyList())
        }
        return loadLegacyLastWorkout(preferences)?.let(::listOf).orEmpty()
    }

    fun saveLastWorkout(summary: LastWorkoutSummary) {
        val preferences = checkNotNull(legacyPreferences) {
            "Legacy history preferences are unavailable in this repository."
        }
        val history = loadWorkoutHistory() + summary
        val array = JSONArray().apply {
            history.forEach { item ->
                put(
                    JSONObject()
                        .put("name", item.name)
                        .put("completedAtMillis", item.completedAtMillis)
                        .put("completedSets", item.completedSets)
                        .put("volumeKg", item.volumeKg),
                )
            }
        }
        preferences.edit {
            putString(KEY_WORKOUT_HISTORY, array.toString())
            remove(KEY_LAST_NAME)
            remove(KEY_LAST_DATE)
            remove(KEY_LAST_SETS)
            remove(KEY_LAST_VOLUME)
        }
    }

    private fun loadLegacyLastWorkout(preferences: SharedPreferences): LastWorkoutSummary? {
        val name = preferences.getString(KEY_LAST_NAME, null) ?: return null
        return LastWorkoutSummary(
            name = name,
            completedAtMillis = preferences.getLong(KEY_LAST_DATE, 0L),
            completedSets = preferences.getInt(KEY_LAST_SETS, 0),
            volumeKg = when (val value = preferences.all[KEY_LAST_VOLUME]) {
                is Float -> value.toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                else -> 0.0
            },
        )
    }

    private fun decodeWorkoutHistory(array: JSONArray): List<LastWorkoutSummary> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                LastWorkoutSummary(
                    name = item.getString("name"),
                    completedAtMillis = item.getLong("completedAtMillis"),
                    completedSets = item.optInt("completedSets"),
                    volumeKg = item.optDouble("volumeKg"),
                ),
            )
        }
    }

    internal companion object {
        fun forTesting(
            dao: RoutineLibraryDao,
            legacySource: LegacyRoutineLibrarySource,
        ) = RoutineRepository(dao, legacySource, legacyPreferences = null)

        private const val PREFERENCES = "lift_programs"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_SETS = "last_sets"
        private const val KEY_LAST_VOLUME = "last_volume"
        private const val KEY_WORKOUT_HISTORY = "workout_history"
    }
}