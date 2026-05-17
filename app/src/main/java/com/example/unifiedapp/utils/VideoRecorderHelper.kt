package com.example.unifiedapp.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorderHelper(
    private val context: Context,
    private val listener: VideoRecordingListener
) {

    companion object {
        private const val TAG = "VideoRecorderHelper"
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    interface VideoRecordingListener {
        fun onRecordingStarted(filePath: String)
        fun onRecordingStopped(filePath: String, durationMs: Long)
        fun onRecordingError(error: String)
    }

    fun initializeVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.HD, Quality.SD, Quality.LOWEST),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
        return videoCapture!!
    }

    fun startRecording(userName: String) {
        val capture = videoCapture ?: run {
            listener.onRecordingError("VideoCapture not initialised")
            return
        }

        if (activeRecording != null) {
            listener.onRecordingError("Recording already running")
            return
        }

        val destFile = createOutputFile(userName)
        val outputOptions = FileOutputOptions.Builder(destFile).build()
        val executor = ContextCompat.getMainExecutor(context)

        try {
            // NO AUDIO - just prepare the recording without audio
            val recording = capture.output.prepareRecording(context, outputOptions)

            activeRecording = recording.start(executor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "Recording started → ${destFile.absolutePath}")
                        listener.onRecordingStarted(destFile.absolutePath)
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Log.e(TAG, "Recording error: ${event.error}")
                            listener.onRecordingError(event.error.toString())
                        } else {
                            val durMs = event.recordingStats.recordedDurationNanos / 1_000_000
                            Log.d(TAG, "Recording saved (${durMs} ms) → ${destFile.absolutePath}")
                            listener.onRecordingStopped(destFile.absolutePath, durMs)
                        }
                        activeRecording = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            listener.onRecordingError("Failed to start recording: ${e.message}")
        }
    }

    fun stopRecording() {
        try {
            activeRecording?.let { recording ->
                recording.stop()
                Log.d(TAG, "Recording stop requested")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            listener.onRecordingError("Error stopping recording: ${e.message}")
        }
    }

    fun release() {
        try {
            activeRecording?.stop()
            activeRecording = null
            videoCapture = null
            Log.d(TAG, "VideoRecorderHelper released")
        } catch (e: Exception) {
            Log.e(TAG, "Error during release: ${e.message}")
        }
    }

    private fun createOutputFile(userName: String): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val userDir = File(downloads, "MochanApp/$userName")

        if (!userDir.exists()) {
            userDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(userDir, "${userName}_video_$timestamp.mp4")
    }
}