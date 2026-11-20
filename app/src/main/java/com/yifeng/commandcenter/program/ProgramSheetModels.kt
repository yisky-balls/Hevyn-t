package com.yifeng.commandcenter.program

data class ProgramSheetExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val weightKg: String,
    val restSeconds: Int,
    val notes: String,
)

data class ProgramSheetDay(
    val dayName: String,
    val exercises: List<ProgramSheetExercise>,
)

data class ProgramSheetWeek(
    val weekNumber: Int,
    val label: String,
    val isDeload: Boolean,
    val days: List<ProgramSheetDay>,
)

data class ProgramSheetState(
    val sheetUrl: String = "",
    val weeks: List<ProgramSheetWeek> = emptyList(),
    val activeWeekNumber: Int = 1,
    val lastSyncedAtMillis: Long? = null,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
) {
    val isConfigured: Boolean get() = sheetUrl.isNotBlank()
    val hasData: Boolean get() = weeks.isNotEmpty()
    val activeWeek: ProgramSheetWeek?
        get() = weeks.firstOrNull { it.weekNumber == activeWeekNumber } ?: weeks.firstOrNull()
    val activeWeekDays: List<ProgramSheetDay>
        get() = activeWeek?.days.orEmpty()
}
