package com.yifeng.commandcenter

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRecommendationReason
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.ui.WorkoutProgramsScreen
import com.yifeng.commandcenter.ui.theme.HevyntTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutProgramsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun folderHasNoStartAndRoutineCardUsesSeparateOpenAndStartTargets() {
        var opened: Pair<String, String?>? = null
        var started: String? = null
        setLauncher(
            onBuildRoutine = { folderId, routineId -> opened = folderId to routineId },
            onStartRoutine = { started = it },
        )

        composeRule.onNodeWithText("4 ROUTINES").assertIsDisplayed()
        composeRule.onAllNodesWithText("START").assertCountEquals(0)
        composeRule.onNodeWithText("Training rotation").performClick()
        composeRule.onNodeWithText(LONG_ROUTINE_NAME).performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("folder" to "routine", opened) }

        composeRule.onAllNodesWithText("START")[0].performClick()
        composeRule.runOnIdle { assertEquals("routine", started) }
    }

    @Test
    fun activeWorkoutOverridesPredictionAndDisablesSecondWorkoutActions() {
        setLauncher(
            activeWorkout = ActiveWorkout(
                id = "active",
                routineName = "Current session",
                startedAtMillis = System.currentTimeMillis() - 120_000L,
                exercises = listOf(
                    ActiveExercise(
                        name = "Press",
                        sets = listOf(WorkoutSet(completed = true), WorkoutSet()),
                    ),
                ),
            ),
            likelyNext = RoutineRecommendation(
                routine = routine(),
                reason = RoutineRecommendationReason.ObservedTransition,
            ),
        )

        composeRule.onNodeWithText("CONTINUE WORKOUT").assertIsDisplayed()
        composeRule.onAllNodesWithText("LIKELY NEXT ROUTINE").assertCountEquals(0)
        composeRule.onNodeWithText("START EMPTY WORKOUT").assertIsNotEnabled()
    }

    @Test
    fun longNamesAndActionTargetsRemainSeparatedAtLargeFontScale() {
        setLauncher(fontScale = 2f)
        composeRule.onNodeWithText("Training rotation").performClick()

        composeRule.onNodeWithText(LONG_ROUTINE_NAME).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("START")[0].assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Routine options")[0].assertIsDisplayed()
        val startBounds = composeRule.onAllNodesWithText("START")[0].fetchSemanticsNode().boundsInRoot
        val menuBounds = composeRule.onAllNodesWithContentDescription("Routine options")[0]
            .fetchSemanticsNode().boundsInRoot
        assertTrue("START must remain left of the overflow action", startBounds.right <= menuBounds.left)
    }

    private fun setLauncher(
        activeWorkout: ActiveWorkout? = null,
        likelyNext: RoutineRecommendation? = null,
        fontScale: Float = 1f,
        onBuildRoutine: (String, String?) -> Unit = { _, _ -> },
        onStartRoutine: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            HevyntTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale),
                ) {
                    WorkoutProgramsScreen(
                        folders = folders(),
                        likelyNext = likelyNext,
                        activeWorkout = activeWorkout,
                        listState = rememberLazyListState(),
                        onContinueWorkout = {},
                        onStartRoutine = onStartRoutine,
                        onBuildRoutine = onBuildRoutine,
                        onCreateFolder = {},
                        onRenameFolder = { _, _ -> },
                        onDeleteFolder = {},
                        onDeleteRoutine = { _, _ -> },
                        onStartEmpty = {},
                    )
                }
            }
        }
    }

    private fun folders() = listOf(
        RoutineFolderSummary(
            id = "folder",
            name = "Training rotation",
            routines = listOf(
                routine(),
                RoutineSummary("two", "folder", "Short", 2),
                RoutineSummary("three", "folder", "Another", 3),
                RoutineSummary("four", "folder", "Final", 1),
            ),
        ),
    )

    private fun routine() = RoutineSummary(
        id = "routine",
        folderId = "folder",
        name = LONG_ROUTINE_NAME,
        exerciseCount = 4,
        lastPerformedAtMillis = System.currentTimeMillis() - 5 * 86_400_000L,
    )

    private companion object {
        const val LONG_ROUTINE_NAME =
            "Extremely long full body strength and hypertrophy routine name for overflow testing"
    }
}