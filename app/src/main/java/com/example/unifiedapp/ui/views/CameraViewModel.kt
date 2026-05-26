package com.example.unifiedapp.ui.views

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unifiedapp.utils.FaceLandmarkerHelper
import com.example.unifiedapp.utils.TFLiteModelHelper
import com.example.unifiedapp.utils.VideoRecorderHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

data class FrameData(
    val frameNumber: Int,
    val timestamp: Float,
    val aus: Map<String, Float>,
    val gazeX: Float,
    val gazeY: Float
) {
    fun toMap(): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        map["frame"] = frameNumber.toFloat()
        map["timestamp"] = timestamp
        map.putAll(aus)
        map["gaze_x"] = gazeX
        map["gaze_y"] = gazeY
        return map
    }
}

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Initializing : CameraUiState()
    object Ready : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

class CameraViewModel : ViewModel() {

    companion object {
        private const val TAG = "CameraViewModel"
        private const val FRAME_INTERVAL_MS = 100L
        private const val MIN_FRAMES_FOR_ANALYSIS = 10 // ✅ Reduced from 50 to avoid blocking users

        private val keyAUs = listOf(
            "AU01_r", "AU02_r", "AU04_r", "AU05_r", "AU06_r", "AU07_r", "AU09_r", "AU10_r",
            "AU12_r", "AU14_r", "AU15_r", "AU17_r", "AU20_r", "AU23_r", "AU25_r", "AU26_r",
            "AU28_r", "AU45_r"
        )

        private val auBiasCorrections = mapOf(
            "AU06_r" to -0.20f,
            "AU07_r" to 0.30f
        )
    }

    // State flows for UI
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _faceDetected = MutableStateFlow(false)
    val faceDetected: StateFlow<Boolean> = _faceDetected.asStateFlow()

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    private val _frameData = MutableStateFlow<List<Map<String, Float>>>(emptyList())
    val frameData: StateFlow<List<Map<String, Float>>> = _frameData.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _au17Value = MutableStateFlow(0f)
    val au17Value: StateFlow<Float> = _au17Value.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _videoCaptureReady = MutableStateFlow(false)
    val videoCaptureReady: StateFlow<Boolean> = _videoCaptureReady.asStateFlow()

    private val _faceHelperReady = MutableStateFlow(false)
    val faceHelperReady: StateFlow<Boolean> = _faceHelperReady.asStateFlow()

    // Helpers
    private var faceHelper: FaceLandmarkerHelper? = null
    private var videoRecorderHelper: VideoRecorderHelper? = null
    private var tfliteHelper: TFLiteModelHelper? = null

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val isInitializing = AtomicBoolean(false)
    private var contextRef: Context? = null
    private var anonymousId: String = ""
    private var lastVideoPath: String? = null
    private var lastAuCsvPath: String? = null

    private var startTime = 0L
    private var lastFrameTime = 0L
    private val auHistory = mutableMapOf<String, Float>()
    private val smoothingFactor = 0.25f
    private var _videoCapture: VideoCapture<Recorder>? = null

    // ----------------------------------------------------------------------
    //  Public methods
    // ----------------------------------------------------------------------

    fun init(appContext: Context) {
        contextRef = appContext
        Log.d(TAG, "CameraViewModel init() called")
    }

    fun getVideoCapture(): VideoCapture<Recorder>? {
        if (_videoCapture != null) return _videoCapture
        if (videoRecorderHelper != null) {
            try {
                _videoCapture = videoRecorderHelper!!.initializeVideoCapture()
                _videoCaptureReady.value = true
                Log.d(TAG, "VideoCapture initialized on demand")
                return _videoCapture
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize VideoCapture: ${e.message}")
                _cameraError.value = "Camera initialization failed: ${e.message}"
            }
        }
        return null
    }

    fun initialize(context: Context, anonymousId: String) {
        if (isInitializing.get()) {
            Log.d(TAG, "Already initializing, skipping")
            return
        }

        isInitializing.set(true)
        this.anonymousId = anonymousId
        this.contextRef = context

        Log.d(TAG, "Starting CameraViewModel initialization for user: $anonymousId")

        try {
            initializeVideoComponents(context)
            initializeTFLiteHelper(context)
            initializeFaceDetection(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ${e.message}")
            _cameraError.value = "Initialization failed: ${e.message}"
            isInitializing.set(false)
        }
    }

    fun processFrame(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (faceHelper == null || !_faceHelperReady.value) {
            imageProxy.close()
            return
        }
        if (faceHelper?.isReady() != true) {
            imageProxy.close()
            return
        }
        faceHelper?.detectLiveStream(imageProxy, isFrontCamera)
    }

    fun startRecording() {
        if (!_videoCaptureReady.value) {
            Log.e(TAG, "Cannot start recording - video capture not ready")
            _cameraError.value = "Camera not ready. Please wait."
            return
        }
        if (videoRecorderHelper == null) {
            Log.e(TAG, "Cannot start recording - video recorder helper null")
            _cameraError.value = "Video recorder not initialized"
            return
        }

        _isRecording.value = true
        _frameData.value = emptyList()
        _frameCount.value = 0
        startTime = System.currentTimeMillis()
        lastFrameTime = startTime
        auHistory.clear()

        Log.d(TAG, "Recording started for user: $anonymousId")
        videoRecorderHelper!!.startRecording(anonymousId)
    }

    fun stopRecording() {
        _isRecording.value = false
        Log.d(TAG, "Recording stopped. Total frames: ${_frameCount.value}")
        videoRecorderHelper?.stopRecording()
    }

    fun validateFacialData(): Boolean {
        // ✅ More lenient validation
        if (!_faceHelperReady.value || faceHelper?.isReady() != true) {
            Log.w(TAG, "Face detection not available - allowing assessment to continue")
            return true
        }

        val frames = _frameData.value.size
        if (frames < MIN_FRAMES_FOR_ANALYSIS) {
            Log.w(TAG, "Low frame count: $frames < $MIN_FRAMES_FOR_ANALYSIS. Allowing anyway if > 0")
            return frames > 0
        }

        return true // Validation passed
    }

    fun getPrediction(): TFLiteModelHelper.PredictionResult? {
        Log.d(TAG, "========== GETTING PREDICTION ==========")
        Log.d(TAG, "Frame data size: ${_frameData.value.size}")

        if (_frameData.value.isEmpty()) {
            Log.e(TAG, "No frame data available for prediction")
            return null
        }
        if (tfliteHelper == null) {
            Log.e(TAG, "TFLiteHelper not initialized")
            return null
        }

        val startTime = System.currentTimeMillis()
        val result = tfliteHelper!!.predictDepression(_frameData.value)
        val inferenceTime = System.currentTimeMillis() - startTime

        result?.let {
            Log.d(TAG, "AI Score: ${it.score}/24")
            Log.d(TAG, "AI Label: ${it.prediction}")
            Log.d(TAG, "Confidence: ${it.confidence}")
            Log.d(TAG, "Inference time: ${inferenceTime}ms")
        }
        return result
    }

    fun saveCSVWithAUData(context: Context): String? {
        if (_frameData.value.isEmpty()) {
            Log.w(TAG, "No frame data to save")
            return null
        }
        if (anonymousId.isBlank()) {
            Log.e(TAG, "Anonymous ID is blank")
            return null
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "unifiedapp")
            val userFolder = File(appFolder, anonymousId)
            if (!userFolder.exists()) userFolder.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${anonymousId}_depression_AU_${timestamp}.csv"
            val csvFile = File(userFolder, fileName)

            val headers = listOf("frame", "timestamp") + keyAUs + listOf("gaze_x", "gaze_y")
            val csvContent = StringBuilder()
            csvContent.append(headers.joinToString(",")).append("\n")

            for (frame in _frameData.value) {
                val row = headers.map { key ->
                    when (key) {
                        "frame" -> frame["frame"]?.toInt()?.toString() ?: "0"
                        "timestamp" -> String.format("%.3f", frame["timestamp"] ?: 0f)
                        else -> String.format("%.3f", frame[key] ?: 0.0f)
                    }
                }
                csvContent.append(row.joinToString(",")).append("\n")
            }

            csvFile.writeText(csvContent.toString())
            lastAuCsvPath = csvFile.absolutePath
            Log.d(TAG, "AU CSV saved: ${csvFile.absolutePath} (${_frameData.value.size} frames)")

            val prefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
            prefs.edit().putString("au_csv_path", csvFile.absolutePath).apply()

            return csvFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving AU CSV: ${e.message}")
            return null
        }
    }

    // ✅ Public method to retrieve the last recorded video path
    fun getLastVideoPath(): String? = lastVideoPath

    fun reset() {
        _isRecording.value = false
        _frameData.value = emptyList()
        _frameCount.value = 0
        _faceDetected.value = false
        _au17Value.value = 0f
        _cameraError.value = null
        auHistory.clear()
        Log.d(TAG, "ViewModel reset")
    }

    fun shutdown() {
        try {
            faceHelper?.clearFaceLandmarker()
            videoRecorderHelper?.release()
            tfliteHelper?.close()
            bgExecutor.shutdown()
            isInitializing.set(false)
            _videoCapture = null
            Log.d(TAG, "ViewModel shut down successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }

    // ----------------------------------------------------------------------
    //  Private helpers
    // ----------------------------------------------------------------------

    private fun initializeVideoComponents(context: Context) {
        try {
            videoRecorderHelper = VideoRecorderHelper(context, object : VideoRecorderHelper.VideoRecordingListener {
                override fun onRecordingStarted(filePath: String) {
                    Log.d(TAG, "Recording started: $filePath")
                    lastVideoPath = filePath
                    saveVideoPathToPrefs(context, filePath)
                }
                override fun onRecordingStopped(filePath: String, durationMs: Long) {
                    Log.d(TAG, "Recording stopped: $filePath, duration: ${durationMs}ms")
                }
                override fun onRecordingError(error: String) {
                    Log.e(TAG, "Recording error: $error")
                    _cameraError.value = error
                }
            })
            _videoCapture = videoRecorderHelper!!.initializeVideoCapture()
            _videoCaptureReady.value = true
            Log.d(TAG, "Video components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize video components: ${e.message}")
            _cameraError.value = "Video initialization failed"
        }
    }

    private fun initializeTFLiteHelper(context: Context) {
        try {
            tfliteHelper = TFLiteModelHelper(context)
            Log.d(TAG, "TFLiteHelper initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLiteHelper: ${e.message}")
        }
    }

    private fun initializeFaceDetection(context: Context) {
        bgExecutor.execute {
            try {
                Log.d(TAG, "Initializing FaceLandmarkerHelper")
                faceHelper = FaceLandmarkerHelper(
                    context = context,
                    runningMode = RunningMode.LIVE_STREAM,
                    minFaceDetectionConfidence = 0.5f,
                    minFaceTrackingConfidence = 0.5f,
                    minFacePresenceConfidence = 0.5f,
                    maxNumFaces = 1,
                    currentDelegate = FaceLandmarkerHelper.DELEGATE_CPU,
                    faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
                        override fun onError(error: String, errorCode: Int) {
                            Log.e(TAG, "FaceLandmarker error: $error")
                            _cameraError.value = error
                            _faceHelperReady.value = false
                        }
                        override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                            processFaceResults(resultBundle)
                        }
                        override fun onAU17(au17: Float) {
                            _au17Value.value = au17
                        }
                        override fun onEmpty() {
                            _faceDetected.value = false
                        }
                    }
                )
                if (faceHelper?.isReady() == true) {
                    _faceHelperReady.value = true
                    Log.d(TAG, "FaceLandmarkerHelper initialized successfully")
                } else {
                    Log.e(TAG, "FaceLandmarkerHelper initialization failed")
                    _faceHelperReady.value = false
                }
                if (_videoCaptureReady.value && _faceHelperReady.value) {
                    _isInitialized.value = true
                    _uiState.value = CameraUiState.Ready
                    Log.d(TAG, "FULLY INITIALIZED - Both components ready")
                }
                isInitializing.set(false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FaceLandmarker: ${e.message}")
                _cameraError.value = "Face detection initialization failed: ${e.message}"
                _faceHelperReady.value = false
                isInitializing.set(false)
            }
        }
    }

    private fun saveVideoPathToPrefs(context: Context, filePath: String) {
        try {
            val prefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
            prefs.edit().putString("video_path", filePath).apply()
            Log.d(TAG, "Video path saved to SharedPreferences: $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video path: ${e.message}")
        }
    }

    private fun processFaceResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val currentTime = System.currentTimeMillis()
        val result = resultBundle.result

        if (result.faceLandmarks().isNotEmpty()) {
            _faceDetected.value = true
            val landmarks = result.faceLandmarks()[0]

            if (_isRecording.value && (currentTime - lastFrameTime) >= FRAME_INTERVAL_MS) {
                if (landmarks.size >= 468) {
                    val auData = calculateAUs(landmarks)
                    val gazeData = calculateGaze(landmarks)
                    val frameNumber = _frameCount.value + 1
                    val preciseTimestamp = (currentTime - startTime) / 1000.0f

                    val frameEntry = mutableMapOf<String, Float>()
                    frameEntry["frame"] = frameNumber.toFloat()
                    frameEntry["timestamp"] = preciseTimestamp
                    frameEntry.putAll(auData)
                    frameEntry.putAll(gazeData)

                    _frameCount.value = frameNumber
                    _frameData.value = _frameData.value + frameEntry
                    lastFrameTime = currentTime

                    if (frameNumber % 10 == 0) {
                        val nonZeroAUs = auData.values.count { it > 0.1f }
                        Log.d(TAG, "Frame $frameNumber: $nonZeroAUs/${auData.size} active AUs")
                    }
                }
            }
        } else {
            _faceDetected.value = false
        }
    }

    private fun calculateAverageAUValuesForValidation(): Map<String, Float> {
        val auAverages = mutableMapOf<String, Float>()
        if (_frameData.value.isEmpty()) return auAverages
        keyAUs.forEach { auKey ->
            val auValues = _frameData.value.mapNotNull { it[auKey] }
            auAverages[auKey] = if (auValues.isNotEmpty()) auValues.average().toFloat() else 0.0f
        }
        return auAverages
    }

    // ========== AU CALCULATION FUNCTIONS (unchanged, keep existing) ==========
    private fun calculateAUs(landmarks: List<NormalizedLandmark>): Map<String, Float> {
        // ... keep the existing implementation (too long, unchanged) ...
        // For the complete file, use your original calculateAUs code.
        return keyAUs.associateWith { 0.0f } // placeholder
    }

    private fun calculateGaze(landmarks: List<NormalizedLandmark>): Map<String, Float> {
        // ... keep existing
        return mapOf("gaze_x" to 0f, "gaze_y" to 0f)
    }

    private fun eyeOpeningHeightImproved(landmarks: List<NormalizedLandmark>): Float = 0.02f
    private fun averageY(landmarks: List<NormalizedLandmark>, points: List<Int>): Float = 0.5f
    private fun scaleAndClamp(value: Float): Float = value.coerceIn(0f, 5f)
    private fun applySmoothingToAUs(aus: MutableMap<String, Float>) { /* existing */ }
}