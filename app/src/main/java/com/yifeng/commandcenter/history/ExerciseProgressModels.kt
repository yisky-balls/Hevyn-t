package com.yifeng.commandcenter.history

enum class ExerciseProgressMetric(
    val shortLabel: String,
    val fullLabel: String,
    val unit: String,
) {
    TOP_WEIGHT("Top", "Top weight", "kg"),
    ESTIMATED_1RM("1RM", "Est. 1RM", "kg"),
    SESSION_VOLUME("Vol", "Volume", "kg"),
}

enum class ExerciseProgressPeriod(val label: String, val days: Long?) {
    DAYS_30("30D", 30L),
    DAYS_90("90D", 90L),
    DAYS_180("6M", 180L),
    ALL("All", null),
}

data class ExerciseProgressOption(
    val normalizedKey: String,
    val displayName: String,
    val sessionCount: Int,
    val lastPerformedMillis: Long,
    val bestWeightKg: Double?,
)

data class ExerciseSessionPoint(
    val workoutExerciseId: String,
    val completedAtMillis: Long,
    val dateEpochDay: Long,
    val topWeightKg: Double?,
    val topSetReps: Int?,
    val estimated1RmKg: Double?,
    val sessionVolumeKg: Double?,
    val sets: List<PreviousSetPerformance>,
) {
    fun valueFor(metric: ExerciseProgressMetric): Double? = when (metric) {
        ExerciseProgressMetric.TOP_WEIGHT -> topWeightKg
        ExerciseProgressMetric.ESTIMATED_1RM -> estimated1RmKg
        ExerciseProgressMetric.SESSION_VOLUME -> sessionVolumeKg
    }
}

data class ExerciseProgressSummary(
    val exerciseKey: String,
    val exerciseName: String,
    val metric: ExerciseProgressMetric,
    val period: ExerciseProgressPeriod,
    val points: List<ExerciseSessionPoint>,
    val latestValue: Double?,
    val deltaValue: Double?,
    val periodBest: Double?,
    val allTimePr: Double?,
    val sessionCount: Int,
)
