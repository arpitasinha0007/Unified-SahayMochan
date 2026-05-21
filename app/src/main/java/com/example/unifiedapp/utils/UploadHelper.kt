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

    /**
     * Upload assessment data (video, AU CSV, and questionnaire CSV) to the server.
     *
     * @param context        Application context
     * @param coroutineScope Coroutine scope for background tasks
     * @param anonymousId    User's anonymous ID
     * @param age            User's age
     * @param aiRawScore     AI raw score (optional)
     * @param email          User's email (optional)
     * @param registrationId User's registration ID (must be non‑blank; fallback will be used if null)
     * @param onProgress     Progress callback (progress percent, message)
     * @param onSuccess      Success callback
     * @param onError        Error callback
     */
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
        Log.d(TAG, "PHQ‑9 CSV path: $phq9CsvPath")

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

        // ✅ Get assessment type and scores from preferences (set during assessment)
        val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
        val assessmentType = prefs.getString("assessment_type", "depression") ?: "depression"
        val gad7Score = prefs.getInt("gad7_score", 0)        // For anxiety (GAD‑7)
        val phqScore = prefs.getInt("phq_score", 0)          // For depression (PHQ‑9)

        Log.d(TAG, "All required files present:")
        Log.d(TAG, "  - Video: ${videoFile.length()} bytes")
        Log.d(TAG, "  - AU CSV: ${auCsvFile?.length() ?: 0} bytes")
        Log.d(TAG, "  - PHQ‑9 CSV: ${phq9CsvFile?.length() ?: 0} bytes")
        Log.d(TAG, "  - Registration ID: $finalRegistrationId")
        Log.d(TAG, "  - Assessment type: $assessmentType")
        Log.d(TAG, "  - GAD‑7 score: $gad7Score")
        Log.d(TAG, "  - PHQ‑9 score: $phqScore")

        coroutineScope.launch {
            try {
                val serverClient = SimpleServerClient(context)

                // Data class expected by SimpleServerClient.uploadAnonymousAssessment
                val assessmentData = SimpleServerClient.AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = assessmentType,
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,
                    phq9CsvFile = phq9CsvFile,      // This will be mapped to "phq_csv" in the request
                    aiRawScore = aiRawScore,
                    email = email,
                    registrationId = finalRegistrationId,
                    gad7Score = gad7Score,           // Added for server
                    phqScore = phqScore              // Added for server
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
                onError("Upload failed: ${e.message}")
            }
        }
    }
}