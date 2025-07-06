package com.example.ecgarrhythmiaclassification.data

import android.app.Application
import androidx.lifecycle.LiveData

class ECGRepository(application: Application) {
    private val dao = AppDatabase.getDatabase(application).classificationDao()

    fun getAllResults(): LiveData<List<ClassificationResult>> = dao.getAllResults()

    suspend fun insertResult(result: ClassificationResult): Long = dao.insertResult(result)

    suspend fun deleteResult(result: ClassificationResult) = dao.deleteResult(result)

    suspend fun deleteAllResults() = dao.deleteAllResults()
}
