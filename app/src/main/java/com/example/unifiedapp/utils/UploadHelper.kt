package com.example.unifiedapp.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import com.example.unifiedapp.ui.remote.SimpleServerClient
import com.example.unifiedapp.ui.views.AssessmentData

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
        var videoPath = filePrefs.getString("video_path", null)
        var auCsvPath = filePrefs.getString("au_csv_path", null)
        var phq9CsvPath = filePrefs.getString("phq9_csv_path", null)

        Log.d(TAG, "Video path: $videoPath")
        Log.d(TAG, "AU CSV path: $auCsvPath")
        Log.d(TAG, "PHQ‑9 CSV path: $phq9CsvPath")

        var videoFile = videoPath?.let { File(it) }
        var auCsvFile = auCsvPath?.let { File(it) }
        var phq9CsvFile = phq9CsvPath?.let { File(it) }

        // ✅ Ensure questionnaire CSV exists (fallback)
        val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
        val assessmentType = prefs.getString("assessment_type", "depression") ?: "depression"
        val gad7Score = prefs.getInt("gad7_score", 0)
        val phqScore = prefs.getInt("phq_score", 0)

        if (phq9CsvFile == null || !phq9CsvFile.exists()) {
            val assessmentScore = if (assessmentType == "depression") phqScore else gad7Score
            val ensuredCsv = ensureCsvFileExists(context, anonymousId, assessmentType, assessmentScore)
            if (ensuredCsv != null) {
                phq9CsvFile = ensuredCsv
                phq9CsvPath = ensuredCsv.absolutePath
                Log.d(TAG, "Re-created missing CSV at: ${ensuredCsv.absolutePath}")
            }
        }

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
        Log.d(TAG, "  - AU CSV: ${auCsvFile?.length() ?: 0} bytes")
        Log.d(TAG, "  - PHQ‑9 CSV: ${phq9CsvFile?.length() ?: 0} bytes")
        Log.d(TAG, "  - Registration ID: $finalRegistrationId")
        Log.d(TAG, "  - Assessment type: $assessmentType")
        Log.d(TAG, "  - GAD‑7 score: $gad7Score")
        Log.d(TAG, "  - PHQ‑9 score: $phqScore")

        coroutineScope.launch {
            try {
                val serverClient = SimpleServerClient(context)

                val assessmentData = AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = assessmentType,
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,
                    gad7CsvFile = if (assessmentType == "anxiety") phq9CsvFile else null,
                    phq9CsvFile = if (assessmentType == "depression") phq9CsvFile else null,
                    email = email ?: "",
                    registrationId = finalRegistrationId,
                    gad7Score = gad7Score,
                    phqScore = phqScore,
                    aiRawScore = aiRawScore,
                    questionnaireScore = if (assessmentType == "depression") phqScore else gad7Score
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

    /**
     * Fallback: Re‑create the questionnaire CSV if it is missing.
     */
    private fun ensureCsvFileExists(
        context: Context,
        anonymousId: String,
        assessmentType: String,
        score: Int
    ): File? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "unifiedapp")
            val userFolder = File(appFolder, anonymousId)
            if (!userFolder.exists()) userFolder.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val prefix = if (assessmentType == "depression") "PHQ9" else "GAD7"
            val fileName = "${anonymousId}_${assessmentType}_${prefix}_${timestamp}.csv"
            val csvFile = File(userFolder, fileName)

            val csvContent = "question_index,question_text,answer_score\n0,\"Score\",$score"
            csvFile.writeText(csvContent)

            val prefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
            prefs.edit().putString("phq9_csv_path", csvFile.absolutePath).apply()

            Log.d(TAG, "Fallback CSV created at: ${csvFile.absolutePath}")
            return csvFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create fallback CSV: ${e.message}")
            return null
        }
    }
}