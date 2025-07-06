package com.example.ecgarrhythmiaclassification.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.ecgarrhythmiaclassification.data.ECGRepository
import com.example.ecgarrhythmiaclassification.data.ClassificationResult

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ECGRepository(application)

    val allResults: LiveData<List<ClassificationResult>> = repository.getAllResults()

    fun deleteResult(result: ClassificationResult) {
        viewModelScope.launch {
            repository.deleteResult(result)
        }
    }

    fun deleteAllResults() {
        viewModelScope.launch {
            repository.deleteAllResults()
        }
    }
}
