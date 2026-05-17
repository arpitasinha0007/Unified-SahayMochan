package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModelHelper(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteModelHelper"
        private const val DEPRESSION_MODEL_FILE = "best_bilstm_transformer_model.tflite"

        private var DEPRESSION_SEQUENCE_LENGTH = 40
        private var DEPRESSION_FEATURE_COUNT = 17

        private val ALL_AU_FEATURES = arrayOf(
            "AU01_r", "AU02_r", "AU04_r", "AU05_r", "AU06_r", "AU07_r",
            "AU09_r", "AU10_r", "AU12_r", "AU14_r", "AU15_r", "AU17_r",
            "AU20_r", "AU23_r", "AU25_r", "AU26_r", "AU45_r"
        )

        private val DEPRESSION_SCALER_MEAN = floatArrayOf(
            0.03007093f, 0.00942261f, 0.11000000f, 0.00944268f, 0.12073521f,
            0.18748200f, 0.00000000f, 0.42035713f, 0.06885481f, 0.63000000f,
            0.05707361f, 0.52195373f, 0.03185095f, 0.07435518f, 0.04621311f,
            0.07596862f, 0.00000000f
        )

        private val DEPRESSION_SCALER_SCALE = floatArrayOf(
            0.31819804f, 0.18307858f, 0.81328385f, 0.15659705f, 0.72000003f,
            0.97572090f, 0.06638029f, 1.05822224f, 0.53522202f, 1.44906432f,
            0.41999999f, 1.03546003f, 0.22660471f, 0.51452545f, 0.42163063f,
            0.56000000f, 0.07483750f
        )
    }

    private var depressionInterpreter: Interpreter? = null
    private var isDepressionModelLoaded = false
    private var depressionModelSource = "assets"

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
            Log.d(TAG, "Assets folder contains: ${assetFiles.joinToString()}")

            val depressionExists = assetFiles.contains(DEPRESSION_MODEL_FILE)
            if (depressionExists) {
                loadDepressionModel()
            } else {
                Log.e(TAG, "Depression model file not found: $DEPRESSION_MODEL_FILE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite models: ${e.message}")
        }
    }

    private fun loadDepressionModel() {
        try {
            // First, try to load from dynamic location (downloaded updates)
            val dynamicModelsDir = File(context.filesDir, "models")
            val dynamicDepressionFile = File(dynamicModelsDir, DEPRESSION_MODEL_FILE)

            val modelSource = if (dynamicDepressionFile.exists() && dynamicDepressionFile.length() > 0) {
                Log.d(TAG, "Found updated depression model: ${dynamicDepressionFile.absolutePath}")
                depressionModelSource = "dynamic"
                loadModelFile(dynamicDepressionFile)
            } else {
                Log.d(TAG, "Loading original depression model from assets")
                depressionModelSource = "assets"
                loadModelFileFromAssets(DEPRESSION_MODEL_FILE)
            }

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            depressionInterpreter = Interpreter(modelSource, options)
            isDepressionModelLoaded = true
            Log.d(TAG, "Depression model loaded successfully from: $depressionModelSource")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load depression model: ${e.message}")
            isDepressionModelLoaded = false
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
        Log.d(TAG, "Reloading depression model after update...")
        try {
            // Close existing interpreter
            depressionInterpreter?.close()
            depressionInterpreter = null
            isDepressionModelLoaded = false

            // Check for dynamic model first
            val dynamicModelsDir = File(context.filesDir, "models")
            val dynamicDepressionFile = File(dynamicModelsDir, DEPRESSION_MODEL_FILE)

            val modelSource = if (dynamicDepressionFile.exists() && dynamicDepressionFile.length() > 0) {
                Log.d(TAG, "Loading updated depression model: ${dynamicDepressionFile.absolutePath}")
                depressionModelSource = "dynamic"
                loadModelFile(dynamicDepressionFile)
            } else {
                Log.d(TAG, "Loading original depression model from assets")
                depressionModelSource = "assets"
                loadModelFileFromAssets(DEPRESSION_MODEL_FILE)
            }

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            depressionInterpreter = Interpreter(modelSource, options)
            isDepressionModelLoaded = true

            Log.d(TAG, "Model reload completed successfully - using: $depressionModelSource")
        } catch (e: Exception) {
            Log.e(TAG, "Error during model reload: ${e.message}")
            // If reload fails, try to restore original
            loadDepressionModel()
        }
    }

    fun predictDepression(frameDataList: List<Map<String, Float>>): PredictionResult? {
        if (!isDepressionModelLoaded || depressionInterpreter == null) {
            Log.e(TAG, "BiLSTM depression model not loaded")
            return createMockDepressionPrediction(frameDataList)
        }
        return try {
            Log.d(TAG, "BiLSTM TRANSFORMER DEPRESSION PREDICTION")
            if (frameDataList.isEmpty()) {
                Log.e(TAG, "No frame data received for BiLSTM depression prediction")
                return null
            }

            val features = if (DEPRESSION_SEQUENCE_LENGTH == 1) {
                prepareSingleFrameBiLSTMFeatures(frameDataList)
            } else {
                prepareSequentialBiLSTMFeatures(frameDataList)
            }

            if (features == null) {
                Log.e(TAG, "Failed to prepare BiLSTM features")
                return createMockDepressionPrediction(frameDataList)
            }

            val totalInputSize = DEPRESSION_SEQUENCE_LENGTH * DEPRESSION_FEATURE_COUNT
            val inputBuffer = ByteBuffer.allocateDirect(4 * totalInputSize).apply {
                order(ByteOrder.nativeOrder())
                if (DEPRESSION_SEQUENCE_LENGTH == 1) {
                    (features as FloatArray).forEach { putFloat(it) }
                } else {
                    (features as List<FloatArray>).forEach { frameFeatures ->
                        frameFeatures.forEach { putFloat(it) }
                    }
                }
            }

            val outputBuffer = ByteBuffer.allocateDirect(4 * 1).apply { order(ByteOrder.nativeOrder()) }
            val startTime = System.currentTimeMillis()
            depressionInterpreter?.run(inputBuffer, outputBuffer)
            val inferenceTime = System.currentTimeMillis() - startTime

            outputBuffer.rewind()
            val predictedScore = outputBuffer.float
            val clampedScore = predictedScore.coerceIn(0f, 24f).toInt()
            Log.d(TAG, "BiLSTM inference completed in ${inferenceTime}ms. Raw: $predictedScore, Clamped: $clampedScore")

            val (predictionLabel, predictionDescription) = when {
                clampedScore <= 9 -> "Low (0-9)" to "Minimal to mild depression"
                clampedScore <= 14 -> "Moderate (10-14)" to "Moderate depression"
                else -> "High (15-24)" to "Moderately severe to severe"
            }
            val confidence = when {
                clampedScore <= 9 -> 0.85f
                clampedScore <= 14 -> 0.80f
                else -> 0.90f
            }

            val result = PredictionResult(
                prediction = predictionLabel,
                confidence = confidence,
                probability = predictedScore / 24f,
                status = "PHQ-8 Score: $clampedScore/24 - $predictionDescription",
                score = clampedScore,
                rawProbability = predictedScore,
                modelVersion = depressionModelSource
            )

            Log.d(TAG, "BiLSTM depression prediction: ${result.status}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error during BiLSTM depression prediction: ${e.message}")
            e.printStackTrace()
            createMockDepressionPrediction(frameDataList)
        }
    }

    private fun prepareSingleFrameBiLSTMFeatures(frameDataList: List<Map<String, Float>>): FloatArray? {
        try {
            val avgAUValues = ALL_AU_FEATURES.associateWith { auKey ->
                val values = frameDataList.mapNotNull { frame ->
                    frame[auKey]?.takeIf { it.isFinite() && it >= 0f }
                }
                if (values.isNotEmpty()) values.average().toFloat() else 0f
            }
            val features = FloatArray(DEPRESSION_FEATURE_COUNT)
            ALL_AU_FEATURES.forEachIndexed { index, auKey ->
                val auValue = avgAUValues[auKey] ?: 0.0f
                val center = DEPRESSION_SCALER_MEAN[index]
                val scale = DEPRESSION_SCALER_SCALE[index]
                features[index] = if (scale != 0.0f) (auValue - center) / scale else auValue - center
            }
            return features
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing single-frame BiLSTM features: ${e.message}")
            return null
        }
    }

    private fun prepareSequentialBiLSTMFeatures(frameDataList: List<Map<String, Float>>): List<FloatArray>? {
        try {
            val sequentialFeatures = mutableListOf<FloatArray>()
            val framesToUse = if (frameDataList.size >= DEPRESSION_SEQUENCE_LENGTH) {
                frameDataList.takeLast(DEPRESSION_SEQUENCE_LENGTH)
            } else {
                List(DEPRESSION_SEQUENCE_LENGTH) { i -> frameDataList[i % frameDataList.size] }
            }

            framesToUse.forEach { frameData ->
                val frameFeatures = FloatArray(DEPRESSION_FEATURE_COUNT)
                ALL_AU_FEATURES.forEachIndexed { featureIndex, auKey ->
                    if (featureIndex < DEPRESSION_FEATURE_COUNT) {
                        val auValue = frameData[auKey] ?: 0.0f
                        val center = DEPRESSION_SCALER_MEAN[featureIndex]
                        val scale = DEPRESSION_SCALER_SCALE[featureIndex]
                        frameFeatures[featureIndex] = if (scale != 0.0f) (auValue - center) / scale else auValue - center
                    }
                }
                sequentialFeatures.add(frameFeatures)
            }
            return sequentialFeatures
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing sequential BiLSTM features: ${e.message}")
            return null
        }
    }

    private fun createMockDepressionPrediction(frameDataList: List<Map<String, Float>>): PredictionResult? {
        if (frameDataList.isEmpty()) return null
        Log.d(TAG, "Creating mock BiLSTM depression prediction...")
        try {
            val framesToAnalyze = frameDataList.takeLast(10)
            val avgAUValues = ALL_AU_FEATURES.associateWith { auKey ->
                val values = framesToAnalyze.mapNotNull { it[auKey] }.filter { it > 0f }
                if (values.isNotEmpty()) values.average().toFloat() else 0f
            }
            val au12 = avgAUValues["AU12_r"] ?: 0f
            val au15 = avgAUValues["AU15_r"] ?: 0f
            val au04 = avgAUValues["AU04_r"] ?: 0f
            val depressionScore = ((au15 * 1.5f) + (au04 * 1.2f) - (au12 * 1.3f)) * 8f
            val clampedScore = depressionScore.coerceIn(0f, 24f).toInt()
            val predictionLabel = when {
                clampedScore <= 9 -> "Low (0-9)"
                clampedScore <= 14 -> "Moderate (10-14)"
                else -> "High (15-24)"
            }
            return PredictionResult(
                prediction = predictionLabel,
                confidence = 0.65f,
                probability = clampedScore / 24f,
                status = "Mock PHQ-8 Score: $clampedScore/24 - $predictionLabel (BiLSTM Fallback)",
                score = clampedScore,
                rawProbability = depressionScore,
                modelVersion = depressionModelSource
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating mock BiLSTM prediction: ${e.message}")
            return null
        }
    }

    // ========== DEBUG FUNCTIONS ==========

    /**
     * Check if model is loaded
     */
    fun isModelLoaded(): Boolean = isDepressionModelLoaded

    /**
     * Get model information for debugging
     */
    fun getModelInfo(): String {
        return buildString {
            append("Model Status:\n")
            append("- Loaded: $isDepressionModelLoaded\n")
            append("- Source: $depressionModelSource\n")
            if (isDepressionModelLoaded && depressionInterpreter != null) {
                try {
                    val inputTensor = depressionInterpreter?.getInputTensor(0)
                    val outputTensor = depressionInterpreter?.getOutputTensor(0)
                    append("- Input shape: ${inputTensor?.shape()?.contentToString()}\n")
                    append("- Output shape: ${outputTensor?.shape()?.contentToString()}\n")
                    append("- Sequence length: $DEPRESSION_SEQUENCE_LENGTH\n")
                    append("- Feature count: $DEPRESSION_FEATURE_COUNT\n")
                } catch (e: Exception) {
                    append("- Error getting tensor info: ${e.message}\n")
                }
            }
        }
    }

    /**
     * Get current model version
     */
    fun getCurrentModelVersion(): String {
        return try {
            val internalDir = context.filesDir
            val downloadedModel = File(internalDir, "depression_model_downloaded.tflite")

            if (downloadedModel.exists()) {
                // Check if we have version info stored
                val versionFile = File(internalDir, "depression_model_version.txt")
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
            Log.e(TAG, "Error getting model version: ${e.message}")
            "unknown"
        }
    }

    fun getModelStatus(): Map<String, Boolean> {
        return mapOf("depression_bilstm_transformer" to isDepressionModelLoaded)
    }

    fun close() {
        try {
            depressionInterpreter?.close()
            depressionInterpreter = null
            isDepressionModelLoaded = false
            Log.d(TAG, "TFLite depression model closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TFLite model: ${e.message}")
        }
    }
}