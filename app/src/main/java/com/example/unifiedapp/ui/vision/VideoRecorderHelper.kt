package com.example.unifiedapp.ui.vision

import android.Manifest
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorderHelper(
    private val context: Context,
    private val listener: VideoRecordingListener
) {
    private var recordingStartTime: Long = 0L
    init {
        Log.d("VideoRecorderHelper", "Instance created: $this")

    }
    companion object {
        private const val TAG = "VideoRecorderHelper"
    }

    var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    interface VideoRecordingListener {
        fun onRecordingStarted(filePath: String)
        fun onRecordingStopped(filePath: String, durationMs: Long)
        fun onRecordingError(error: String)
    }

    fun buildVideoCapture(): VideoCapture<Recorder> {
        Log.d(TAG, "Building VideoCapture")
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
        Log.d(TAG, "VideoCapture built successfully")
        return videoCapture!!
    }

    fun bind(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner
    ) {
        Log.d(TAG, "Binding VideoCapture to lifecycle")
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            videoCapture
        )
        Log.d(TAG, "VideoCapture bound successfully")
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(userName: String) {
        Log.d(TAG, "startRecording called for user: $userName")

        val capture = videoCapture ?: run {
            Log.e(TAG, "VideoCapture not initialized!")

            listener.onRecordingError("VideoCapture not initialized")
            return
        }

        if (activeRecording != null) {
            Log.e(TAG, "Recording already running!")
            listener.onRecordingError("Recording already running")
            return
        }

        val outputFile = createOutputFile(userName)
        Log.d(TAG, "Output file created: ${outputFile.absolutePath}")

        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        val executor = ContextCompat.getMainExecutor(context)

        try {
            activeRecording = capture.output
                .prepareRecording(context, outputOptions)
                .withAudioEnabled()
                .start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "✅ Recording started: ${outputFile.absolutePath}")
                            listener.onRecordingStarted(outputFile.absolutePath)
                        }

                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                Log.e(TAG, "❌ Recording error: ${event.error}")
                                listener.onRecordingError(event.error.toString())
                            } else {
                                val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000
                                Log.d(TAG, "✅ Recording finalized: ${outputFile.absolutePath}, duration: ${durationMs}ms")
                                listener.onRecordingStopped(outputFile.absolutePath, durationMs)
                            }
                            activeRecording = null
                        }
                    }
                }
            Log.d(TAG, "Recording started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Start recording failed", e)
            listener.onRecordingError("Start failed: ${e.message}")
        }
    }

    fun stopRecording() {
        Log.d(TAG, "stopRecording called")
        activeRecording?.stop() ?: Log.w(TAG, "No active recording to stop")

    }

    fun release() {
        Log.d(TAG, "release called")
        activeRecording?.stop()
        activeRecording = null
        videoCapture = null
    }

    private fun createOutputFile(userName: String): File {
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val userDir = File(downloads, userName)

        if (!userDir.exists()) {
            userDir.mkdirs()
            Log.d(TAG, "Created directory: ${userDir.absolutePath}")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(userDir, "${userName}_$timestamp.mp4")
    }
}