package com.example.ecgarrhythmiaclassification.utils

import android.util.Log
import com.opencsv.CSVReader
import com.example.ecgarrhythmiaclassification.data.ECGData
import java.io.InputStream
import java.io.InputStreamReader

class CSVReader {
    private val preferredLeads = listOf("MLII", "V5", "V1", "V2", "V4") // Priority order

    fun readMITBIHData(inputStream: InputStream): ECGData {
        val reader = CSVReader(InputStreamReader(inputStream))
        var records: List<Array<String>>

        try {
            records = reader.readAll()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid CSV format: ${e.message}")
        }

        if (records.isEmpty()) {
            throw IllegalArgumentException("CSV file is empty")
        }

        val header = records[0]
        Log.i("CSVReader", "CSV Header: ${header.joinToString()}")

        // Find time column
        val timeMsIdx = header.indexOfFirst {
            it.contains("time_ms", ignoreCase = true) || it.contains("time", ignoreCase = true)
        }

        if (timeMsIdx == -1) {
            throw IllegalArgumentException("CSV must contain time_ms column")
        }

        // Find available ECG leads
        val availableLeads = mutableListOf<Pair<String, Int>>()
        for (leadName in preferredLeads) {
            val idx = header.indexOfFirst { it.equals(leadName, ignoreCase = true) }
            if (idx != -1) {
                availableLeads.add(leadName to idx)
            }
        }

        // If no preferred leads found, look for any signal columns (excluding time and index)
        if (availableLeads.isEmpty()) {
            for (i in header.indices) {
                val columnName = header[i].trim()
                if (columnName.isNotEmpty() &&
                    !columnName.contains("time", ignoreCase = true) &&
                    !columnName.matches(Regex("\\d+")) && // Skip numeric indices
                    columnName != "") {
                    availableLeads.add(columnName to i)
                }
            }
        }

        if (availableLeads.isEmpty()) {
            throw IllegalArgumentException("No ECG signal columns found in CSV")
        }

        // Select primary and secondary signals
        val primaryLead = availableLeads[0]
        val secondaryLead = if (availableLeads.size > 1) availableLeads[1] else null

        Log.i("CSVReader", "Primary lead: ${primaryLead.first} (column ${primaryLead.second})")
        secondaryLead?.let {
            Log.i("CSVReader", "Secondary lead: ${it.first} (column ${it.second})")
        }

        val dataRows = records.drop(1)
        val timeMs = mutableListOf<Double>()
        val primarySignal = mutableListOf<Double>()
        val secondarySignal = if (secondaryLead != null) mutableListOf<Double>() else null

        for ((rowIndex, row) in dataRows.withIndex()) {
            try {
                val maxColumnIndex = listOfNotNull(
                    timeMsIdx,
                    primaryLead.second,
                    secondaryLead?.second
                ).maxOrNull() ?: 0

                if (row.size > maxColumnIndex) {
                    timeMs.add(row[timeMsIdx].trim().toDouble())
                    primarySignal.add(row[primaryLead.second].trim().toDouble())
                    secondarySignal?.add(row[secondaryLead!!.second].trim().toDouble())
                }
            } catch (e: NumberFormatException) {
                Log.w("CSVReader", "Skipping invalid row $rowIndex: ${row.joinToString()}")
            }
        }

        if (timeMs.size < 1000) {
            throw IllegalArgumentException("Insufficient data points. Need at least 1000 samples, got ${timeMs.size}.")
        }

        Log.i("CSVReader", "Successfully loaded ${timeMs.size} data points from ${primaryLead.first}")

        return ECGData(
            timeMs = timeMs.toDoubleArray(),
            primarySignal = primarySignal.toDoubleArray(),
            secondarySignal = secondarySignal?.toDoubleArray(),
            primaryLeadName = primaryLead.first,
            secondaryLeadName = secondaryLead?.first
        )
    }
}