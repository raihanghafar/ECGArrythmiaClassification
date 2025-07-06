package com.example.ecgarrhythmiaclassification.ml

import android.util.Log
import com.example.ecgarrhythmiaclassification.data.ECGData

data class PreprocessedData(
    val originalSignal: DoubleArray,
    val normalizedSignal: DoubleArray,
    val rPeaks: IntArray,
    val scalograms: Array<Array<FloatArray>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreprocessedData

        if (!originalSignal.contentEquals(other.originalSignal)) return false
        if (!normalizedSignal.contentEquals(other.normalizedSignal)) return false
        if (!rPeaks.contentEquals(other.rPeaks)) return false
        if (!scalograms.contentDeepEquals(other.scalograms)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originalSignal.contentHashCode()
        result = 31 * result + normalizedSignal.contentHashCode()
        result = 31 * result + rPeaks.contentHashCode()
        result = 31 * result + scalograms.contentDeepHashCode()
        return result
    }
}

class ECGPreprocessor {
    private val tompkinsDetector = TompkinsDetector()
    private val waveletTransform = WaveletTransform()
    private val samplingRate = 360.0
    private val tolerance = 0.05

    fun preprocessECGData(ecgData: ECGData): PreprocessedData {
        // Use primary signal instead of mliiSignal
        val signal = ecgData.primarySignal

        Log.i("ECGPreprocessor", "Processing ${ecgData.primaryLeadName} signal with ${signal.size} samples")

        // Rest of the method remains the same
        val rPeaks = tompkinsDetector.detectQRS(signal)
        Log.i("ECGPreprocessor", "Found ${rPeaks.size} R-peaks")

        val denoisedSignal = applyMedianFilter(signal)
        val alignedRPeaks = alignRPeaks(denoisedSignal, rPeaks)
        val normalizedSignal = normalizeSignal(denoisedSignal, alignedRPeaks)

        val scalograms = waveletTransform.continuousWaveletTransformTest(
            normalizedSignal, alignedRPeaks
        )

        Log.i("ECGPreprocessor", "Generated ${scalograms.size} scalograms from ${ecgData.primaryLeadName}")

        return PreprocessedData(
            originalSignal = signal,
            normalizedSignal = normalizedSignal,
            rPeaks = alignedRPeaks,
            scalograms = scalograms
        )
    }

    // ... rest of methods tetap sama
    private fun applyMedianFilter(signal: DoubleArray): DoubleArray {
        val filter200ms = (0.2 * samplingRate).toInt() - 1
        val filter600ms = (0.6 * samplingRate).toInt() - 1

        val firstFilter = medianFilter(signal, filter200ms)
        val baseline = medianFilter(firstFilter, filter600ms)

        return signal.zip(baseline) { sig, base -> sig - base }.toDoubleArray()
    }

    private fun medianFilter(signal: DoubleArray, windowSize: Int): DoubleArray {
        val result = DoubleArray(signal.size)
        val halfWindow = windowSize / 2

        for (i in signal.indices) {
            val start = maxOf(0, i - halfWindow)
            val end = minOf(signal.size, i + halfWindow + 1)
            val window = signal.sliceArray(start until end).sorted()
            result[i] = window[window.size / 2]
        }

        return result
    }

    private fun alignRPeaks(signal: DoubleArray, rPeaks: IntArray): IntArray {
        val alignedPeaks = mutableListOf<Int>()
        val toleranceSamples = (tolerance * samplingRate).toInt()

        for (peak in rPeaks) {
            val leftBound = maxOf(peak - toleranceSamples, 0)
            val rightBound = minOf(peak + toleranceSamples, signal.size - 1)

            var maxIdx = peak
            var maxVal = signal[peak]

            for (i in leftBound..rightBound) {
                if (signal[i] > maxVal) {
                    maxVal = signal[i]
                    maxIdx = i
                }
            }

            alignedPeaks.add(maxIdx)
        }

        return alignedPeaks.toIntArray()
    }

    private fun normalizeSignal(signal: DoubleArray, rPeaks: IntArray): DoubleArray {
        val rPeakValues = rPeaks.map { signal[it] }
        val meanRPeakValue = rPeakValues.average()

        return signal.map { it / meanRPeakValue }.toDoubleArray()
    }
}
