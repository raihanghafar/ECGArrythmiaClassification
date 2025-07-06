package com.example.ecgarrhythmiaclassification.data

data class ECGData(
    val timeMs: DoubleArray,
    val primarySignal: DoubleArray,
    val secondarySignal: DoubleArray? = null,
    val primaryLeadName: String,
    val secondaryLeadName: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ECGData

        if (!timeMs.contentEquals(other.timeMs)) return false
        if (!primarySignal.contentEquals(other.primarySignal)) return false
        if (secondarySignal != null) {
            if (other.secondarySignal == null) return false
            if (!secondarySignal.contentEquals(other.secondarySignal)) return false
        } else if (other.secondarySignal != null) return false
        if (primaryLeadName != other.primaryLeadName) return false
        if (secondaryLeadName != other.secondaryLeadName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timeMs.contentHashCode()
        result = 31 * result + primarySignal.contentHashCode()
        result = 31 * result + (secondarySignal?.contentHashCode() ?: 0)
        result = 31 * result + primaryLeadName.hashCode()
        result = 31 * result + (secondaryLeadName?.hashCode() ?: 0)
        return result
    }
}