package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelDebugHelper(private val context: Context) {

    companion object {
        private const val TAG = "ModelDebugHelper"
        private const val DEPRESSION_MODEL_FILE = "best_bilstm_transformer_model.tflite"
    }
    fun debugModelLoading() {
        Log.d(TAG, "Starting comprehensive model debugging...")

        checkAssetsFolder()
        checkModelFiles()
        testIndividualModelLoading()
    }

    private fun checkAssetsFolder() {
        try {
            Log.d(TAG, "\n=== ASSETS FOLDER ANALYSIS ===")

            val assetManager = context.assets
            val rootFiles = assetManager.list("") ?: emptyArray()

            Log.d(TAG, "Root assets folder contains ${rootFiles.size} items:")
            rootFiles.forEachIndexed { index, file ->
                Log.d(TAG, "   ${index + 1}. $file")
            }

            val tfliteFiles = rootFiles.filter { it.endsWith(".tflite") }
            Log.d(TAG, "TFLite models found: ${tfliteFiles.size}")
            tfliteFiles.forEach { file ->
                Log.d(TAG, "   ✓ $file")

                try {
                    val afd = assetManager.openFd(file)
                    val sizeKB = afd.length / 1024
                    Log.d(TAG, "     Size: ${sizeKB}KB")
                    afd.close()
                } catch (e: Exception) {
                    Log.e(TAG, "     Error getting size: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking assets folder: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkModelFiles() {
        Log.d(TAG, "\n=== MODEL FILE VERIFICATION ===")

        val modelsToCheck = listOf(DEPRESSION_MODEL_FILE)

        modelsToCheck.forEach { modelFile ->
            Log.d(TAG, "\nChecking: $modelFile")

            try {
                val assetManager = context.assets
                val assetFileDescriptor = assetManager.openFd(modelFile)

                Log.d(TAG, "   File exists and accessible")
                Log.d(TAG, "   Size: ${assetFileDescriptor.length} bytes (${assetFileDescriptor.length / 1024}KB)")
                Log.d(TAG, "   Start offset: ${assetFileDescriptor.startOffset}")

                assetFileDescriptor.close()

                try {
                    val modelBuffer = loadModelFileForTest(modelFile)
                    Log.d(TAG, "   Model buffer created successfully")

                    val testInterpreter = Interpreter(modelBuffer)

                    val inputTensors = testInterpreter.inputTensorCount
                    val outputTensors = testInterpreter.outputTensorCount

                    Log.d(TAG, "   Input tensors: $inputTensors")
                    Log.d(TAG, "   Output tensors: $outputTensors")

                    if (inputTensors > 0) {
                        val inputTensor = testInterpreter.getInputTensor(0)
                        Log.d(TAG, "   Input shape: ${inputTensor.shape().contentToString()}")
                        Log.d(TAG, "   Input type: ${inputTensor.dataType()}")
                    }

                    if (outputTensors > 0) {
                        val outputTensor = testInterpreter.getOutputTensor(0)
                        Log.d(TAG, "   Output shape: ${outputTensor.shape().contentToString()}")
                        Log.d(TAG, "   Output type: ${outputTensor.dataType()}")
                    }

                    testInterpreter.close()
                    Log.d(TAG, "   Model loaded and analyzed successfully!")

                } catch (interpreterError: Exception) {
                    Log.e(TAG, "   Interpreter creation failed: ${interpreterError.message}")
                    interpreterError.printStackTrace()
                }

            } catch (e: Exception) {
                Log.e(TAG, "   File not accessible: ${e.message}")
                Log.d(TAG, "   Possible solutions:")
                Log.d(TAG, "      1. Ensure $modelFile is in app/src/main/assets/")
                Log.d(TAG, "      2. Check file name spelling exactly")
                Log.d(TAG, "      3. Clean and rebuild project")
            }
        }
    }

    private fun testIndividualModelLoading() {
        Log.d(TAG, "\n=== INDIVIDUAL MODEL LOADING TEST ===")

        testSingleModel(
            DEPRESSION_MODEL_FILE,
            "Depression",
            expectedInputFeatures = 17
        )
    }

    private fun testSingleModel(fileName: String, modelName: String, expectedInputFeatures: Int) {
        Log.d(TAG, "\nTesting $modelName Model ($fileName)")

        try {
            val modelBuffer = loadModelFileForTest(fileName)
            val interpreter = Interpreter(modelBuffer)

            Log.d(TAG, "   $modelName interpreter created successfully")

            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()

            Log.d(TAG, "   Expected input features: $expectedInputFeatures")
            Log.d(TAG, "   Actual input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "   Output shape: ${outputShape.contentToString()}")

            if (inputShape.isNotEmpty() && inputShape.last() == expectedInputFeatures) {
                Log.d(TAG, "   Input shape matches expected features")
                testModelInference(interpreter, modelName, inputShape)
            } else {
                Log.e(TAG, "   Input shape mismatch!")
                Log.e(TAG, "      Expected features: $expectedInputFeatures")
                Log.e(TAG, "      Actual shape: ${inputShape.contentToString()}")
            }

            interpreter.close()

        } catch (e: Exception) {
            Log.e(TAG, "   $modelName model test failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun testModelInference(interpreter: Interpreter, modelName: String, inputShape: IntArray) {
        try {
            Log.d(TAG, "   Testing inference with dummy data...")

            val totalInputSize = inputShape.reduce { acc, i -> acc * i }
            val inputBuffer = java.nio.ByteBuffer.allocateDirect(4 * totalInputSize).apply {
                order(java.nio.ByteOrder.nativeOrder())
                repeat(totalInputSize) { putFloat(0.1f) }
            }

            val outputShape = interpreter.getOutputTensor(0).shape()
            val totalOutputSize = if (outputShape.isNotEmpty()) outputShape.reduce { acc, i -> acc * i } else 1
            val outputBuffer = java.nio.ByteBuffer.allocateDirect(4 * totalOutputSize).apply {
                order(java.nio.ByteOrder.nativeOrder())
            }

            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val outputValue = outputBuffer.float

            Log.d(TAG, "   Inference successful!")
            Log.d(TAG, "   Raw Output: $outputValue")

            val prediction = if (outputValue > 0.5f) "Depressed" else "Not Depressed"
            Log.d(TAG, "   Prediction: $prediction (score: %.3f)".format(outputValue))


        } catch (e: Exception) {
            Log.e(TAG, "   Inference test failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFileForTest(modelFileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun copyModelToInternalStorage(modelFileName: String): String? {
        return try {
            val inputStream = context.assets.open(modelFileName)
            val outputFile = File(context.filesDir, modelFileName)
            val outputStream = FileOutputStream(outputFile)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            Log.d(TAG, "Model copied to: ${outputFile.absolutePath}")
            outputFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model: ${e.message}")
            null
        }
    }

    fun validateRequiredFiles(): Map<String, Boolean> {
        val requiredFiles = listOf(DEPRESSION_MODEL_FILE)
        val results = mutableMapOf<String, Boolean>()

        Log.d(TAG, "\n=== REQUIRED FILES VALIDATION ===")

        requiredFiles.forEach { fileName ->
            val exists = try {
                context.assets.open(fileName).close()
                true
            } catch (e: Exception) {
                false
            }
            results[fileName] = exists
            Log.d(TAG, "   $fileName: ${if (exists) "Found" else "Missing"}")
        }

        val allPresent = results.values.all { it }
        Log.d(TAG, "\nOverall status: ${if (allPresent) "All files present" else "Missing file"}")

        if (!allPresent) {
            Log.d(TAG, "\nTo fix missing file:")
            Log.d(TAG, "   1. Copy your .tflite file to app/src/main/assets/")
            Log.d(TAG, "   2. Ensure exact file name is: $DEPRESSION_MODEL_FILE")
            Log.d(TAG, "   3. Clean and rebuild your project")
        }

        return results
    }
}