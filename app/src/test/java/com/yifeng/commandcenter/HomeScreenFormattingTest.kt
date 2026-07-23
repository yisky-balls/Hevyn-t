package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.ui.formatRoutineExercisePreview
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenFormattingTest {
    @Test
    fun previewDisplaysExerciseNamesInsteadOfDataObjects() {
        val exercises = listOf(
            RoutineExercise(id = "bench-id", name = "Bench Press", restSeconds = 180),
            RoutineExercise(id = "row-id", name = "Cable Row", restSeconds = 120),
        )

        assertEquals(
            "Bench Press  •  Cable Row",
            formatRoutineExercisePreview(exercises),
        )
    }

    @Test
    fun previewUsesEmptyStateWhenRoutineHasNoExercises() {
        assertEquals(
            "Add exercises to build your first program.",
            formatRoutineExercisePreview(emptyList()),
        )
    }

    @Test
    fun previewShowsAtMostThreeExerciseNames() {
        val exercises = listOf("One", "Two", "Three", "Four").map { name ->
            RoutineExercise(name = name)
        }

        assertEquals(
            "One  •  Two  •  Three",
            formatRoutineExercisePreview(exercises),
        )
    }
}