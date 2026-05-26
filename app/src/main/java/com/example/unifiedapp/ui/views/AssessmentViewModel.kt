package com.example.unifiedapp.ui.views

import java.security.MessageDigest
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import java.io.File
import java.util.*
import android.app.Application
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import com.example.unifiedapp.ui.vision.FaceLandmarkerHelper
import com.example.unifiedapp.ui.vision.VideoRecorderHelper
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.example.unifiedapp.ui.anxiety.AnxietyVideoProcessor
import com.example.unifiedapp.ui.anxiety.HeatmapResult
import com.example.unifiedapp.ui.anxiety.ProcessingConfig
import com.example.unifiedapp.ui.anxiety.VideoProcessor
import com.example.unifiedapp.ui.home.QuestionnaireQuestion
import com.example.unifiedapp.ui.vision.AUCsvWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AssessmentData(
    val anonymousId: String,
    val age: Int = 0,
    val email: String = "",
    val registrationId: String = "",
    val assessmentType: String = "anxiety",
    val videoFile: File? = null,
    val auCsvFile: File? = null,
    val gad7CsvFile: File? = null,
    val phq9CsvFile: File? = null,
    val gad7Score: Int = 0,
    val phqScore: Int = 0,
    val aiRawScore: Float? = null,
    val questionnaireScore: Int = 0
)

//data class AssessmentState(
//    val isRecording: Boolean = false,
//    val sessionId: String = "",
//    val frameCount: Int = 0,
//    val anxietyScore: Int = 0,
//    val anxietyConfidence: Float = 0f,
//    val anxietyPrediction: String = ""
//)

fun generateAnonymousId(name: String, registrationId: String): String {
    return try {
        val normalizedName = name.trim().lowercase(Locale.getDefault())
        val normalizedRegId = registrationId.trim().lowercase(Locale.getDefault())
        val input = "${normalizedName}_${normalizedRegId}"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray(Charsets.UTF_8))
        val hexString = hash.joinToString("") { "%02x".format(it) }
        "STU_${hexString.take(8)}"
    } catch (e: Exception) {
        Log.e("AnonymousId", "Failed to generate hash-based ID", e)
        "STU_${System.currentTimeMillis().toString().takeLast(8)}"
    }
}

class RegressionModel(context: Context) {

    private var interpreter: Interpreter? = null
    private var isLoading = true
    private val inputShape: IntArray

    init {
        Log.d("MODEL", "Starting background model load...")
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                val model = loadModelFile(context, "new_hybrid_anxiety_model.tflite")
                interpreter = Interpreter(model)
                isLoading = false
                val loadTime = System.currentTimeMillis() - startTime
                Log.d("MODEL", "✅ Model loaded in background in ${loadTime}ms")
            } catch (e: Exception) {
                Log.e("MODEL", "❌ Failed to load model", e)
                isLoading = false
            }
        }.start()

        inputShape = try {
            val tempModel = loadModelFile(context, "new_hybrid_anxiety_model.tflite")
            val tempInterpreter = Interpreter(tempModel)
            val shape = tempInterpreter.getInputTensor(0).shape()
            tempInterpreter.close()
            shape
        } catch (e: Exception) {
            intArrayOf(1, 1200, 18)
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    fun predict(input: Array<Array<FloatArray>>): Float {
        var waitTime = 0
        while (isLoading && waitTime < 5000) {
            Thread.sleep(50)
            waitTime += 50
        }

        val interp = interpreter ?: throw IllegalStateException("Model not loaded")

        if (input.size != inputShape[0]) {
            throw IllegalArgumentException("Batch size must be ${inputShape[0]}")
        }

        if (input[0].size != inputShape[1]) {
            throw IllegalArgumentException("Expected ${inputShape[1]} frames (timesteps)")
        }

        if (input[0][0].size != inputShape[2]) {
            throw IllegalArgumentException("Expected ${inputShape[2]} features per frame")
        }

        val output = Array(1) { FloatArray(1) }
        Log.d("MODEL", "Input shape passed: [${input.size}, ${input[0].size}, ${input[0][0].size}]")

        interp.run(input, output)

        return output[0][0]
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

class AssessmentViewModel(
    application: Application
) : AndroidViewModel(application),
    FaceLandmarkerHelper.LandmarkerListener, VideoRecorderHelper.VideoRecordingListener {

    companion object {
        private const val TAG = "AssessmentVM"
        private const val MAX_SEQ_LEN = 1200
    }

    private val regressionModel: RegressionModel by lazy {
        Log.d(TAG, "🔄 Lazy init: Creating RegressionModel")
        RegressionModel(getApplication())
    }

    private val processor: AnxietyVideoProcessor by lazy {
        Log.d(TAG, "🔄 Lazy init: Creating AnxietyVideoProcessor")
        AnxietyVideoProcessor(
            getApplication(),
            ProcessingConfig(
                heatmapSize = 800,
                samplingRate = 3,
                gaussianSigma = 3.5f
            )
        )
    }

    private val cleanupMutex = Mutex()
    private val isCleaningUp = AtomicBoolean(false)
    private val isRecordingActive = AtomicBoolean(false)
    private var processingJob: Job? = null

    private val _isFrameProcessingEnabled = MutableStateFlow(true)
    val isFrameProcessingEnabled: StateFlow<Boolean> = _isFrameProcessingEnabled.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _processingResult = MutableStateFlow<HeatmapResult?>(null)
    val processingResult: StateFlow<HeatmapResult?> = _processingResult.asStateFlow()

    fun processVideo(videoFile: File?) {
        processingJob?.cancel()

        processingJob = viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0
            _statusMessage.value = "Starting..."

            try {
                if (videoFile == null || !videoFile.exists()) {
                    _statusMessage.value = "Error: Video file not found"
                    _isProcessing.value = false
                    return@launch
                }

                Log.d(TAG, "Processing video: ${videoFile.absolutePath}")
                Log.d(TAG, "File size: ${videoFile.length()} bytes")

                withContext(Dispatchers.IO + NonCancellable) {
                    val result = processor.processVideoAndClassify(
                        videoFile = videoFile,
                        onProgress = { progress, message ->
                            _progress.value = progress
                            _statusMessage.value = message
                        }
                    )

                    result.onSuccess { heatmapResult ->
                        _processingResult.value = heatmapResult
                        _statusMessage.value = "Complete! Level: ${heatmapResult.anxietyLevel}"
                        Log.d(TAG, "✅ Processing successful")
                    }.onFailure { error ->
                        _statusMessage.value = "Error: ${error.message}"
                        Log.e(TAG, "❌ Processing failed", error)
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Processing was cancelled")
                _statusMessage.value = "Processing cancelled"
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _statusMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearResult() {
        _processingResult.value = null
        _progress.value = 0
        _statusMessage.value = ""
    }

    private var isWaitingForVideo = false
    private var gad7QuizScore: Int = 0
    private var modelAnxietyScore: Int = 0
    private var gad7Responses: Map<Int, QuestionnaireResponse> = emptyMap()
    private val _assessmentResult = MutableStateFlow<AssessmentData?>(null)
    val assessmentResult: StateFlow<AssessmentData?> = _assessmentResult

    private var videoFile: File? = null
    var auCsvWriter: AUCsvWriter? = null
    private var finalAssessmentData: AssessmentData? = null
    private var age: Int = 0

    private val userPreferences = UserPreferences(application)

    val userData: StateFlow<UserData> = userPreferences.userData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserData(
            isLoggedIn = false,
            name = "",
            email = "",
            gender = "",
            age = 0,
            id = "",
            TOKEN = ""
        )
    )

    private var gad7CsvFile: File? = null
    private val gad7Questions: List<QuestionnaireQuestion> = createGAD7Questions()
    private var auCsvFile: File? = null
    private var assessmentType: String = "anxiety"
    private val keyAUs = listOf(
        "AU01_r","AU02_r","AU04_r","AU05_r","AU06_r","AU07_r",
        "AU09_r","AU10_r","AU12_r","AU14_r","AU15_r","AU17_r",
        "AU20_r","AU23_r","AU25_r","AU26_r","AU28_r","AU45_r","gaze_x","gaze_y"
    )

    private val REQUIRED_TIMESTEPS = 1200
    private val FEATURE_SIZE = 18
    private val featureBuffer = mutableListOf<FloatArray>()

    private fun buildInput(frames: List<FloatArray>): Array<Array<FloatArray>> {
        val timesteps = 1200
        val features = 18

        val input = Array(1) { Array(timesteps) { FloatArray(features) { 0f } } }
        val length = frames.size.coerceAtMost(timesteps)

        for (i in 0 until length) {
            if (frames[i].size == features) {
                input[0][i] = frames[i]
            } else {
                Log.w("MODEL", "Frame $i has ${frames[i].size} features, expected $features. Padding/truncating.")
                val temp = FloatArray(features)
                val copyLength = minOf(frames[i].size, features)
                System.arraycopy(frames[i], 0, temp, 0, copyLength)
                input[0][i] = temp
            }
        }

        if (frames.size > timesteps) {
            Log.w("MODEL", "Frames size (${frames.size}) exceeds timesteps ($timesteps). Truncating to $timesteps.")
        }

        Log.d("MODEL", "Frames collected: ${frames.size}, used for input: $length")

        return input
    }

    private var anonymousId = ""
    private val collectedFrames = mutableListOf<FloatArray>()
    private val _uiState = MutableStateFlow(AssessmentState())
    val uiState: StateFlow<AssessmentState> = _uiState

    private var faceHelper: FaceLandmarkerHelper? = null

    fun setFaceHelper(helper: FaceLandmarkerHelper) {
        faceHelper = helper
    }

    private var pendingQuizScore: Int? = null

    fun completeSession(score: Int, responses: Map<Int, QuestionnaireResponse>) {
        Log.d(TAG, "completeSession called with score: $score")
        Log.d(TAG, "Responses count: ${responses.size}")

        _isFrameProcessingEnabled.value = false

        gad7QuizScore = score
        gad7Responses = responses
        pendingQuizScore = score
        isWaitingForVideo = true

        stopSession()
        Log.d(TAG, "Waiting for video to complete...")
    }

    private var videoHelper: VideoRecorderHelper? = null

    fun attachVideoHelper(helper: VideoRecorderHelper) {
        videoHelper = helper
    }

    private var isModelClosed = false

    override fun onCleared() {
        Log.d(TAG, "========== ViewModel onCleared ==========")

        viewModelScope.launch {
            cleanupMutex.withLock {
                if (isCleaningUp.compareAndSet(false, true)) {
                    performCleanup()
                }
            }
        }

        super.onCleared()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startSession(userName: String) {
        Log.d(TAG, "========== startSession ==========")
        Log.d(TAG, "User: $userName")
        Log.d(TAG, "Anonymous ID: $anonymousId")
        _isFrameProcessingEnabled.value = true
        isRecordingActive.set(true)

        isCleaningUp.set(false)

        auCsvWriter = AUCsvWriter(
            context = getApplication(),
            anonymousId = anonymousId,
            keyAUs = keyAUs
        )

        auCsvFile = auCsvWriter?.getFile()

        Log.d(TAG, "AU CSV file: ${auCsvFile?.absolutePath}")

        if (videoHelper?.videoCapture == null) {
            Log.e(TAG, "Cannot start session - VideoCapture not initialized yet!")
            isRecordingActive.set(false)
            return
        }

        frameBuffer.clear()
        _uiState.value = AssessmentState(
            isRecording = true,
            sessionId = "SESSION_${System.currentTimeMillis()}"
        )

        videoHelper?.startRecording(userName) ?: run {
            Log.e(TAG, "VideoHelper not attached!")
            isRecordingActive.set(false)
        }

        Log.d(TAG, "========== startSession complete ==========")
    }

    fun stopSession() {
        _isFrameProcessingEnabled.value = false
        isRecordingActive.set(false)

        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                faceHelper?.clearFaceLandmarker()
            }
            delay(150)
            videoHelper?.stopRecording()
        }
    }

    private val frameBuffer = Collections.synchronizedList(mutableListOf<FloatArray>())

    fun processImageFrame(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (!_isFrameProcessingEnabled.value) {
            imageProxy.close()
            return
        }

        try {
            faceHelper?.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = isFrontCamera
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
            imageProxy.close()
        }
    }

    private suspend fun closeCSVWritersSafely() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 Closing CSV writers...")

            auCsvWriter?.let { writer ->
                try {
                    writer.flush()
                    writer.close()
                    Log.d(TAG, "✅ AU CSV closed, file size: ${auCsvFile?.length()} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing AU CSV", e)
                }
            }

            if (auCsvFile?.exists() == true) {
                Log.d(TAG, "✅ AU CSV verified: ${auCsvFile?.absolutePath}")
            } else {
                Log.e(TAG, "❌ AU CSV missing after close!")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in closeCSVWritersSafely", e)
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        if (!_uiState.value.isRecording || !isRecordingActive.get() || !_isFrameProcessingEnabled.value) {
            Log.v(TAG, "Not recording, skipping frame")
            return
        }

        if (isCleaningUp.get()) {
            Log.v(TAG, "Cleaning up, skipping frame")
            return
        }

        val faceLandmarks = resultBundle.result.faceLandmarks()

        if (faceLandmarks.isEmpty()) {
            Log.v(TAG, "No face detected")
            return
        }

        try {
            val landmarks = faceLandmarks[0]
            val auMap = calculateAUs(landmarks)

            val frameVector = FloatArray(keyAUs.size)
            keyAUs.forEachIndexed { index, key ->
                frameVector[index] = auMap[key] ?: 0f
            }

            frameBuffer.add(frameVector)

            auCsvWriter?.let { writer ->
                try {
                    val timestamp = System.currentTimeMillis()
                    writer.writeFrame(frameVector, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing AU frame", e)
                }
            }

            _uiState.value = _uiState.value.copy(
                frameCount = _uiState.value.frameCount + 1
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        Log.e(TAG, error)
    }

    override fun onEmpty() {}

    override fun onAU17(au17: Float) {}

    fun createAssessmentData(
        context: Context,
        age: Int,
        email: String,
        registrationId: String,
        assessmentType: String = "anxiety"
    ): AssessmentData {
        val anonymousId = this.anonymousId
        val auCsv = File(context.filesDir, "au_$anonymousId.csv")
        val gad7Csv = File(context.filesDir, "gad7_$anonymousId.csv")

        return AssessmentData(
            anonymousId = anonymousId,
            age = age,
            email = email,
            registrationId = registrationId,
            assessmentType = assessmentType,
            videoFile = null,
            auCsvFile = auCsv,
            gad7CsvFile = gad7Csv,
            gad7Score = 0,
            questionnaireScore = 0
        )
    }

    fun startAssessment(context: Context) {
        val user = userData.value

        finalAssessmentData = createAssessmentData(
            context = context,
            age = user.age,
            email = user.email,
            registrationId = user.id,
            assessmentType = assessmentType
        )

        auCsvWriter = AUCsvWriter(
            context = getApplication(),
            anonymousId = anonymousId,
            keyAUs = keyAUs
        )

        frameBuffer.clear()
        _uiState.value = AssessmentState(
            isRecording = true,
            sessionId = "SESSION_${System.currentTimeMillis()}"
        )
    }

    fun stopAssessment() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }

    private fun finishAssessment(quizScore: Int) {
        Log.d(TAG, "========== finishAssessment ==========")
        Log.d(TAG, "Quiz score: $quizScore")
        Log.d(TAG, "Frames collected: ${frameBuffer.size}")

        if (frameBuffer.isEmpty()) {
            Log.e(TAG, "❌ No frames collected!")
            return
        }

        val user = userData.value

        try {
            val input = buildInput(frameBuffer)

            val modelScore = try {
                if (!isModelClosed) {
                    regressionModel.predict(input)
                } else {
                    quizScore / 100f
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model prediction error", e)
                quizScore / 100f
            }

            val normalizedQuiz = quizScore / 100f
            val fused = 0.7f * modelScore + 0.3f * normalizedQuiz

            modelAnxietyScore = (fused * 100).toInt()

            _uiState.value = _uiState.value.copy(
                anxietyScore = modelAnxietyScore,
                anxietyConfidence = fused,
                anxietyPrediction = when {
                    fused < 0.3f -> "Low"
                    fused < 0.6f -> "Moderate"
                    else -> "High"
                }
            )

            gad7CsvFile = File(getApplication<Application>().filesDir, "gad7_$anonymousId.csv")
            saveQuestionnaireToCSV(gad7CsvFile!!, gad7Questions, gad7Responses)

            val assessmentData = AssessmentData(
                anonymousId = generateAnonymousId(user.name, user.id),
                age = user.age,
                email = user.email,
                registrationId = user.id,
                assessmentType = assessmentType,
                videoFile = videoFile,
                auCsvFile = auCsvFile,
                gad7CsvFile = gad7CsvFile,
                gad7Score = gad7QuizScore,
                questionnaireScore = modelAnxietyScore
            )

            _assessmentResult.value = assessmentData

            Log.d(TAG, "✅ Assessment completed")
            Log.d(TAG, "Video: ${videoFile?.absolutePath}, exists: ${videoFile?.exists()}")
            Log.d(TAG, "AU CSV: ${auCsvFile?.absolutePath}, exists: ${auCsvFile?.exists()}")
            Log.d(TAG, "GAD7 CSV: ${gad7CsvFile?.absolutePath}, exists: ${gad7CsvFile?.exists()}")

            frameBuffer.clear()
            isWaitingForVideo = false

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finishing assessment", e)
        }
    }

    fun emergencyStop() {
        viewModelScope.launch {
            Log.w(TAG, "⚠️ Emergency stop triggered")

            _isFrameProcessingEnabled.value = false
            isRecordingActive.set(false)
            _uiState.value = _uiState.value.copy(isRecording = false)

            delay(100)
            videoHelper?.stopRecording()
            delay(200)
            closeCSVWritersSafely()

            isWaitingForVideo = false
            pendingQuizScore = null
        }
    }

    private suspend fun performCleanup() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 Starting cleanup...")
            Log.d(TAG, "isWaitingForVideo: $isWaitingForVideo")
            Log.d(TAG, "isRecordingActive: ${isRecordingActive.get()}")

            processingJob?.cancel()
            processingJob = null
            isRecordingActive.set(false)
            delay(200)

            try {
                auCsvWriter?.flush()
                auCsvWriter?.close()
                auCsvWriter = null
                Log.d(TAG, "✅ AU CSV writer closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing AU CSV writer", e)
            }

            if (!isWaitingForVideo && !isModelClosed) {
                try {
                    regressionModel.close()
                    isModelClosed = true
                    Log.d(TAG, "✅ Regression model closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing model", e)
                }
            }

            frameBuffer.clear()
            Log.d(TAG, "✅ Cleanup complete")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup", e)
        }
    }

    private fun buildSequence(frames: List<FloatArray>): Array<Array<FloatArray>>? {
        if (frames.isEmpty()) return null

        val numFeatures = keyAUs.size
        val input = Array(1) { Array(MAX_SEQ_LEN) { FloatArray(numFeatures) } }
        val usableFrames = frames.take(MAX_SEQ_LEN)

        for (i in usableFrames.indices) {
            input[0][i] = usableFrames[i]
        }

        return input
    }

    @SuppressLint("RestrictedApi")
    private fun calculateAUs(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Map<String, Float> {
        val aus = mutableMapOf<String, Float>()

        try {
            val faceWidth = abs(landmarks[454].x().toFloat() - landmarks[234].x().toFloat())
            val faceHeight = abs(landmarks[10].y().toFloat() - landmarks[152].y().toFloat())
            @Suppress("UNUSED_VARIABLE")
            val normFactor = sqrt(faceWidth * faceHeight).coerceAtLeast(0.01f)

            val innerBrowRaise = averageY(landmarks, listOf(21, 22, 23, 24, 25)) - 0.392f
            aus["AU01_r"] = (abs(innerBrowRaise) * 10f).coerceIn(0f, 5f)
            aus["AU01_c"] = if (innerBrowRaise > 0.01f) 1f else 0f

            // ... rest of the AU calculations (keep the same as your original)
            // (I've shortened this for brevity - keep your existing AU calculation code here)

            // Make sure to include all the AU calculations from your original file

        } catch (e: Exception) {
            Log.e(TAG, "AU calculation error: ${e.message}", e)
            return keyAUs.associateWith { 0f }
        }

        return aus
    }

    private fun averageY(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        points: List<Int>
    ): Float {
        var sum = 0f
        var count = 0
        for (p in points) {
            if (p < landmarks.size) {
                sum += landmarks[p].y().toFloat()
                count++
            }
        }
        return if (count == 0) 0.5f else sum / count
    }

    private fun averageX(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        points: List<Int>
    ): Float {
        var sum = 0f
        var count = 0
        for (p in points) {
            if (p < landmarks.size) {
                sum += landmarks[p].x().toFloat()
                count++
            }
        }
        return if (count == 0) 0.5f else sum / count
    }

    @SuppressLint("RestrictedApi")
    private fun calculateEyeAspectRatio(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Float {
        val leftVertical1 = abs(landmarks[159].y().toFloat() - landmarks[145].y().toFloat())
        val leftVertical2 = abs(landmarks[158].y().toFloat() - landmarks[153].y().toFloat())
        val leftHorizontal = abs(landmarks[133].x().toFloat() - landmarks[173].x().toFloat())
        val leftEAR = (leftVertical1 + leftVertical2) / (2.0f * leftHorizontal)

        val rightVertical1 = abs(landmarks[386].y().toFloat() - landmarks[374].y().toFloat())
        val rightVertical2 = abs(landmarks[385].y().toFloat() - landmarks[380].y().toFloat())
        val rightHorizontal = abs(landmarks[362].x().toFloat() - landmarks[398].x().toFloat())
        val rightEAR = (rightVertical1 + rightVertical2) / (2.0f * rightHorizontal)

        return (leftEAR + rightEAR) / 2.0f
    }

    override fun onRecordingStarted(filePath: String) {
        Log.d("VM", "Recording started: $filePath")
    }

    override fun onRecordingStopped(filePath: String, durationMs: Long) {
        viewModelScope.launch {
            _isFrameProcessingEnabled.value = false
            isRecordingActive.set(false)
            _uiState.value = _uiState.value.copy(isRecording = false)

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Video file doesn't exist: $filePath")
                return@launch
            }

            this@AssessmentViewModel.videoFile = file
            closeCSVWritersSafely()

            if (isWaitingForVideo && pendingQuizScore != null) {
                finishAssessment(pendingQuizScore!!)
                pendingQuizScore = null
                isWaitingForVideo = false
            }
        }
    }

    override fun onRecordingError(error: String) {
        Log.e("VM", "Recording error: $error")
    }

    private fun saveQuestionnaireToCSV(
        file: File,
        questions: List<QuestionnaireQuestion>,
        responses: Map<Int, QuestionnaireResponse>
    ) {
        val content = StringBuilder()
        content.append("question_id,selected_option,score,timestamp,attempted\n")
        questions.forEach { question ->
            val response = responses[question.id]
            if (response != null) {
                content.append("${question.id},${response.selectedOption},${response.score},${response.timestamp},1\n")
            } else {
                content.append("${question.id},-1,0,0,0\n")
            }
        }
        file.writeText(content.toString())
        Log.d(TAG, "Questionnaire CSV saved: ${file.absolutePath}")
    }

    fun stopFrameProcessing() {
        _isFrameProcessingEnabled.value = false
        Log.d(TAG, "Frame processing stopped")
    }
}

private fun createGAD7Questions(): List<QuestionnaireQuestion> {
    return listOf(
        QuestionnaireQuestion(
            id = 1,
            text = "Feeling nervous, anxious, or on edge",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 2,
            text = "Not being able to stop or control worrying",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 3,
            text = "Worrying too much about different things",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 4,
            text = "Trouble relaxing",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 5,
            text = "Being so restless that it's hard to sit still",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 6,
            text = "Becoming easily annoyed or irritable",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        ),
        QuestionnaireQuestion(
            id = 7,
            text = "Feeling afraid as if something awful might happen",
            options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
        )
    )
}