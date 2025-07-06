package com.example.ecgarrhythmiaclassification.ml

import kotlin.math.*  // Import semua fungsi math

class TompkinsDetector {
    private val samplingRate = 360.0

    fun detectQRS(signal: DoubleArray): IntArray {
        val filtered = bandPassFilter(signal)
        val derivative = derivative(filtered)
        val squared = squaring(derivative)
        val integrated = movingWindowIntegration(squared)

        return findRPeaks(integrated, filtered, signal)
    }

    private fun bandPassFilter(signal: DoubleArray): DoubleArray {
        val result = signal.copyOf()

        // Low pass filter
        for (i in signal.indices) {
            result[i] = signal[i]

            if (i >= 1) result[i] += 2 * result[i - 1]
            if (i >= 2) result[i] -= result[i - 2]
            if (i >= 6) result[i] -= 2 * signal[i - 6]
            if (i >= 12) result[i] += signal[i - 12]
        }

        val highPassResult = DoubleArray(signal.size)

        // High pass filter
        for (i in signal.indices) {
            highPassResult[i] = -result[i]

            if (i >= 1) highPassResult[i] -= highPassResult[i - 1]
            if (i >= 16) highPassResult[i] += 32 * result[i - 16]
            if (i >= 32) highPassResult[i] += result[i - 32]
        }

        // Normalize - Perbaikan di sini
        val maxVal = highPassResult.maxOfOrNull { abs(it) } ?: 1.0  // ✅ Gunakan abs, bukan kotlin.math.abs
        return highPassResult.map { it / maxVal }.toDoubleArray()
    }

    private fun derivative(signal: DoubleArray): DoubleArray {
        val result = DoubleArray(signal.size)

        for (i in signal.indices) {
            result[i] = 0.0

            if (i >= 1) result[i] -= 2 * signal[i - 1]
            if (i >= 2) result[i] -= signal[i - 2]
            if (i >= 2 && i <= signal.size - 3) result[i] += 2 * signal[i + 1]
            if (i >= 2 && i <= signal.size - 4) result[i] += signal[i + 2]

            result[i] = (result[i] * samplingRate) / 8
        }

        return result
    }

    private fun squaring(signal: DoubleArray): DoubleArray {
        return signal.map { it * it }.toDoubleArray()
    }

    private fun movingWindowIntegration(signal: DoubleArray): DoubleArray {
        val result = DoubleArray(signal.size)
        val winSize = (0.150 * samplingRate).toInt()
        var sum = 0.0

        // Calculate sum for first N terms
        for (j in 0 until winSize) {
            sum += signal[j] / winSize
            result[j] = sum
        }

        // Moving window integration
        for (i in winSize until signal.size) {
            sum += signal[i] / winSize
            sum -= signal[i - winSize] / winSize
            result[i] = sum
        }

        return result
    }

    private fun findRPeaks(integrated: DoubleArray, filtered: DoubleArray, original: DoubleArray): IntArray {
        val peaks = mutableListOf<Int>()
        val threshold = integrated.average() * 1.5

        for (i in 1 until integrated.size - 1) {
            if (integrated[i] > threshold &&
                integrated[i] > integrated[i - 1] &&
                integrated[i] > integrated[i + 1]) {
                peaks.add(i)
            }
        }

        return peaks.toIntArray()
    }
}