package com.yifeng.commandcenter.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        WorkoutPhotoEntity::class,
        WorkoutHistoryMetadataEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class WorkoutHistoryDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): WorkoutHistoryDao

    companion object {
        const val DATABASE_NAME = "workout_history.db"

        @Volatile
        private var instance: WorkoutHistoryDatabase? = null

        fun getInstance(context: Context): WorkoutHistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutHistoryDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 only stored completed rows, so every existing row is completed.
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN completed INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}