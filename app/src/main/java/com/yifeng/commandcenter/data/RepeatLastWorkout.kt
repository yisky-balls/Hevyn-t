package com.yifeng.commandcenter.data

import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import java.util.UUID

sealed interface RepeatLastWorkoutResult {
    data class Started(val workout: ActiveWorkout) : RepeatLastWorkoutResult
    data object NoHistory : RepeatLastWorkoutResult
    data object ActiveWorkoutExists : RepeatLastWorkoutResult
}

object RepeatedWorkoutMapper {
    fun fromHistory(
        graph: WorkoutHistoryGraph,
        startedAtMillis: Long = System.currentTimeMillis(),
        nextId: () -> String = { UUID.randomUUID().toString() },
    ): ActiveWorkout = ActiveWorkout(
        id = nextId(),
        sourceRoutineId = graph.session.sourceRoutineId,
        routineName = graph.session.routineName,
        startedAtMillis = startedAtMillis,
        exercises = graph.exercises
            .sortedBy { it.exercise.position }
            .map { historyExercise ->
                ActiveExercise(
                    id = nextId(),
                    sourceRoutineExerciseId = historyExercise.exercise.sourceRoutineExerciseId,
                    name = historyExercise.exercise.name,
                    notes = historyExercise.exercise.note,
                    restSeconds = historyExercise.exercise.restSeconds,
                    sets = historyExercise.sets
                        .sortedBy { it.position }
                        .map { historySet ->
                            WorkoutSet(
                                id = nextId(),
                                weight = historySet.weightKg?.let(::formatWeight).orEmpty(),
                                reps = historySet.repetitions?.toString().orEmpty(),
                                completed = false,
                            )
                        },
                )
            },
    )
}

class RepeatLastWorkout(
    private val latestGraph: suspend () -> WorkoutHistoryGraph?,
    private val startIfIdle: (ActiveWorkoutSession) -> Boolean,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nextId: () -> String = { UUID.randomUUID().toString() },
) {
    constructor(
        historyRepository: WorkoutHistoryRepository,
        activeWorkoutRepository: ActiveWorkoutRepository,
    ) : this(
        latestGraph = historyRepository::latestRepeatableGraph,
        startIfIdle = activeWorkoutRepository::startIfIdle,
    )

    suspend operator fun invoke(): RepeatLastWorkoutResult {
        val graph = latestGraph() ?: return RepeatLastWorkoutResult.NoHistory
        val workout = RepeatedWorkoutMapper.fromHistory(
            graph = graph,
            startedAtMillis = nowMillis(),
            nextId = nextId,
        )
        return if (startIfIdle(ActiveWorkoutSession(workout))) {
            RepeatLastWorkoutResult.Started(workout)
        } else {
            RepeatLastWorkoutResult.ActiveWorkoutExists
        }
    }
}