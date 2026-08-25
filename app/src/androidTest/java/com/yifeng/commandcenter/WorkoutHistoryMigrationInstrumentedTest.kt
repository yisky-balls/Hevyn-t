package com.yifeng.commandcenter

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yifeng.commandcenter.history.WorkoutHistoryDatabase
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutHistoryMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutHistoryDatabase::class.java,
    )

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun cleanUp() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrationOneToTwoMarksExistingHistoricalRowsCompleted() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO workout_sessions VALUES
                ('session', NULL, 'Upper', 1000, 2000, 1000, '', 1, 400.0, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_exercises VALUES
                ('exercise', 'session', 'source', 'Bench', '', 90, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sets VALUES
                ('set', 'exercise', 0, 80.0, 5)
                """.trimIndent(),
            )
            close()
        }

        val database = Room.databaseBuilder(
            context,
            WorkoutHistoryDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(WorkoutHistoryDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = WorkoutHistoryRepository.forTesting(database.workoutHistoryDao())
            val graph = kotlinx.coroutines.runBlocking { repository.graph("session") }
            assertTrue(graph?.exercises?.single()?.sets?.single()?.completed == true)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DATABASE_NAME = "workout-history-migration-test.db"
    }
}