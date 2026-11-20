package com.yifeng.commandcenter.history

import android.content.Context
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.widget.WorkoutWidgetUpdateDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WorkoutHistoryRepository private constructor(
    private val dao: WorkoutHistoryDao,
    private val onHistoryChanged: () -> Unit,
) {
    constructor(context: Context) : this(
        WorkoutHistoryDatabase.getInstance(context.applicationContext ?: context)
            .workoutHistoryDao(),
        widgetRefreshCallback(context),
    )

    fun observeJournalEntries(): Flow<List<WorkoutJournalEntry>> =
        combine(dao.observeSessions(), dao.observePhotos()) { sessions, photos ->
            val firstPhotoBySession = photos
                .groupBy(WorkoutPhotoEntity::sessionId)
                .mapValues { (_, sessionPhotos) -> sessionPhotos.first().toModel() }
            sessions.map { session ->
                WorkoutJournalEntry(
                    session = session.toModel(),
                    photo = firstPhotoBySession[session.id],
                )
            }
        }

    suspend fun sessions(): List<WorkoutHistorySession> =
        dao.sessions().map(WorkoutSessionEntity::toModel)

    fun observeCompletedExerciseSets(): Flow<List<HistoricalExerciseSetRow>> =
        dao.observeCompletedExerciseSets()

    suspend fun graph(sessionId: String): WorkoutHistoryGraph? = dao.graph(sessionId)

    suspend fun latestRepeatableGraph(): WorkoutHistoryGraph? = dao.latestRepeatableGraph()

    suspend fun graphsCompletedBefore(completedAtExclusive: Long): List<WorkoutHistoryGraph> {
        val graphs = mutableListOf<WorkoutHistoryGraph>()
        dao.sessions()
            .filter { it.completedAtMillis < completedAtExclusive }
            .forEach { session -> dao.graph(session.id)?.let(graphs::add) }
        return graphs
    }

    suspend fun previousSets(workout: ActiveWorkout): Map<String, PreviousSetPerformance> =
        PreviousSetResolver.resolveRows(
            workout = workout,
            rows = dao.completedExerciseSetsBefore(workout.startedAtMillis),
        )

    suspend fun exerciseHistory(workout: ActiveWorkout): Map<String, ExerciseHistorySummary> =
        PreviousSetResolver.resolveExerciseHistoryRows(
            workout = workout,
            rows = dao.completedExerciseSetsBefore(workout.startedAtMillis),
        )

    /** Resolves both previous sets and exercise history using a single database query. */
    suspend fun workoutHistoryContext(
        workout: ActiveWorkout,
    ): Pair<Map<String, PreviousSetPerformance>, Map<String, ExerciseHistorySummary>> {
        val rows = dao.completedExerciseSetsBefore(workout.startedAtMillis)
        return PreviousSetResolver.resolveRows(workout = workout, rows = rows) to
            PreviousSetResolver.resolveExerciseHistoryRows(workout = workout, rows = rows)
    }

    suspend fun renameExercises(renamesByNormalizedKey: Map<String, String>): Int =
        dao.renameExercises(renamesByNormalizedKey)

    suspend fun saveCompletedWorkout(graph: WorkoutHistoryGraph) {
        dao.insertGraph(graph)
        notifyHistoryChanged()
    }

    suspend fun updateNote(sessionId: String, note: String): Boolean =
        dao.updateNote(sessionId, note) == 1

    suspend fun replaceVisiblePhoto(photo: WorkoutHistoryPhoto): List<String> =
        dao.replaceVisiblePhoto(photo)

    suspend fun removeVisiblePhoto(sessionId: String): List<String> =
        dao.removeVisiblePhoto(sessionId)

    suspend fun deleteSession(sessionId: String): List<String> {
        val removedPaths = dao.deleteSession(sessionId)
        notifyHistoryChanged()
        return removedPaths
    }

    suspend fun importLegacySummaries(summaries: List<LastWorkoutSummary>): Boolean {
        val imported = dao.importLegacyOnce(summaries.mapIndexed { index, summary ->
            CompletedWorkoutMapper.legacy(summary, index)
        })
        if (imported) notifyHistoryChanged()
        return imported
    }

    private fun notifyHistoryChanged() {
        try {
            onHistoryChanged()
        } catch (_: Exception) {
            // Room is authoritative; an ancillary widget refresh must not undo a committed mutation.
        }
    }

    internal companion object {
        private fun widgetRefreshCallback(context: Context): () -> Unit {
            val appContext = context.applicationContext ?: context
            return { WorkoutWidgetUpdateDispatcher.request(appContext) }
        }

        fun forTesting(
            dao: WorkoutHistoryDao,
            onHistoryChanged: () -> Unit = {},
        ) = WorkoutHistoryRepository(dao, onHistoryChanged)
    }
}