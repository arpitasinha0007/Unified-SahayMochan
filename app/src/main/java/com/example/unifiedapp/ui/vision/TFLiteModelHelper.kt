package com.example.unifiedapp.ui.vision

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TFLiteModelHelper(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteModelHelper"
        private const val ANXIETY_MODEL_FILE = "new_hybrid_anxiety_model.tflite"

        private const val ANXIETY_SEQUENCE_LENGTH = 25
        private const val ANXIETY_FEATURE_COUNT = 8
        private const val ANXIETY_STATS_COUNT = 32

        private val ANXIETY_AU_FEATURES = arrayOf(
            "AU01_r", "AU02_r", "AU04_r", "AU05_r", "AU06_r", "AU07_r", "AU09_r", "AU10_r"
        )

        private val ANXIETY_STATS_SCALER_MEAN = floatArrayOf(
            0.113843f, 0.063427f, 0.448065f, 0.099194f, 0.264734f, 0.063506f,
            0.516774f, 0.258065f, 0.022493f, 0.012493f, 0.100645f, 0.019516f,
            0.008811f, 0.006056f, 0.038710f, 0.008871f, 0.000000f, 0.000000f,
            0.000000f, 0.000000f, 0.295129f, 0.063165f, 0.530000f, 0.293871f,
            0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.003851f, 0.012753f,
            0.108065f, 0.000645f
        )

        private val ANXIETY_STATS_SCALER_SCALE = floatArrayOf(
            0.098711f, 0.055470f, 0.290255f, 0.091034f, 0.139028f, 0.036625f,
            0.215846f, 0.140837f, 0.045750f, 0.013877f, 0.104663f, 0.044402f,
            0.005746f, 0.002808f, 0.043235f, 0.006440f, 1.000000f, 1.000000f,
            1.000000f, 1.000000f, 0.081938f, 0.026226f, 0.094152f, 0.083412f,
            1.000000f, 1.000000f, 1.000000f, 1.000000f, 0.011420f, 0.032276f,
            0.225952f, 0.002457f
        )
    }

    private var anxietyInterpreter: Interpreter? = null
    private var isAnxietyModelLoaded = false
    private var anxietyModelSource = "assets"

    data class PredictionResult(
        val prediction: String,
        val confidence: Float,
        val probability: Float,
        val classIndex: Int = -1,
        val status: String = "",
        val rawProbability: Float = 0f,
        val score: Int = -1,
        val modelVersion: String = "unknown"
    )

    init {
        loadModels()
    }

    private fun loadModels() {
        try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            val anxietyExists = assetFiles.contains(ANXIETY_MODEL_FILE)
            if (anxietyExists) {
                loadAnxietyModel()
            } else {
                Log.e(TAG, "Anxiety model file not found: $ANXIETY_MODEL_FILE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite models: ${e.message}")
        }
    }

    private fun loadAnxietyModel() {
        try {
            // First, try to load from dynamic location (downloaded updates)
            val dynamicModelsDir = File(context.filesDir, "models")
            val dynamicAnxietyFile = File(dynamicModelsDir, ANXIETY_MODEL_FILE)

            val modelSource = if (dynamicAnxietyFile.exists() && dynamicAnxietyFile.length() > 0) {
                Log.d(TAG, "Found updated anxiety model: ${dynamicAnxietyFile.absolutePath}")
                anxietyModelSource = "dynamic"
                loadModelFile(dynamicAnxietyFile)
            } else {
                Log.d(TAG, "Loading original anxiety model from assets")
                anxietyModelSource = "assets"
                loadModelFileFromAssets(ANXIETY_MODEL_FILE)
            }

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            anxietyInterpreter = Interpreter(modelSource, options)
            isAnxietyModelLoaded = true
            Log.d(TAG, "Anxiety model loaded successfully from: $anxietyModelSource")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load anxiety model: ${e.message}")
            isAnxietyModelLoaded = false
        }
    }

    private fun loadModelFile(file: File): MappedByteBuffer {
        val inputStream = FileInputStream(file)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }

    private fun loadModelFileFromAssets(modelFileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun reloadModelsAfterUpdate() {
        Log.d(TAG, "Reloading anxiety model after update...")
        try {
            // Close existing interpreter
            anxietyInterpreter?.close()
            anxietyInterpreter = null
            isAnxietyModelLoaded = false

            // Check for dynamic model first
            val dynamicModelsDir = File(context.filesDir, "models")
            val dynamicAnxietyFile = File(dynamicModelsDir, ANXIETY_MODEL_FILE)

            val modelSource = if (dynamicAnxietyFile.exists() && dynamicAnxietyFile.length() > 0) {
                Log.d(TAG, "Loading updated anxiety model: ${dynamicAnxietyFile.absolutePath}")
                anxietyModelSource = "dynamic"
                loadModelFile(dynamicAnxietyFile)
            } else {
                Log.d(TAG, "Loading original anxiety model from assets")
                anxietyModelSource = "assets"
                loadModelFileFromAssets(ANXIETY_MODEL_FILE)
            }

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            anxietyInterpreter = Interpreter(modelSource, options)
            isAnxietyModelLoaded = true

            Log.d(TAG, "Model reload completed successfully - using: $anxietyModelSource")
        } catch (e: Exception) {
            Log.e(TAG, "Error during model reload: ${e.message}")
            // If reload fails, try to restore original
            loadAnxietyModel()
        }
    }

    private fun calculateStatisticalFeatures(frameDataList: List<Map<String, Float>>): FloatArray {
        Log.d("ANXIETY_MODEL", "inside calculateStatisticalFeatures....")

        if (frameDataList.isEmpty()) {
            return FloatArray(ANXIETY_STATS_COUNT) { 0f }
        }
        val stats = FloatArray(ANXIETY_STATS_COUNT)
        var index = 0
        ANXIETY_AU_FEATURES.forEach { auKey ->
            val auValues = frameDataList.mapNotNull { frame ->
                frame[auKey]?.takeIf { it.isFinite() && it >= 0f }
            }
            if (auValues.isNotEmpty()) {
                val mean = auValues.average().toFloat()
                val variance = auValues.map { (it - mean) * (it - mean) }.average()
                val std = sqrt(variance).toFloat()
                val max = auValues.maxOrNull() ?: 0f
                val median = auValues.sorted().let { sorted ->
                    if (sorted.size % 2 == 0) (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
                    else sorted[sorted.size / 2]
                }
                stats[index++] = mean
                stats[index++] = std
                stats[index++] = max
                stats[index++] = median
            } else {
                stats[index++] = 0f
                stats[index++] = 0f
                stats[index++] = 0f
                stats[index++] = 0f
            }
        }
        return stats
    }

    private fun scaleStatisticalFeatures(rawStats: FloatArray): FloatArray {
        val scaledStats = FloatArray(ANXIETY_STATS_COUNT)
        for (i in 0 until ANXIETY_STATS_COUNT) {
            val mean = ANXIETY_STATS_SCALER_MEAN[i]
            val scale = ANXIETY_STATS_SCALER_SCALE[i]
            scaledStats[i] = if (scale != 0f) (rawStats[i] - mean) / scale else rawStats[i] - mean
        }
        return scaledStats
    }

    private fun prepareSequentialAnxietyFeatures(frameDataList: List<Map<String, Float>>): List<FloatArray>? {

        try {
            val framesToUse = if (frameDataList.size >= ANXIETY_SEQUENCE_LENGTH) {
                frameDataList.takeLast(ANXIETY_SEQUENCE_LENGTH)
            } else {
                val repeatedFrames = mutableListOf<Map<String, Float>>()
                repeat(ANXIETY_SEQUENCE_LENGTH) { i ->
                    val frameIndex = i % frameDataList.size
                    repeatedFrames.add(frameDataList[frameIndex])
                }
                repeatedFrames
            }
            val sequentialFeatures = mutableListOf<FloatArray>()
            framesToUse.forEach { frameData ->
                val frameFeatures = FloatArray(ANXIETY_FEATURE_COUNT)
                ANXIETY_AU_FEATURES.forEachIndexed { index, auKey ->
                    frameFeatures[index] = frameData[auKey] ?: 0f
                }
                sequentialFeatures.add(frameFeatures)
            }
            return sequentialFeatures
        } catch (e: Exception) {
            return null
        }
    }

    private fun runHybridModelInference(
        sequentialFeatures: List<FloatArray>,
        statisticalFeatures: FloatArray
    ): Float? {

        Log.d("ANXIETY_MODEL", "outside try block of runHybridModelInference prediction")
        return try {
            Log.d("ANXIETY_MODEL", "Running REAL model inference")

            val sequentialInputSize = 1 * ANXIETY_SEQUENCE_LENGTH * ANXIETY_FEATURE_COUNT
            val sequentialBuffer = ByteBuffer.allocateDirect(4 * sequentialInputSize).apply {
                order(ByteOrder.nativeOrder())
                sequentialFeatures.forEach { frameFeatures ->
                    frameFeatures.forEach { value -> putFloat(value) }
                }
            }

            val statsInputSize = 1 * ANXIETY_STATS_COUNT
            val statsBuffer = ByteBuffer.allocateDirect(4 * statsInputSize).apply {
                order(ByteOrder.nativeOrder())
                statisticalFeatures.forEach { value -> putFloat(value) }
            }

            val outputBuffer = ByteBuffer.allocateDirect(4 * 1).apply {
                order(ByteOrder.nativeOrder())
            }

            val inputs = arrayOf(sequentialBuffer, statsBuffer)
            val outputs = mapOf(0 to outputBuffer)

            try {
                anxietyInterpreter?.runForMultipleInputsOutputs(inputs, outputs)
            } catch (e: Exception) {
                Log.d("ANXIETY_MODEL", e.printStackTrace().toString())

                return null
            }

            outputBuffer.rewind()
            val rawOutput = outputBuffer.float

            if (!rawOutput.isFinite()) null else rawOutput
        } catch (e: Exception) {
            null
        }
    }

    private fun processHybridModelOutput(rawOutput: Float, frameCount: Int): PredictionResult {

        Log.d("ANXIETY_MODEL", "process hybrid model....")
        val clampedScore = rawOutput.coerceIn(0f, 21f).toInt()
        val (predictionLabel, predictionDescription) = when {
            clampedScore <= 9 -> "Low (0-9)" to "Minimal to mild anxiety"
            clampedScore <= 14 -> "Moderate (10-14)" to "Moderate anxiety"
            else -> "High (15-21)" to "Severe anxiety"
        }
        val confidence = when {
            clampedScore <= 9 -> 0.88f
            clampedScore <= 14 -> 0.85f
            else -> 0.92f
        }
        return PredictionResult(
            prediction = predictionLabel,
            confidence = confidence,
            probability = (clampedScore / 21f).coerceIn(0f, 1f),
            classIndex = when {
                clampedScore <= 9 -> 0
                clampedScore <= 14 -> 1
                else -> 2
            },
            status = "Hybrid GAD-7 Score: $clampedScore/21 - $predictionDescription",
            rawProbability = rawOutput,
            score = clampedScore,
            modelVersion = anxietyModelSource
        )
    }

    fun predictAnxiety(frameDataList: List<Map<String, Float>>): PredictionResult? {

        Log.d("ANXIETY_MODEL", "outside try block of predictAnxiety prediction")
        if (!isAnxietyModelLoaded || anxietyInterpreter == null) {
            return createMockAnxietyPrediction(frameDataList)
        }
        if (frameDataList.isEmpty()) {
            return null
        }
        return try {

            Log.d("ANXIETY_MODEL", "Using anxiety prediction")

            val sequentialFeatures = prepareSequentialAnxietyFeatures(frameDataList)
            if (sequentialFeatures == null || sequentialFeatures.size != ANXIETY_SEQUENCE_LENGTH) {
                return createMockAnxietyPrediction(frameDataList)
            }
            val rawStatisticalFeatures = calculateStatisticalFeatures(frameDataList)
            val scaledStatisticalFeatures = scaleStatisticalFeatures(rawStatisticalFeatures)
            val result = runHybridModelInference(sequentialFeatures, scaledStatisticalFeatures)
            if (result == null) {
                return createMockAnxietyPrediction(frameDataList)
            }
            processHybridModelOutput(result, frameDataList.size)
        } catch (e: Exception) {
            Log.d("ANXIETY_MODEL", e.printStackTrace().toString())
            return createMockAnxietyPrediction(frameDataList)
        }
    }

    private fun createMockAnxietyPrediction(frameDataList: List<Map<String, Float>>): PredictionResult? {




        Log.d("ANXIETY_MODEL", "outside try block of MOCK fallback prediction")

        if (frameDataList.isEmpty()) return null
        try {

            Log.d("ANXIETY_MODEL", "Using MOCK fallback prediction")

            val rawStats = calculateStatisticalFeatures(frameDataList)
            val au04Stats = rawStats.slice(8..11)
            val au01Stats = rawStats.slice(0..3)
            val au07Stats = rawStats.slice(20..23)
            val mockScore = ((au04Stats[0] * 3.0f) + (au04Stats[2] * 1.5f) + (au01Stats[0] * 2.0f) + (au07Stats[0] * 1.8f)) * 2.5f
            val clampedScore = mockScore.coerceIn(0f, 21f).toInt()
            val predictionLabel = when {
                clampedScore <= 9 -> "Low (0-9)"
                clampedScore <= 14 -> "Moderate (10-14)"
                else -> "High (15-21)"
            }
            return PredictionResult(
                prediction = predictionLabel,
                confidence = 0.70f,
                probability = clampedScore / 21f,
                status = "Mock Hybrid GAD-7 Score: $clampedScore/21 - $predictionLabel (Fallback)",
                score = clampedScore,
                rawProbability = mockScore,
                modelVersion = anxietyModelSource
            )
        } catch (e: Exception) {
            Log.d("ANXIETY_MODEL", e.printStackTrace().toString())
            return null
        }
    }

    fun getModelStatus(): Map<String, String> {
        return mapOf(
            "anxiety_source" to anxietyModelSource,
            "anxiety_loaded" to isAnxietyModelLoaded.toString(),
            "anxiety_config" to "Hybrid dual-input model"
        )
    }

    fun getCurrentModelVersion(): String {
        return try {
            val internalDir = context.filesDir
            val downloadedModel = File(internalDir, "anxiety_model_downloaded.tflite")

            if (downloadedModel.exists()) {
                // Check if we have version info stored
                val versionFile = File(internalDir, "model_version.txt")
                val version = if (versionFile.exists()) {
                    versionFile.readText().trim()
                } else {
                    "downloaded"
                }
                "v$version (server)"
            } else {
                "assets (original)"
            }
        } catch (e: Exception) {
            Log.e("TFLiteModelHelper", "Error getting model version: ${e.message}")
            "unknown"
        }
    }


    fun close() {

        Log.d("ANXIETY_MODEL", "outside try block of close()")
        try {
            Log.d("ANXIETY_MODEL", "inside try block of close()")
            anxietyInterpreter?.close()
            anxietyInterpreter = null
            isAnxietyModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TFLite models: ${e.message}")
        }
    }
}