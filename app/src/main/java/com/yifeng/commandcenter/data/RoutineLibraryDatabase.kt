package com.yifeng.commandcenter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoutineFolderEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
        RoutineLibraryMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RoutineLibraryDatabase : RoomDatabase() {
    abstract fun routineLibraryDao(): RoutineLibraryDao

    companion object {
        const val DATABASE_NAME = "routine_library.db"

        @Volatile
        private var instance: RoutineLibraryDatabase? = null

        fun getInstance(context: Context): RoutineLibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoutineLibraryDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }

        internal fun resetForTesting(context: Context) = synchronized(this) {
            instance?.close()
            instance = null
            context.deleteDatabase(DATABASE_NAME)
        }
    }
}