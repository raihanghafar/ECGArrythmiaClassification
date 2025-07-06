package com.example.ecgarrhythmiaclassification.utils

object Constants {
    const val SAMPLING_RATE = 360.0
    const val TOLERANCE = 0.05
    const val BEFORE_R_PEAK = 90
    const val AFTER_R_PEAK = 110
    const val SCALOGRAM_SIZE = 100

    const val MAX_FILE_SIZE_MB = 100L

    val CLASS_LABELS = arrayOf("N", "S", "V", "F", "Q")
    val CLASS_NAMES = arrayOf(
        "Normal",
        "Supraventricular",
        "Ventricular",
        "Fusion",
        "Unclassified"
    )
}
