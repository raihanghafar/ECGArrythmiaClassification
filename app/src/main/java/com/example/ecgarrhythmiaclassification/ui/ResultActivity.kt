package com.example.ecgarrhythmiaclassification.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecgarrhythmiaclassification.ui.theme.ECGArrhythmiaClassificationTheme
import com.example.ecgarrhythmiaclassification.data.ClassificationResult
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = intent.getSerializableExtra("result") as? ClassificationResult

        if (result == null) {
            finish()
            return
        }

        setContent {
            ECGArrhythmiaClassificationTheme {
                ResultScreen(
                    result = result,
                    onBackClick = { finish() },
                    onSaveClick = { saveResult(result) }
                )
            }
        }
    }

    private fun saveResult(result: ClassificationResult) {
        // TODO: Implement save functionality
        // For now, just finish the activity
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ClassificationResult,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Classification Results",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Info Card
            FileInfoCard(result = result)

            // Classification Results Card
            ClassificationResultsCard(result = result)

            // Performance Metrics Card
            PerformanceMetricsCard(result = result)
        }
    }
}

@Composable
fun FileInfoCard(result: ClassificationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "File Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "File Name", value = result.fileName)
            InfoRow(label = "Processed At", value = formatTimestamp(result.timestamp))
            InfoRow(label = "Total Beats", value = result.totalBeats.toString())
            InfoRow(
                label = "Average Confidence",
                value = "${String.format("%.1f", result.averageConfidence * 100)}%"
            )
        }
    }
}

@Composable
fun ClassificationResultsCard(result: ClassificationResult) {
    val classificationData = listOf(
        Triple("Normal (N)", result.nCount, Color(0xFF4CAF50)),
        Triple("Supraventricular (S)", result.sCount, Color(0xFFFF9800)),
        Triple("Ventricular (V)", result.vCount, Color(0xFFF44336)),
        Triple("Fusion (F)", result.fCount, Color(0xFF9C27B0)),
        Triple("Unclassified (Q)", result.qCount, Color(0xFF607D8B))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Heartbeat Classification",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Classification grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classificationData.chunked(2).forEach { rowData ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowData.forEach { (label, count, color) ->
                            ClassificationItem(
                                label = label,
                                count = count,
                                color = color,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fill remaining space if odd number
                        if (rowData.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )

            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun PerformanceMetricsCard(result: ClassificationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                label = "Inference Time",
                value = "${result.inferenceTimeMs} ms"
            )
            InfoRow(
                label = "Memory Usage",
                value = "${String.format("%.1f", result.memoryUsageMB)} MB"
            )
            InfoRow(
                label = "CPU Usage",
                value = "${String.format("%.1f", result.cpuUsagePercent)}%"
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
