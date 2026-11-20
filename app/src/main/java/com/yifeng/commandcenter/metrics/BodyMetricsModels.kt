package com.yifeng.commandcenter.metrics

enum class BodyMetricsPeriod(val label: String, val days: Long?) {
    DAYS_7("7D", 7L),
    DAYS_30("30D", 30L),
    DAYS_90("90D", 90L),
    ALL("All", null),
}

data class BodyMetricEntry(
    val dateEpochDay: Long,
    val weightKg: Double?,
    val caloriesKcal: Int?,
)

data class WeightPoint(
    val dateEpochDay: Long,
    val weightKg: Double,
)

data class CaloriesPoint(
    val dateEpochDay: Long,
    val caloriesKcal: Int,
)

data class BodyMetricsSummary(
    val period: BodyMetricsPeriod,
    val weightPoints: List<WeightPoint>,
    val weightMovingAvgPoints: List<WeightPoint>,
    val caloriesPoints: List<CaloriesPoint>,
    val latestWeightKg: Double?,
    val latestWeightDateEpochDay: Long?,
    val weightDeltaKg: Double?,
    val minWeightKg: Double?,
    val maxWeightKg: Double?,
    val avgWeightKg: Double?,
    val latestCalories: Int?,
    val latestCaloriesDateEpochDay: Long?,
    val avgCalories: Int?,
    val minCalories: Int?,
    val maxCalories: Int?,
)

data class BodyMetricsSyncState(
    val sheetUrl: String = "",
    val entries: List<BodyMetricEntry> = emptyList(),
    val lastSyncedAtMillis: Long? = null,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
) {
    val isConfigured: Boolean get() = sheetUrl.isNotBlank()
}
