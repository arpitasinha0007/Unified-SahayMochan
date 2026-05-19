package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object UploadHelper {
    private const val TAG = "UploadHelper"

    fun uploadAssessment(
        context: Context,
        coroutineScope: CoroutineScope,
        anonymousId: String,
        age: Int,
        aiRawScore: Float?,
        email: String?,
        registrationId: String?,
        onProgress: (Int, String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "========== UPLOAD HELPER STARTED ==========")

        val filePrefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
        val videoPath = filePrefs.getString("video_path", null)
        val auCsvPath = filePrefs.getString("au_csv_path", null)
        val phq9CsvPath = filePrefs.getString("phq9_csv_path", null)

        Log.d(TAG, "Video path: $videoPath")
        Log.d(TAG, "AU CSV path: $auCsvPath")
        Log.d(TAG, "PHQ9 CSV path: $phq9CsvPath")

        val videoFile = videoPath?.let { File(it) }
        val auCsvFile = auCsvPath?.let { File(it) }
        val phq9CsvFile = phq9CsvPath?.let { File(it) }

        if (videoFile == null || !videoFile.exists()) {
            onError("Video file not found. Please retake the assessment.")
            return
        }

        // ✅ Ensure registration_id is never blank
        val finalRegistrationId = when {
            !registrationId.isNullOrBlank() -> registrationId
            else -> {
                val userPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val savedRegId = userPrefs.getString("registration_id", null)
                if (!savedRegId.isNullOrBlank()) savedRegId else anonymousId
            }
        }

        Log.d(TAG, "All required files present:")
        Log.d(TAG, "  - Video: ${videoFile.length()} bytes")
        Log.d(TAG, "  - Final Registration ID: $finalRegistrationId")

        coroutineScope.launch {
            try {
                val serverClient = SimpleServerClient(context)

                val assessmentData = SimpleServerClient.AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = "depression",
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,
                    phq9CsvFile = phq9CsvFile,
                    aiRawScore = aiRawScore,
                    email = email,
                    registrationId = finalRegistrationId
                )

                withContext(Dispatchers.IO) {
                    serverClient.uploadAnonymousAssessment(
                        assessmentData,
                        object : SimpleServerClient.UploadCallback {
                            override fun onProgress(progress: Int, message: String) {
                                onProgress(progress, message)
                            }

                            override fun onSuccess(message: String) {
                                onSuccess(message)
                            }

                            override fun onError(error: String) {
                                onError(error)
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during upload: ${e.message}", e)
                onError("Upload failed. Please check your connection.")
            }
        }
    }
}