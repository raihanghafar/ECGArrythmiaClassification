package com.example.ecgarrhythmiaclassification.ml

import android.util.Log
import kotlin.math.*

class WaveletTransform {
    private val samplingRate = 360.0

    // ✅ TAMBAHKAN: Method test untuk quick fix
    fun continuousWaveletTransformTest(
        signal: DoubleArray,
        rPeaks: IntArray
    ): Array<Array<FloatArray>> {

        Log.i("WaveletTransform", "TEST MODE: Processing only first 10 heartbeats")

        val before = 90
        val after = 110
        val scalograms = mutableListOf<Array<FloatArray>>()

        // ✅ TEST: Only process first 10 heartbeats
        val maxTest = minOf(10, rPeaks.size - 2)

        for (i in 1..maxTest) {
            val rPeak = rPeaks[i]
            val startIdx = rPeak - before
            val endIdx = rPeak + after

            if (startIdx >= 0 && endIdx < signal.size) {
                val segment = signal.sliceArray(startIdx..endIdx)

                // ✅ Simplified CWT - reduced scales
                val coeffs = simplifiedCWT(segment)
                scalograms.add(coeffs)
            }

            Log.d("WaveletTransform", "Test heartbeat $i/$maxTest completed")
        }

        Log.i("WaveletTransform", "TEST: Generated ${scalograms.size} scalograms")
        return scalograms.toTypedArray()
    }

    // ✅ TAMBAHKAN: Simplified CWT untuk testing
    private fun simplifiedCWT(signal: DoubleArray): Array<FloatArray> {
        // Create realistic-looking dummy data instead of all 0.5f
        val result = Array(100) { FloatArray(100) }

        for (i in 0 until 100) {
            for (j in 0 until 100) {
                // Generate some pattern based on signal
                val signalValue = if (signal.isNotEmpty()) {
                    val idx = (j * signal.size / 100).coerceIn(0, signal.size - 1)
                    signal[idx].toFloat()
                } else {
                    0.5f
                }

                // Add some variation
                result[i][j] = (signalValue * (i + 1) / 100f).toFloat()
            }
        }

        return result
    }

    // ✅ TETAP ADA: Method original (untuk nanti)
    fun continuousWaveletTransform(
        signal: DoubleArray,
        rPeaks: IntArray
    ): Array<Array<FloatArray>> {
        // ... kode original tetap ada
        val before = 90
        val after = 110

        val centralFrequency = getMexicanHatCentralFrequency()
        val scales = (1..100).map { scale ->
            centralFrequency * samplingRate / scale
        }.toDoubleArray()

        val avgRRI = if (rPeaks.size > 1) {
            val rrIntervals = IntArray(rPeaks.size - 1) { i ->
                rPeaks[i + 1] - rPeaks[i]
            }
            rrIntervals.average()
        } else {
            360.0
        }

        val scalograms = mutableListOf<Array<FloatArray>>()

        for (i in 1 until rPeaks.size - 1) {
            val rPeak = rPeaks[i]
            val startIdx = rPeak - before
            val endIdx = rPeak + after

            if (startIdx >= 0 && endIdx < signal.size) {
                val segment = signal.sliceArray(startIdx..endIdx)

                val coeffs = mexicanHatCWT(segment, scales)
                val resizedScalogram = resizeWithOpenCVEquivalent(coeffs, 100, 100)

                scalograms.add(resizedScalogram)
            }
        }

        return scalograms.toTypedArray()
    }

    private fun mexicanHatCWT(signal: DoubleArray, scales: DoubleArray): Array<FloatArray> {
        val coefficients = Array(scales.size) { FloatArray(signal.size) }
        val samplingPeriod = 1.0 / samplingRate

        for (scaleIdx in scales.indices) {
            val scale = scales[scaleIdx]

            for (n in signal.indices) {
                var sum = 0.0

                for (k in signal.indices) {
                    val t = (n - k) * samplingPeriod / scale
                    val waveletValue = mexicanHatWavelet(t)
                    sum += signal[k] * waveletValue
                }

                coefficients[scaleIdx][n] = (sum * samplingPeriod / sqrt(scale)).toFloat()
            }
        }

        return coefficients
    }

    private fun mexicanHatWavelet(t: Double): Double {
        val t2 = t * t
        val normalizationFactor = 2.0 / (sqrt(3.0) * PI.pow(0.25))
        return normalizationFactor * (1.0 - t2) * exp(-t2 / 2.0)
    }

    private fun getMexicanHatCentralFrequency(): Double {
        return 0.25
    }

    private fun resizeWithOpenCVEquivalent(
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
                val srcY = i * scaleY
                val srcX = j * scaleX

                val y1 = srcY.toInt().coerceIn(0, srcHeight - 1)
                val x1 = srcX.toInt().coerceIn(0, srcWidth - 1)
                val y2 = (y1 + 1).coerceIn(0, srcHeight - 1)
                val x2 = (x1 + 1).coerceIn(0, srcWidth - 1)

                val dy = srcY - y1
                val dx = srcX - x1

                val w1 = (1 - dx) * (1 - dy)
                val w2 = dx * (1 - dy)
                val w3 = (1 - dx) * dy
                val w4 = dx * dy

                result[i][j] = (
                        w1 * input[y1][x1] +
                                w2 * input[y1][x2] +
                                w3 * input[y2][x1] +
                                w4 * input[y2][x2]
                        ).toFloat()
            }
        }

        return result
    }
}
