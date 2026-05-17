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

    // UPDATED: Added registrationId parameter
    // In UploadHelper.uploadAssessment

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

        // Get file paths from SharedPreferences
        val filePrefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
        val videoPath = filePrefs.getString("video_path", null)
        val auCsvPath = filePrefs.getString("au_csv_path", null)  // This is CRITICAL
        val phq9CsvPath = filePrefs.getString("phq9_csv_path", null)  // This is CRITICAL

        Log.d(TAG, "Video path: $videoPath")
        Log.d(TAG, "AU CSV path: $auCsvPath")
        Log.d(TAG, "PHQ9 CSV path: $phq9CsvPath")

        // Validate ALL required files
        val videoFile = videoPath?.let { File(it) }
        val auCsvFile = auCsvPath?.let { File(it) }
        val phq9CsvFile = phq9CsvPath?.let { File(it) }

        // Check video file
        if (videoFile == null || !videoFile.exists()) {
            onError("Video file not found. Please retake the assessment.")
            return
        }

        // CHECK: AU CSV file is REQUIRED by server
        if (auCsvFile == null || !auCsvFile.exists()) {
            onError("Action Units data file not found. Please retake the assessment.")
            return
        }

        // CHECK: PHQ-9 CSV file is REQUIRED by server for depression
        if (phq9CsvFile == null || !phq9CsvFile.exists()) {
            onError("PHQ-9 data file not found. Please retake the assessment.")
            return
        }

        // CHECK: Registration ID is required
        val finalRegistrationId = registrationId ?: run {
            val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            userPrefs.getString("registration_id", null)
        }

        if (finalRegistrationId.isNullOrBlank()) {
            onError("Registration ID is missing. Please login again.")
            return
        }

        Log.d(TAG, "All required files present:")
        Log.d(TAG, "  - Video: ${videoFile.length()} bytes")
        Log.d(TAG, "  - AU CSV: ${auCsvFile.length()} bytes")
        Log.d(TAG, "  - PHQ-9 CSV: ${phq9CsvFile.length()} bytes")
        Log.d(TAG, "  - Registration ID: $finalRegistrationId")

        coroutineScope.launch {
            try {
                val serverClient = SimpleServerClient(context)

                val assessmentData = SimpleServerClient.AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = "depression",  // Must match server expectation
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,  // This is now required
                    phq9CsvFile = phq9CsvFile,  // This is now required
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
                                deleteLocalFiles(context, anonymousId)
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

    private fun deleteLocalFiles(context: Context, anonymousId: String) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val appFolder = File(downloadsDir, "MochanApp")
            val userFolder = File(appFolder, anonymousId)

            if (userFolder.exists() && userFolder.isDirectory) {
                userFolder.deleteRecursively()
                Log.d(TAG, "Deleted local files for $anonymousId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local files: ${e.message}")
        }
    }
}