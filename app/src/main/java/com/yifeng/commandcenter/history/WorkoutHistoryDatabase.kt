package com.yifeng.commandcenter.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        WorkoutPhotoEntity::class,
        WorkoutHistoryMetadataEntity::class,
    ],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}