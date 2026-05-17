package com.example.unifiedapp.ui.Controller

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Initializing : CameraUiState()
    object Ready : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

class CameraController(
    private val context: Context
) {
    companion object {
        private const val TAG = "CameraController"
    }

    private var cameraProvider: ProcessCameraProvider? = null

    fun shutdown() {
        Log.d(TAG, "Shutting down camera controller")
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    suspend fun initialize(): ProcessCameraProvider {
        Log.d(TAG, "Initializing camera provider")
        return suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cameraProvider = future.get()
                    Log.d(TAG, "Camera provider initialized successfully")
                    continuation.resume(cameraProvider!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Camera provider initialization failed", e)
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider
        if (provider == null) {
            Log.e(TAG, "Cannot bind preview - cameraProvider is null")
            return
        }

        Log.d(TAG, "Binding preview to lifecycle")

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
            Log.d(TAG, "Preview bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding preview", e)
        }
    }

    fun unbind() {
        Log.d(TAG, "Unbinding all camera use cases")
        cameraProvider?.unbindAll()
    }
}