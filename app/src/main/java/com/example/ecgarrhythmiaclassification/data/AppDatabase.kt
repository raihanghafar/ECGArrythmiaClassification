package com.example.ecgarrhythmiaclassification.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [ClassificationResult::class],
    version = 2, // incremented from 1 to 2
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classificationDao(): ClassificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ecg_database"
                )
                    // Add a fallback for destructive migration for simplicity
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
