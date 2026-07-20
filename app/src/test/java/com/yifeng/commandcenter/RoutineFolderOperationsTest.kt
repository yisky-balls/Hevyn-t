package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineFolderOperations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineFolderOperationsTest {
    private val upper = Routine(id = "upper", name = "Upper", exercises = listOf("Bench"))
    private val leg = Routine(id = "leg", name = "Leg", exercises = listOf("Squat"))
    private val emptyFolder = RoutineFolder(id = "empty", name = "2b2d1s")
    private val programFolder = RoutineFolder(id = "program", name = "PPL UL", routines = listOf(upper))

    @Test
    fun `new routine is added only to the selected folder`() {
        val result = RoutineFolderOperations.saveRoutine(
            folders = listOf(emptyFolder, programFolder),
            folderId = emptyFolder.id,
            routine = leg,
        )

        assertEquals(listOf(leg), result.first().routines)
        assertEquals(listOf(upper), result.last().routines)
    }

    @Test
    fun `editing routine replaces it without changing its position`() {
        val edited = upper.copy(name = "Upper A", exercises = listOf("Bench", "Row"))
        val result = RoutineFolderOperations.saveRoutine(
            folders = listOf(programFolder),
            folderId = programFolder.id,
            routine = edited,
            existingRoutineId = upper.id,
        )

        assertEquals(listOf(edited), result.single().routines)
    }

    @Test
    fun `deleting routine keeps the parent folder`() {
        val result = RoutineFolderOperations.deleteRoutine(
            folders = listOf(programFolder),
            folderId = programFolder.id,
            routineId = upper.id,
        )

        assertEquals("PPL UL", result.single().name)
        assertTrue(result.single().routines.isEmpty())
    }
}