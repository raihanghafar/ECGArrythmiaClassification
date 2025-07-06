package com.example.ecgarrhythmiaclassification.data

data class PerformanceMetrics(
    val inferenceTimeMs: Long,
    val cpuUsagePercent: Float,
    val memoryUsageMB: Float,
    val batteryLevel: Int
)