package com.example.ecgarrhythmiaclassification.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class ClassificationOutput(
    val predictions: Array<FloatArray>,
    val confidences: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassificationOutput

        if (!predictions.contentDeepEquals(other.predictions)) return false
        if (!confidences.contentEquals(other.confidences)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = predictions.contentDeepHashCode()
        result = 31 * result + confidences.contentHashCode()
        return result
    }
}

class TFLiteClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val labels = arrayOf("N", "S", "V", "F", "Q")

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("wavelet_cwt.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.i("TFLiteClassifier", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Error loading model", e)
        }
    }

    private fun loadModelFile(filename: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classifyHeartbeats(scalograms: Array<Array<FloatArray>>): ClassificationOutput {
        val results = mutableListOf<FloatArray>()
        val confidences = mutableListOf<Float>()

        interpreter?.let { interp ->
            Log.d("TFLiteClassifier", "Processing ${scalograms.size} scalograms")

            for ((index, scalogram) in scalograms.withIndex()) {
                val inputArray = Array(1) { Array(100) { Array(100) { FloatArray(1) } } }

                for (i in 0 until 100) {
                    for (j in 0 until 100) {
                        inputArray[0][i][j][0] = scalogram[i][j]
                    }
                }

                val outputArray = Array(1) { FloatArray(5) }

                interp.run(inputArray, outputArray)

                val prediction = outputArray[0]
                results.add(prediction.copyOf())

                val maxProb = prediction.maxOrNull() ?: 0f
                confidences.add(maxProb)

                val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
                Log.v("TFLiteClassifier",
                    "Heartbeat $index: Class=${labels[predictedClass]}, Confidence=${maxProb}")
            }
        }

        Log.i("TFLiteClassifier", "Classification completed for ${results.size} heartbeats")

        return ClassificationOutput(
            predictions = results.toTypedArray(),
            confidences = confidences.toFloatArray()
        )
    }

    fun close() {
        interpreter?.close()
    }
}
