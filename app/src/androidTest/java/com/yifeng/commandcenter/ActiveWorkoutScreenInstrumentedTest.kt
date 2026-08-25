package com.yifeng.commandcenter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.ui.ActiveWorkoutScreen
import com.yifeng.commandcenter.ui.theme.HevyntTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveWorkoutScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fourExerciseWorkoutStartsDenseAndKeepsUtilitiesSecondary() {
        setWorkout()

        composeRule.onNodeWithText("FIRST LOWER").assertIsDisplayed()
        composeRule.onNodeWithText("DURATION").assertIsDisplayed()
        composeRule.onNodeWithText("VOLUME").assertIsDisplayed()
        composeRule.onNodeWithText("SETS").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Collapse exercise").assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("Expand exercise").assertCountEquals(3)
        composeRule.onNodeWithText("ROMANIAN DEADLIFT").assertIsDisplayed()
        composeRule.onNodeWithText("LEG PRESS").assertIsDisplayed()
        composeRule.onNodeWithText("SEATED LEG CURL").assertIsDisplayed()
        composeRule.onAllNodesWithText("ADD A SHORT NOTE…").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Open exercise notes").performClick()
        composeRule.onNodeWithText("CONTROL THE ECCENTRIC").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Edit rest timer").performClick()
        composeRule.onNodeWithText("REST TIMER").assertIsDisplayed()
        composeRule.onNodeWithText("02:15").assertIsDisplayed()
    }

    @Test
    fun previousCopyNumericReplacementAndDoneUpdateImmediately() {
        val workout = setWorkout()

        composeRule
            .onNodeWithContentDescription("Copy previous 77 × 10 to set 1")
            .performClick()
        composeRule.onNodeWithContentDescription("Set 1 weight in kilograms").assertTextEquals("77")
        composeRule.onNodeWithContentDescription("Set 1 repetitions").assertTextEquals("10")

        composeRule
            .onNodeWithContentDescription("Set 1 weight in kilograms")
            .performClick()
            .performTextReplacement("82,5")
        composeRule.runOnIdle {
            assertEquals("82.5", workout.value.exercises.first().sets.first().weight)
        }

        composeRule.onNodeWithContentDescription("Complete set 3").performClick()
        composeRule.onNodeWithContentDescription("Mark set 3 incomplete").assertIsDisplayed()
        composeRule.runOnIdle {
            assertTrue(workout.value.exercises.first().sets[2].completed)
        }
    }

    @Test
    fun destructiveExerciseActionLivesBehindOverflow() {
        val workout = setWorkout()
        composeRule.onAllNodesWithText("REMOVE EXERCISE").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Exercise options").performClick()
        composeRule.onNodeWithText("REMOVE EXERCISE").performClick()
        composeRule.onNodeWithText("REMOVE EXERCISE?").assertIsDisplayed()
        composeRule.onNodeWithText("REMOVE").performClick()

        composeRule.runOnIdle {
            assertEquals(3, workout.value.exercises.size)
            assertTrue(workout.value.exercises.none { it.id == "squat" })
        }
    }

    @Test
    fun addAndRemoveSetStayAvailableWithoutAVisibleDestructiveColumn() {
        val workout = setWorkout()

        composeRule.onNodeWithText("ADD SET").performClick()
        composeRule.runOnIdle {
            assertEquals(5, workout.value.exercises.first().sets.size)
        }

        composeRule.onNodeWithContentDescription("Set 5 options").performClick()
        composeRule.onNodeWithText("REMOVE SET").performClick()
        composeRule.runOnIdle {
            assertEquals(4, workout.value.exercises.first().sets.size)
        }
    }

    private fun setWorkout() = mutableStateOf(fixture()).also { workoutState ->
        composeRule.setContent {
            HevyntTheme {
                ActiveWorkoutScreen(
                    workout = workoutState.value,
                    previousSets = previousSets,
                    onRequestExerciseSuggestions = {},
                    onAddExercise = {},
                    onExerciseNameChanged = { exerciseId, name ->
                        workoutState.value = ActiveWorkoutOperations.updateExerciseName(
                            workoutState.value,
                            exerciseId,
                            name,
                        )!!
                    },
                    onExerciseNotesChanged = { exerciseId, notes ->
                        workoutState.value = ActiveWorkoutOperations.updateExerciseNotes(
                            workoutState.value,
                            exerciseId,
                            notes,
                        )!!
                    },
                    onExerciseRestChanged = { exerciseId, seconds ->
                        workoutState.value = ActiveWorkoutOperations.updateRestSeconds(
                            workoutState.value,
                            exerciseId,
                            seconds,
                        )!!
                    },
                    onExerciseRemoved = { exerciseId ->
                        workoutState.value = ActiveWorkoutOperations.removeExercise(
                            workoutState.value,
                            exerciseId,
                        )!!
                    },
                    onSetChanged = { exerciseId, setId, updated ->
                        workoutState.value = ActiveWorkoutOperations.updateSet(
                            workoutState.value,
                            exerciseId,
                            setId,
                        ) { updated }!!
                    },
                    onSetCompletionChanged = { exerciseId, setId, completed ->
                        workoutState.value = if (completed) {
                            ActiveWorkoutOperations.completeSet(
                                workoutState.value,
                                exerciseId,
                                setId,
                            )!!.workout
                        } else {
                            ActiveWorkoutOperations.uncompleteSet(
                                workoutState.value,
                                exerciseId,
                                setId,
                            )!!
                        }
                    },
                    onSetRemoved = { exerciseId, setId ->
                        workoutState.value = ActiveWorkoutOperations.removeSet(
                            workoutState.value,
                            exerciseId,
                            setId,
                        )!!
                    },
                    onSetAdded = { exerciseId ->
                        workoutState.value = ActiveWorkoutOperations.addSet(
                            workoutState.value,
                            exerciseId,
                        )!!
                    },
                    onFinish = {},
                    onMinimize = {},
                    onCancel = {},
                )
            }
        }
    }

    private fun fixture() = ActiveWorkout(
        id = "workout",
        routineName = "First Lower",
        startedAtMillis = System.currentTimeMillis() - 754_000L,
        exercises = listOf(
            exercise(
                id = "squat",
                name = "BACK SQUAT",
                note = "CONTROL THE ECCENTRIC",
                restSeconds = 135,
                completedSets = setOf(1),
            ),
            exercise("rdl", "ROMANIAN DEADLIFT", completedSets = setOf(0, 1)),
            exercise("press", "LEG PRESS"),
            exercise("curl", "SEATED LEG CURL", completedSets = setOf(0)),
        ),
    )

    private fun exercise(
        id: String,
        name: String,
        note: String = "",
        restSeconds: Int = 180,
        completedSets: Set<Int> = emptySet(),
    ) = ActiveExercise(
        id = id,
        name = name,
        notes = note,
        restSeconds = restSeconds,
        sets = List(4) { index ->
            WorkoutSet(
                id = "$id-set-$index",
                weight = if (index == 0) "75" else "77.5",
                reps = if (index == 0) "10" else "8",
                completed = index in completedSets,
            )
        },
    )

    private companion object {
        val previousSets = mapOf(
            "squat-set-0" to PreviousSetPerformance(77.0, 10),
            "squat-set-1" to PreviousSetPerformance(77.5, 8),
            "squat-set-2" to PreviousSetPerformance(80.0, 6),
            "squat-set-3" to PreviousSetPerformance(80.0, 6),
        )
    }
}