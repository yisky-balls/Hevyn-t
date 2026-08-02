package com.yifeng.commandcenter.history

import android.content.Context
import com.yifeng.commandcenter.data.LastWorkoutSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WorkoutHistoryRepository private constructor(
    private val dao: WorkoutHistoryDao,
) {
    constructor(context: Context) : this(
        WorkoutHistoryDatabase.getInstance(context).workoutHistoryDao(),
    )

    fun observeSessions(): Flow<List<WorkoutHistorySession>> =
        dao.observeSessions().map { sessions -> sessions.map(WorkoutSessionEntity::toModel) }

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

    suspend fun graph(sessionId: String): WorkoutHistoryGraph? = dao.graph(sessionId)

    suspend fun graphsCompletedBefore(completedAtExclusive: Long): List<WorkoutHistoryGraph> {
        val graphs = mutableListOf<WorkoutHistoryGraph>()
        dao.sessions()
            .filter { it.completedAtMillis < completedAtExclusive }
            .forEach { session -> dao.graph(session.id)?.let(graphs::add) }
        return graphs
    }

    suspend fun saveCompletedWorkout(graph: WorkoutHistoryGraph) = dao.insertGraph(graph)

    suspend fun updateNote(sessionId: String, note: String): Boolean =
        dao.updateNote(sessionId, note) == 1

    suspend fun replaceVisiblePhoto(photo: WorkoutHistoryPhoto): List<String> =
        dao.replaceVisiblePhoto(photo)

    suspend fun removeVisiblePhoto(sessionId: String): List<String> =
        dao.removeVisiblePhoto(sessionId)

    suspend fun deleteSession(sessionId: String): List<String> = dao.deleteSession(sessionId)

    suspend fun importLegacySummaries(summaries: List<LastWorkoutSummary>): Boolean =
        dao.importLegacyOnce(summaries.mapIndexed { index, summary ->
            CompletedWorkoutMapper.legacy(summary, index)
        })

    internal companion object {
        fun forTesting(dao: WorkoutHistoryDao) = WorkoutHistoryRepository(dao)
    }
}