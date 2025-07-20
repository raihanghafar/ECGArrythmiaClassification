package com.example.ecgarrhythmiaclassification.ml

import kotlin.math.*

class TompkinsDetector {
    private val samplingRate = 360.0

    fun detectQRS(signal: DoubleArray): IntArray {
        val bpass = bandPassFilter(signal)
        val der = derivative(bpass)
        val sqr = squaring(der)
        val mwin = movingWindowIntegration(sqr)

        val heartRate = HeartRate(mwin, bpass, samplingRate)
        return heartRate.findRPeaks()
    }

    private fun bandPassFilter(signal: DoubleArray): DoubleArray {
        val sig = signal.copyOf()
        for (i in signal.indices) {
            sig[i] = signal[i]
            if (i >= 1) sig[i] += 2 * sig[i - 1]
            if (i >= 2) sig[i] -= sig[i - 2]
            if (i >= 6) sig[i] -= 2 * signal[i - 6]
            if (i >= 12) sig[i] += signal[i - 12]
        }
        val result = sig.copyOf()
        for (i in signal.indices) {
            result[i] = -sig[i]
            if (i >= 1) result[i] -= result[i - 1]
            if (i >= 16) result[i] += 32 * sig[i - 16]
            if (i >= 32) result[i] += sig[i - 32]
        }
        val maxVal = result.maxOf { abs(it) }
        return result.map { it / maxVal }.toDoubleArray()
    }

    private fun derivative(signal: DoubleArray): DoubleArray {
        val result = DoubleArray(signal.size)
        for (i in signal.indices) {
            result[i] = 0.0
            if (i >= 1) result[i] -= 2 * signal[i - 1]
            if (i >= 2) result[i] -= signal[i - 2]
            if (i >= 2 && i <= signal.size - 2) result[i] += 2 * signal[i + 1]
            if (i >= 2 && i <= signal.size - 3) result[i] += signal[i + 2]
            result[i] = (result[i] * samplingRate) / 8
        }
        return result
    }

    private fun squaring(signal: DoubleArray): DoubleArray =
        signal.map { it * it }.toDoubleArray()

    private fun movingWindowIntegration(signal: DoubleArray): DoubleArray {
        val result = DoubleArray(signal.size)
        val winSize = (0.150 * samplingRate).toInt()
        var sum = 0.0
        for (j in 0 until winSize) {
            sum += signal[j] / winSize
            result[j] = sum
        }
        for (i in winSize until signal.size) {
            sum += signal[i] / winSize
            sum -= signal[i - winSize] / winSize
            result[i] = sum
        }
        return result
    }

    // --- HeartRate class (inner, matches Python logic) ---
    private class HeartRate(
        private val mwin: DoubleArray,
        private val bpass: DoubleArray,
        private val sampFreq: Double
    ) {
        private val win150ms = (0.15 * sampFreq).roundToInt()
        private val win200ms = (0.2 * sampFreq).roundToInt()
        private val peaks = mutableListOf<Int>()
        private val probablePeaks = mutableListOf<Int>()
        private val rLocs = mutableListOf<Int>()
        private val result = mutableListOf<Int>()
        private var SPKI = 0.0
        private var NPKI = 0.0
        private var ThresholdI1 = 0.0
        private var ThresholdI2 = 0.0
        private var SPKF = 0.0
        private var NPKF = 0.0
        private var ThresholdF1 = 0.0
        private var ThresholdF2 = 0.0
        private var TWave = false
        private var RRLowLimit = 0.0
        private var RRHighLimit = 0.0
        private var RRMissedLimit = 0.0
        private var RRAverage1 = 0.0
        private val RR1 = mutableListOf<Double>()
        private val RR2 = mutableListOf<Double>()

        fun findRPeaks(): IntArray {
            approxPeak()
            for (ind in peaks.indices) {
                val peakVal = peaks[ind]
                val win300ms = (maxOf(0, peaks[ind] - win150ms)..minOf(peaks[ind] + win150ms, bpass.size - 1))
                val maxVal = win300ms.map { bpass[it] }.maxOrNull() ?: 0.0
                if (maxVal != 0.0) {
                    val xCoord = bpass.indexOfFirst { it == maxVal }
                    probablePeaks.add(xCoord)
                }
                if (ind < probablePeaks.size && ind != 0) {
                    adjustRRInterval(ind)
                    if (RRAverage1 < RRLowLimit || RRAverage1 > RRMissedLimit) {
                        ThresholdI1 /= 2
                        ThresholdF1 /= 2
                    }
                    val RRn = RR1.lastOrNull() ?: 0.0
                    searchback(peakVal, RRn, (RRn * sampFreq).roundToInt())
                    findTWave(peakVal, RRn, ind, ind - 1)
                } else {
                    adjustThresholds(peakVal, ind)
                }
                updateThresholds()
            }
            ecgSearchback()
            return result.toIntArray()
        }

        private fun approxPeak() {
            val slopes = fftConvolve(mwin, DoubleArray(25) { 1.0 / 25 })
            for (i in (0.5 * sampFreq).roundToInt() + 1 until slopes.size - 1) {
                if (slopes[i] > slopes[i - 1] && slopes[i + 1] < slopes[i]) {
                    peaks.add(i)
                }
            }
        }

        private fun adjustRRInterval(ind: Int) {
            RR1.clear()
            val start = maxOf(0, ind - 8)
            val diffs = peaks.subList(start, ind + 1).zipWithNext { a, b -> (b - a) / sampFreq }
            RR1.addAll(diffs)
            RRAverage1 = RR1.average()
            var RRAverage2 = RRAverage1
            if (ind >= 8) {
                RR2.clear()
                for (i in 0 until 8) {
                    if (RR1[i] > RRLowLimit && RR1[i] < RRHighLimit) {
                        RR2.add(RR1[i])
                        if (RR2.size > 8) {
                            RR2.removeAt(0)
                            RRAverage2 = RR2.average()
                        }
                    }
                }
            }
            if (RR2.size > 7 || ind < 8) {
                RRLowLimit = 0.92 * RRAverage2
                RRHighLimit = 1.16 * RRAverage2
                RRMissedLimit = 1.66 * RRAverage2
            }
        }

        private fun searchback(peakVal: Int, RRn: Double, sbWin: Int) {
            if (RRn > RRMissedLimit) {
                val winRR = mwin.slice((peakVal - sbWin + 1).coerceAtLeast(0)..peakVal)
                val coord = winRR.withIndex().filter { it.value > ThresholdI1 }.map { it.index }
                val xMax = coord.maxByOrNull { winRR[it] } ?: -1
                if (xMax != -1) {
                    SPKI = 0.25 * mwin[xMax] + 0.75 * SPKI
                    ThresholdI1 = NPKI + 0.25 * (SPKI - NPKI)
                    ThresholdI2 = 0.5 * ThresholdI1
                    val winB = bpass.slice((xMax - win150ms).coerceAtLeast(0)..minOf(bpass.size - 1, xMax))
                    val coordB = winB.withIndex().filter { it.value > ThresholdF1 }.map { it.index }
                    val rMax = coordB.maxByOrNull { winB[it] } ?: -1
                    if (rMax != -1 && winB[rMax] > ThresholdF2) {
                        SPKF = 0.25 * winB[rMax] + 0.75 * SPKF
                        ThresholdF1 = NPKF + 0.25 * (SPKF - NPKF)
                        ThresholdF2 = 0.5 * ThresholdF1
                        rLocs.add(rMax)
                    }
                }
            }
        }

        private fun findTWave(peakVal: Int, RRn: Double, ind: Int, prevInd: Int) {
            if (mwin[peakVal] >= ThresholdI1) {
                if (ind > 0 && RRn > 0.20 && RRn < 0.36) {
                    val currSlope = mwin.slice((peakVal - win150ms / 2).coerceAtLeast(0)..peakVal).zipWithNext { a, b -> b - a }.maxOrNull() ?: 0.0
                    val lastSlope = mwin.slice((peaks[prevInd] - win150ms / 2).coerceAtLeast(0)..peaks[prevInd]).zipWithNext { a, b -> b - a }.maxOrNull() ?: 0.0
                    if (currSlope < 0.5 * lastSlope) {
                        TWave = true
                        NPKI = 0.125 * mwin[peakVal] + 0.875 * NPKI
                    }
                }
                if (!TWave) {
                    if (probablePeaks[ind] > ThresholdF1) {
                        SPKI = 0.125 * mwin[peakVal] + 0.875 * SPKI
                        SPKF = 0.125 * bpass[ind] + 0.875 * SPKF
                        rLocs.add(probablePeaks[ind])
                    } else {
                        SPKI = 0.125 * mwin[peakVal] + 0.875 * SPKI
                        NPKF = 0.125 * bpass[ind] + 0.875 * NPKF
                    }
                }
            } else if (mwin[peakVal] < ThresholdI1 || (ThresholdI1 < mwin[peakVal] && mwin[peakVal] < ThresholdI2)) {
                NPKI = 0.125 * mwin[peakVal] + 0.875 * NPKI
                NPKF = 0.125 * bpass[ind] + 0.875 * NPKF
            }
        }

        private fun adjustThresholds(peakVal: Int, ind: Int) {
            if (mwin[peakVal] >= ThresholdI1) {
                SPKI = 0.125 * mwin[peakVal] + 0.875 * SPKI
                if (probablePeaks[ind] > ThresholdF1) {
                    SPKF = 0.125 * bpass[ind] + 0.875 * SPKF
                    rLocs.add(probablePeaks[ind])
                } else {
                    NPKF = 0.125 * bpass[ind] + 0.875 * NPKF
                }
            } else if (mwin[peakVal] < ThresholdI2 || (ThresholdI2 < mwin[peakVal] && mwin[peakVal] < ThresholdI1)) {
                NPKI = 0.125 * mwin[peakVal] + 0.875 * NPKI
                NPKF = 0.125 * bpass[ind] + 0.875 * NPKF
            }
        }

        private fun updateThresholds() {
            ThresholdI1 = NPKI + 0.25 * (SPKI - NPKI)
            ThresholdF1 = NPKF + 0.25 * (SPKF - NPKF)
            ThresholdI2 = 0.5 * ThresholdI1
            ThresholdF2 = 0.5 * ThresholdF1
            TWave = false
        }

        private fun ecgSearchback() {
            val uniqueLocs = rLocs.distinct().sorted()
            for (rVal in uniqueLocs) {
                val coord = (rVal - win200ms).coerceAtLeast(0)..minOf(bpass.size - 1, rVal + win200ms)
                val xMax = coord.maxByOrNull { bpass[it] } ?: -1
                if (xMax != -1) result.add(xMax)
            }
        }

        // Simple FFT convolution for approxPeak
        private fun fftConvolve(a: DoubleArray, b: DoubleArray): DoubleArray {
            val n = a.size
            val m = b.size
            val out = DoubleArray(n)
            for (i in 0 until n) {
                var sum = 0.0
                for (j in 0 until m) {
                    if (i - j >= 0) sum += a[i - j] * b[j]
                }
                out[i] = sum
            }
            return out
        }
    }
}