package com.example.ecgarrhythmiaclassification.ml

import android.util.Log
import kotlin.math.*

class WaveletTransform {
    private val samplingRate = 360.0
    private val samplingPeriod = 1.0 / samplingRate
    private val numScales = 32
    private val scalogramSize = 100

    fun continuousWaveletTransform(
        signal: DoubleArray,
        rPeaks: IntArray
    ): Array<Array<FloatArray>> {
        val before = 90
        val after = 110
        val centralFrequency = 0.25

        val scales = (1..numScales).map { scale ->
            centralFrequency * samplingRate / scale
        }.toDoubleArray()

        val scalograms = mutableListOf<Array<FloatArray>>()

        for (i in 1 until rPeaks.size - 1) {
            val rPeak = rPeaks[i]
            val startIdx = rPeak - before
            val endIdx = rPeak + after

            if (startIdx >= 0 && endIdx < signal.size) {
                val segment = signal.sliceArray(startIdx..endIdx)
                val coeffs = windowedMexicanHatCWT(segment, scales)
                val resizedScalogram = resizeWithNearestNeighbor(coeffs, scalogramSize, scalogramSize)

                // Normalisasi per scalogram dengan clamp minimal 0.1f
                val maxVal = resizedScalogram.flatMap { row -> row.asList() }.map { abs(it) }.maxOrNull() ?: 1f
                val safeMax = max(maxVal, 0.1f)

                val normalizedScalogram = Array(resizedScalogram.size) { y ->
                    FloatArray(resizedScalogram[0].size) { x ->
                        resizedScalogram[y][x] / safeMax
                    }
                }

                logScalogramStats(normalizedScalogram, "Beat $i")

                scalograms.add(normalizedScalogram)
            }
        }

        return scalograms.toTypedArray()
    }

    private fun windowedMexicanHatCWT(signal: DoubleArray, scales: DoubleArray): Array<FloatArray> {
        val windowSize = 30
        val coefficients = Array(scales.size) { FloatArray(signal.size) }

        for (scaleIdx in scales.indices) {
            val scale = scales[scaleIdx]

            for (n in signal.indices) {
                var sum = 0.0
                val startK = (n - windowSize).coerceAtLeast(0)
                val endK = (n + windowSize).coerceAtMost(signal.size - 1)

                for (k in startK..endK) {
                    val t = (n - k) * samplingPeriod / scale
                    val waveletValue = mexicanHatWavelet(t)
                    sum += signal[k] * waveletValue
                }

                // Tetap optimalkan scaling convolution
                coefficients[scaleIdx][n] = (sum * samplingPeriod / sqrt(scale) * 20.0).toFloat()
            }
        }

        return coefficients
    }

    private fun mexicanHatWavelet(t: Double): Double {
        val t2 = t * t
        val normalizationFactor = 2.0 / (sqrt(3.0) * PI.pow(0.25))
        return normalizationFactor * (1.0 - t2) * exp(-t2 / 2.0)
    }

    private fun resizeWithNearestNeighbor(
        input: Array<FloatArray>,
        targetHeight: Int,
        targetWidth: Int
    ): Array<FloatArray> {
        val srcHeight = input.size
        val srcWidth = input[0].size
        val result = Array(targetHeight) { FloatArray(targetWidth) }

        val scaleY = srcHeight.toDouble() / targetHeight
        val scaleX = srcWidth.toDouble() / targetWidth

        for (i in 0 until targetHeight) {
            for (j in 0 until targetWidth) {
                val srcY = (i * scaleY).toInt().coerceIn(0, srcHeight - 1)
                val srcX = (j * scaleX).toInt().coerceIn(0, srcWidth - 1)
                result[i][j] = input[srcY][srcX]
            }
        }

        return result
    }

    private fun logScalogramStats(scalogram: Array<FloatArray>, label: String) {
        val allValues = scalogram.flatMap { row -> row.asList() }
        val minVal = allValues.minOrNull() ?: 0f
        val maxVal = allValues.maxOrNull() ?: 0f
        val meanVal = allValues.average().toFloat()

        Log.d("ScalogramStats", "$label - Min: $minVal, Max: $maxVal, Mean: $meanVal")
    }
}
