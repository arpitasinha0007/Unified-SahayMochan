package com.example.unifiedapp.ui.vision


import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
//It is a utility class that was designed to check and diagnose problems of integrating TensorFlow Lite models.
//  It offers functionality to check the assets folder of an app to ensure model files are present and are named properly.
//  It will then try to interpret the TFLite models into an Interpreter to ensure that their structure
//  is correct and to read information of their input and output tensors and print out detailed logs to help developers debug them.

class ModelDebugHelper(private val context: Context) {

    companion object {
        private const val TAG = "ModelDebugHelper"
    }

    fun debugModelLoading() {
        Log.d(TAG, "Starting comprehensive model debugging...")
        checkAssetsFolder()
        checkModelFiles()
        testIndividualModelLoading()
    }

    private fun checkAssetsFolder() {
        try {
            Log.d(TAG, "\nASSETS FOLDER ANALYSIS")
            val assetManager = context.assets
            val rootFiles = assetManager.list("") ?: emptyArray()
            Log.d(TAG, "Root assets folder contains ${rootFiles.size} items:")
            rootFiles.forEachIndexed { index, file ->
                Log.d(TAG, "   ${index + 1}. $file")
            }

            val tfliteFiles = rootFiles.filter { it.endsWith(".tflite") }
            Log.d(TAG, "TFLite models found: ${tfliteFiles.size}")
            tfliteFiles.forEach { file ->
                Log.d(TAG, "   $file")
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
        Log.d(TAG, "\nMODEL FILE VERIFICATION")
        val modelsToCheck = listOf("new_hybrid_anxiety_model.tflite")

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
        Log.d(TAG, "\nINDIVIDUAL MODEL LOADING TEST")
        testSingleModel(
            "new_hybrid_anxiety_model.tflite",
            "Anxiety",
            expectedInputFeatures = 8
        )
    }

    private fun testSingleModel(fileName: String, modelName: String, expectedInputFeatures: Int) {
        Log.d(TAG, "\nTesting $modelName Model ($fileName)")
        try {
            val modelBuffer = loadModelFileForTest(fileName)
            val interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "   $modelName interpreter created successfully")

            // Note: The hybrid model has two inputs, this is a simplified check.
            val inputShape1 = interpreter.getInputTensor(0).shape()
            val inputShape2 = interpreter.getInputTensor(1).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()

            Log.d(TAG, "   Actual input 1 (sequential) shape: ${inputShape1.contentToString()}")
            Log.d(TAG, "   Actual input 2 (statistical) shape: ${inputShape2.contentToString()}")
            Log.d(TAG, "   Actual output shape: ${outputShape.contentToString()}")

            if (inputShape1.size == 3 && inputShape1[2] == expectedInputFeatures) {
                Log.d(TAG, "   Sequential input shape matches expected features.")
            } else {
                Log.e(TAG, "   Sequential input shape mismatch!")
            }

            interpreter.close()
        } catch (e: Exception) {
            Log.e(TAG, "   $modelName model test failed: ${e.message}")
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

    fun validateRequiredFiles(): Map<String, Boolean> {
        val requiredFiles = listOf("new_hybrid_anxiety_model.tflite")
        val results = mutableMapOf<String, Boolean>()
        Log.d(TAG, "\nREQUIRED FILES VALIDATION")

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
        Log.d(TAG, "\nOverall status: ${if (allPresent) "All files present" else "Missing files"}")

        if (!allPresent) {
            Log.d(TAG, "\nTo fix missing files:")
            Log.d(TAG, "   1. Copy your .tflite file to app/src/main/assets/")
            Log.d(TAG, "   2. Ensure exact file name matches.")
            Log.d(TAG, "   3. Clean and rebuild your project.")
        }
        return results
    }
}