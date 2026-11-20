package com.yifeng.commandcenter.program

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramSheetTest {

    @Test
    fun parsesItalianCsvWithTopTrainingMaxTableAndDeloadWeek() {
        val csv = """
            TRAINING MAX (kg),Valore,Note
            Panca Piana TM,105,Modifica
            Squat TM,115,Arrotondato

            Settimana,Giorno,Esercizio,Serie,Ripetizioni,Peso,Recupero,Note
            1,MAR — Upper 1,Panca Piana,4,6,"77,5",180,72.5% TM
            1,MAR — Upper 1,Alzate Laterali,3,12,,90,RIR 1
            6 (Scarico),MAR — Upper 1,Panca Piana,3,5,65 kg,150,Scarico tecnico
        """.trimIndent()

        val result = ProgramSheetCsvParser.parse(csv)
        assertEquals(2, result.weeks.size)

        val w1 = result.weeks[0]
        assertEquals(1, w1.weekNumber)
        assertFalse(w1.isDeload)
        assertEquals(1, w1.days.size)
        assertEquals("MAR — Upper 1", w1.days[0].dayName)
        assertEquals(2, w1.days[0].exercises.size)
        assertEquals("77.5", w1.days[0].exercises[0].weightKg)
        assertEquals("6", w1.days[0].exercises[0].reps)
        assertEquals(4, w1.days[0].exercises[0].sets)
        assertEquals("", w1.days[0].exercises[1].weightKg)

        val w6 = result.weeks[1]
        assertEquals(6, w6.weekNumber)
        assertTrue(w6.isDeload)
        assertEquals("W6 · Scarico", w6.label)
        assertEquals("65", w6.days[0].exercises[0].weightKg)
        assertEquals(3, w6.days[0].exercises[0].sets)
    }

    @Test
    fun buildsFullPreFilledWorkoutFromSheetDay() {
        val week = ProgramSheetWeek(
            weekNumber = 2,
            label = "W2",
            isDeload = false,
            days = listOf(
                ProgramSheetDay(
                    dayName = "GIO — Lower 1",
                    exercises = listOf(
                        ProgramSheetExercise("Squat", 4, "6", "87.5", 180, "75% TM"),
                        ProgramSheetExercise("Leg Curl", 3, "10", "", 90, "RIR 1"),
                    ),
                ),
            ),
        )

        val workout = ProgramSheetWorkoutMapper.fromSheetDay(week, week.days.first())
        assertEquals("GIO — Lower 1 · W2", workout.routineName)
        assertEquals(2, workout.exercises.size)
        assertEquals(4, workout.exercises[0].sets.size)
        assertTrue(workout.exercises[0].sets.all { it.weight == "87.5" && it.reps == "6" && !it.completed })
        assertEquals(3, workout.exercises[1].sets.size)
        assertTrue(workout.exercises[1].sets.all { it.weight == "" && it.reps == "10" })
    }

    @Test
    fun appliesActiveWeekSheetTargetsOntoExistingLocalRoutine() {
        val baseWorkout = ActiveWorkout(
            routineName = "MAR — Upper 1 (Panca Primaria)",
            exercises = listOf(
                ActiveExercise(
                    name = "Panca Piana",
                    notes = "Fermo al petto",
                    sets = List(4) { WorkoutSet() },
                ),
                ActiveExercise(
                    name = "Curl Bicipiti",
                    sets = List(3) { WorkoutSet() },
                ),
            ),
        )
        val deloadWeek = ProgramSheetWeek(
            weekNumber = 6,
            label = "W6 · Scarico",
            isDeload = true,
            days = listOf(
                ProgramSheetDay(
                    dayName = "MAR — Upper 1 (Panca Primaria)",
                    exercises = listOf(
                        ProgramSheetExercise("Panca Piana", 3, "5", "65", 150, "Scarico tecnico"),
                    ),
                ),
            ),
        )

        val updated = ProgramSheetWorkoutMapper.applySheetTargetsToWorkout(baseWorkout, deloadWeek)
        assertEquals(3, updated.exercises[0].sets.size)
        assertTrue(updated.exercises[0].sets.all { it.weight == "65" && it.reps == "5" })
        assertTrue(updated.exercises[0].notes.contains("W6 Scarico"))
        assertEquals(3, updated.exercises[1].sets.size)
        assertTrue(updated.exercises[1].sets.all { it.weight == "" })
    }

    @Test
    fun advancesWeekOnlyAfterFinishingLastDayAndStopsOnFinalWeek() {
        val w1 = ProgramSheetWeek(
            weekNumber = 1,
            label = "W1",
            isDeload = false,
            days = listOf(
                ProgramSheetDay("MAR — Upper 1", listOf(ProgramSheetExercise("Panca Piana", 4, "6", "75", 180, ""))),
                ProgramSheetDay("DOM — Lower 2", listOf(ProgramSheetExercise("Deadlift", 4, "4", "110", 180, ""))),
            ),
        )
        val w2 = ProgramSheetWeek(
            weekNumber = 2,
            label = "W2",
            isDeload = false,
            days = listOf(
                ProgramSheetDay("MAR — Upper 1", listOf(ProgramSheetExercise("Panca Piana", 4, "6", "80", 180, ""))),
                ProgramSheetDay("DOM — Lower 2", listOf(ProgramSheetExercise("Deadlift", 4, "4", "112.5", 180, ""))),
            ),
        )
        val stateW1 = ProgramSheetState(
            sheetUrl = "https://docs.google.com/spreadsheets/d/abc/edit",
            weeks = listOf(w1, w2),
            activeWeekNumber = 1,
        )

        val nonLastWorkout = ActiveWorkout(routineName = "MAR — Upper 1 · W1", exercises = emptyList())
        assertEquals(null, ProgramSheetWorkoutMapper.nextWeekAfterFinishing(nonLastWorkout, stateW1))

        val lastDayWorkout = ActiveWorkout(routineName = "DOM — Lower 2 · W1", exercises = emptyList())
        assertEquals(2, ProgramSheetWorkoutMapper.nextWeekAfterFinishing(lastDayWorkout, stateW1))

        val stateFinal = stateW1.copy(activeWeekNumber = 2)
        val finalDayWorkout = ActiveWorkout(routineName = "DOM — Lower 2 · W2", exercises = emptyList())
        assertEquals(null, ProgramSheetWorkoutMapper.nextWeekAfterFinishing(finalDayWorkout, stateFinal))
    }
}
