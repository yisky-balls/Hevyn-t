package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.WorkoutPlanParseResult
import com.yifeng.commandcenter.data.WorkoutPlanParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanParserTest {

    private val validJson = """
        {
          "format": "hevynt-workout-plan",
          "version": 1,
          "programId": "powerbuilding-4-day",
          "programName": "Powerbuilding 4-Day",
          "folder": {
            "name": "Powerbuilding 4-Day",
            "routines": [
              {
                "id": "upper-a",
                "name": "TUE — Upper A",
                "exercises": [
                  {
                    "name": "Bench Press",
                    "notes": "HEAVY: 1×4–6 @ RPE 8\nBACKOFF: 3×5–8 @ RPE 7–7.5",
                    "restSeconds": 180,
                    "sets": 4
                  },
                  {
                    "name": "Chest-Supported Row",
                    "notes": "3×8–10 @ RPE 8",
                    "restSeconds": 120,
                    "sets": 3
                  }
                ]
              },
              {
                "id": "lower-a",
                "name": "THU — Lower A",
                "exercises": [
                  {
                    "name": "Back Squat",
                    "notes": "HEAVY: 1×4–6 @ RPE 8",
                    "restSeconds": 240,
                    "sets": 4
                  }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun parse_valid_plan_succeeds_with_correct_structure() {
        val result = WorkoutPlanParser.parse(validJson)
        assertTrue(result is WorkoutPlanParseResult.Success)
        val plan = (result as WorkoutPlanParseResult.Success).plan

        assertEquals("hevynt-workout-plan", plan.format)
        assertEquals(1, plan.version)
        assertEquals("powerbuilding-4-day", plan.programId)
        assertEquals("Powerbuilding 4-Day", plan.programName)
        assertEquals("Powerbuilding 4-Day", plan.folder.name)
        assertEquals(2, plan.folder.routines.size)
        assertEquals(2, plan.totalRoutines)
        assertEquals(3, plan.totalExercises)
        assertEquals(11, plan.totalSets)

        val upperA = plan.folder.routines[0]
        assertEquals("upper-a", upperA.id)
        assertEquals("TUE — Upper A", upperA.name)
        assertEquals(2, upperA.exercises.size)

        val bench = upperA.exercises[0]
        assertEquals("Bench Press", bench.name)
        assertEquals("HEAVY: 1×4–6 @ RPE 8\nBACKOFF: 3×5–8 @ RPE 7–7.5", bench.notes)
        assertEquals(180, bench.restSeconds)
        assertEquals(4, bench.sets)
    }

    @Test
    fun parse_malformed_json_returns_failure() {
        val result = WorkoutPlanParser.parse("{ not valid json ")
        assertTrue(result is WorkoutPlanParseResult.Failure)
        val error = (result as WorkoutPlanParseResult.Failure).error
        assertTrue(error.contains("Malformed JSON") || error.contains("Invalid JSON"))
    }

    @Test
    fun parse_empty_input_returns_failure() {
        val result = WorkoutPlanParser.parse("   ")
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertEquals("Workout plan file is empty.", (result as WorkoutPlanParseResult.Failure).error)
    }

    @Test
    fun parse_unsupported_format_returns_failure() {
        val json = validJson.replace("\"hevynt-workout-plan\"", "\"other-format\"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Unsupported format"))
    }

    @Test
    fun parse_unsupported_version_returns_failure() {
        val json = validJson.replace("\"version\": 1", "\"version\": 2")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Unsupported version 2"))
    }

    @Test
    fun parse_missing_program_id_returns_failure() {
        val json = validJson.replace("\"powerbuilding-4-day\"", "\"   \"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Program ID cannot be empty"))
    }

    @Test
    fun parse_missing_program_name_returns_failure() {
        val json = validJson.replace("\"Powerbuilding 4-Day\"", "\"\"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Program name cannot be empty"))
    }

    @Test
    fun parse_missing_routine_id_returns_failure() {
        val json = validJson.replace("\"id\": \"upper-a\"", "\"id\": \"\"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("missing an 'id'"))
    }

    @Test
    fun parse_duplicate_routine_ids_returns_failure() {
        val json = validJson.replace("\"id\": \"lower-a\"", "\"id\": \"upper-a\"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Duplicate routine ID"))
    }

    @Test
    fun parse_empty_routine_name_returns_failure() {
        val json = validJson.replace("\"name\": \"TUE — Upper A\"", "\"name\": \" \"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("missing a 'name'"))
    }

    @Test
    fun parse_empty_exercise_name_returns_failure() {
        val json = validJson.replace("\"name\": \"Bench Press\"", "\"name\": \"\"")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("missing a 'name'"))
    }

    @Test
    fun parse_negative_set_count_returns_failure() {
        val json = validJson.replace("\"sets\": 4", "\"sets\": -1")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("non-negative"))
    }

    @Test
    fun parse_negative_rest_duration_returns_failure() {
        val json = validJson.replace("\"restSeconds\": 180", "\"restSeconds\": -5")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Invalid rest duration"))
    }

    @Test
    fun parse_excessive_rest_duration_returns_failure() {
        val json = validJson.replace("\"restSeconds\": 180", "\"restSeconds\": 3601")
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("Invalid rest duration"))
    }

    @Test
    fun parse_empty_routines_list_returns_failure() {
        val json = """
            {
              "format": "hevynt-workout-plan",
              "version": 1,
              "programId": "empty-plan",
              "programName": "Empty Plan",
              "folder": {
                "name": "Empty Plan",
                "routines": []
              }
            }
        """.trimIndent()
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Failure)
        assertTrue((result as WorkoutPlanParseResult.Failure).error.contains("at least one routine"))
    }

    @Test
    fun powerbuilding_4_day_sample_plan_parses_successfully() {
        val file = java.io.File("../sample-plans/powerbuilding-4-day.json")
            .takeIf { it.exists() }
            ?: java.io.File("sample-plans/powerbuilding-4-day.json")
        assertTrue("Sample plan file should exist", file.exists())
        val json = file.readText()
        val result = WorkoutPlanParser.parse(json)
        assertTrue(result is WorkoutPlanParseResult.Success)
        val plan = (result as WorkoutPlanParseResult.Success).plan
        assertEquals("powerbuilding-4-day", plan.programId)
        assertEquals("Powerbuilding 4-Day", plan.programName)
        assertEquals(5, plan.totalRoutines)
        assertEquals(
            listOf("TUE — Upper A", "THU — Lower A", "SAT — Upper B", "SUN — Lower B", "OPTIONAL — Arm Day"),
            plan.folder.routines.map { it.name },
        )
        assertEquals(29, plan.totalExercises)
        assertEquals(89, plan.totalSets)
    }
}
