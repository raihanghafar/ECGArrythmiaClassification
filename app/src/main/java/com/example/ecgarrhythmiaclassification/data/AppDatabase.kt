package com.example.ecgarrhythmiaclassification.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [ClassificationResult::class],
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
