package com.example.ecgarrhythmiaclassification.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ClassificationDao {
    @Query("SELECT * FROM classification_results ORDER BY timestamp DESC")
    fun getAllResults(): LiveData<List<ClassificationResult>>

    @Insert
    suspend fun insertResult(result: ClassificationResult): Long

    @Delete
    suspend fun deleteResult(result: ClassificationResult)

    @Query("DELETE FROM classification_results")
    suspend fun deleteAllResults()
}
