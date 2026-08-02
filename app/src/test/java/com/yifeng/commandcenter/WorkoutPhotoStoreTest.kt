package com.yifeng.commandcenter

import com.yifeng.commandcenter.history.WorkoutPhotoStore
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPhotoStoreTest {
    @Test
    fun missingAndEscapingPhotoPathsResolveSafely() {
        val root = createTempDirectory("photos-").toFile()
        try {
            val store = WorkoutPhotoStore(root)
            assertNull(store.existingFile("workout-photos/missing.image"))
            assertNull(store.resolve("../outside.image"))
            val photo = File(root, "workout-photos/valid.image").apply {
                parentFile!!.mkdirs()
                writeText("image")
            }
            assertTrue(store.existingFile("workout-photos/valid.image") == photo.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun deleteRemovesOnlyValidatedWorkoutPhotoPaths() {
        runBlocking {
            val root = createTempDirectory("photos-").toFile()
            try {
                val store = WorkoutPhotoStore(root)
                val photo = File(root, "workout-photos/valid.image").apply {
                    parentFile!!.mkdirs()
                    writeText("image")
                }
                val outside = File(root.parentFile, "outside-${root.name}.image").apply { writeText("keep") }

                store.delete(listOf("workout-photos/valid.image", "../${outside.name}"))

                assertFalse(photo.exists())
                assertTrue(outside.exists())
                outside.delete()
            } finally {
                root.deleteRecursively()
            }
        }
    }
}