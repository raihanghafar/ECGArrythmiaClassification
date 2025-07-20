package com.example.ecgarrhythmiaclassification.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "classification_results")
data class ClassificationResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val timestamp: Long,
    val nCount: Int,
    val sCount: Int,
    val vCount: Int,
    val fCount: Int,
    val qCount: Int,
    val totalBeats: Int,
    val averageConfidence: Float,
    val inferenceTimeMs: Long,
    val cpuUsagePercent: Float,
    val memoryUsageMB: Float,
    val rawPredictions: String,
    val fileSize: Long, // in bytes
    val formattedInferenceTime: String // e.g. "1 min 12.3 s" or "950 ms"
) : Serializable