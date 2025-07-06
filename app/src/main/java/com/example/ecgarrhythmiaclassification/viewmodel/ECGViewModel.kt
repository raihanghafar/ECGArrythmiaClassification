package com.example.ecgarrhythmiaclassification.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.example.ecgarrhythmiaclassification.data.ECGRepository
import com.example.ecgarrhythmiaclassification.data.ClassificationResult
import com.example.ecgarrhythmiaclassification.data.PerformanceMetrics
import com.example.ecgarrhythmiaclassification.ml.ECGPreprocessor
import com.example.ecgarrhythmiaclassification.ml.TFLiteClassifier
import com.example.ecgarrhythmiaclassification.ml.ClassificationOutput
import com.example.ecgarrhythmiaclassification.utils.CSVReader
import com.example.ecgarrhythmiaclassification.utils.PerformanceMonitor

class ECGViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ECGRepository(application)
    private val preprocessor = ECGPreprocessor()
    private val classifier = TFLiteClassifier(application)
    private val performanceMonitor = PerformanceMonitor(application)

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _classificationResult = MutableLiveData<ClassificationResult?>()
    val classificationResult: LiveData<ClassificationResult?> = _classificationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun processCSVFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null

                performanceMonitor.startMonitoring()

                val ecgData = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    CSVReader().readMITBIHData(inputStream!!)
                }

                val preprocessedData = withContext(Dispatchers.Default) {
                    preprocessor.preprocessECGData(ecgData)
                }

                val classificationOutput = withContext(Dispatchers.Default) {
                    classifier.classifyHeartbeats(preprocessedData.scalograms)
                }

                val performanceMetrics = performanceMonitor.stopMonitoring()

                val result = calculateResults(
                    uri,
                    classificationOutput,
                    performanceMetrics
                )

                repository.insertResult(result)

                _classificationResult.value = result

            } catch (e: Exception) {
                _error.value = "Error processing file: ${e.message}"
                Log.e("ECGViewModel", "Error processing CSV", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun calculateResults(
        uri: Uri,
        output: ClassificationOutput,
        metrics: PerformanceMetrics
    ): ClassificationResult {

        val predictions = output.predictions
        val confidences = output.confidences

        var nCount = 0
        var sCount = 0
        var vCount = 0
        var fCount = 0
        var qCount = 0

        for (prediction in predictions) {
            val maxIndex = prediction.indices.maxByOrNull { prediction[it] } ?: 0
            when (maxIndex) {
                0 -> nCount++
                1 -> sCount++
                2 -> vCount++
                3 -> fCount++
                4 -> qCount++
            }
        }

        val averageConfidence = confidences.average().toFloat()
        val fileName = getFileName(uri)

        return ClassificationResult(
            fileName = fileName,
            timestamp = System.currentTimeMillis(),
            nCount = nCount,
            sCount = sCount,
            vCount = vCount,
            fCount = fCount,
            qCount = qCount,
            totalBeats = predictions.size,
            averageConfidence = averageConfidence,
            inferenceTimeMs = metrics.inferenceTimeMs,
            cpuUsagePercent = metrics.cpuUsagePercent,
            memoryUsageMB = metrics.memoryUsageMB,
            rawPredictions = Gson().toJson(predictions)
        )
    }

    private fun getFileName(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "Unknown File"
    }

    override fun onCleared() {
        super.onCleared()
        classifier.close()
    }
}
