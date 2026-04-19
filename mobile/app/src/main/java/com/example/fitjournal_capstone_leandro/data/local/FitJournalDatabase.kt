package com.example.fitjournal_capstone_leandro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MuscleEntity::class, ExerciseEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FitJournalDatabase : RoomDatabase() {

    abstract fun muscleDao(): MuscleDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: FitJournalDatabase? = null

        fun getDatabase(context: Context): FitJournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitJournalDatabase::class.java,
                    "fitjournal_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}