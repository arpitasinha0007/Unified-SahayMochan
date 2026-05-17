package com.example.unifiedapp.ui.views

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Initializing : CameraUiState()
    object Ready : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _faceDetected = MutableStateFlow(false)
    val faceDetected: StateFlow<Boolean> = _faceDetected.asStateFlow()

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    private val _au17Value = MutableStateFlow(0f)
    val au17Value: StateFlow<Float> = _au17Value.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var context: Context? = null
    private var lastVideoPath: String? = null
    private var anonymousId: String = ""

    fun init(appContext: Context) {
        context = appContext
        Log.d("CameraViewModel", "Initialized with context")
    }

    fun initialize(context: Context, anonymousId: String) {
        this.context = context
        this.anonymousId = anonymousId
        _isInitialized.value = true
        _uiState.value = CameraUiState.Ready
        Log.d("CameraViewModel", "Camera initialized with anonymousId: $anonymousId")
    }

    fun startRecording() {
        _isRecording.value = true
        _frameCount.value = 0
        Log.d("CameraViewModel", "Recording started")
    }

    fun stopRecording() {
        _isRecording.value = false
        _faceDetected.value = false
        Log.d("CameraViewModel", "Recording stopped")
    }

    fun processFrame(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (!_isRecording.value) {
            imageProxy.close()
            return
        }

        try {
            _faceDetected.value = true
            _frameCount.value = _frameCount.value + 1
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error processing frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    fun validateFacialData(): Boolean {
        val isValid = _frameCount.value >= 50
        Log.d("CameraViewModel", "Facial data validation: frames=${_frameCount.value}, isValid=$isValid")
        return isValid
    }

    fun getLastVideoPath(): String? = lastVideoPath

    fun getVideoCapture(): Any? = null

    fun shutdown() {
        Log.d("CameraViewModel", "Shutdown")
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }
}