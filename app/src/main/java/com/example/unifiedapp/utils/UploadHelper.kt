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
        val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)

        val assessmentType = prefs.getString("assessment_type", "depression") ?: "depression"
        val gad7Score = prefs.getInt("gad7_score", 0)
        val phqScore = prefs.getInt("phq_score", 0)

        Log.d(TAG, "Assessment type: $assessmentType, PHQ score: $phqScore, GAD score: $gad7Score")

        // Get existing paths
        val videoPath = filePrefs.getString("video_path", null)
        val auCsvPath = filePrefs.getString("au_csv_path", null)
        var phq9CsvPath = filePrefs.getString("phq9_csv_path", null)

        var videoFile = videoPath?.let { File(it) }
        var auCsvFile = auCsvPath?.let { File(it) }
        var phq9CsvFile = phq9CsvPath?.let { File(it) }

        // ✅ FOR DEPRESSION: ALWAYS CREATE A FRESH CSV WITH THE CORRECT SCORE
        if (assessmentType == "depression") {
            Log.d(TAG, "Depression assessment – creating fresh PHQ‑9 CSV with score $phqScore")
            val freshCsv = createCsvFile(context, anonymousId, "depression", phqScore)
            if (freshCsv != null) {
                phq9CsvFile = freshCsv
                phq9CsvPath = freshCsv.absolutePath
                // Update shared preferences so future uploads can use it
                filePrefs.edit().putString("phq9_csv_path", phq9CsvPath).apply()
                Log.d(TAG, "Fresh PHQ‑9 CSV created: ${freshCsv.absolutePath}, size=${freshCsv.length()}")
            } else {
                Log.e(TAG, "Failed to create fresh PHQ‑9 CSV")
            }
        } else if (assessmentType == "anxiety") {
            // For anxiety, create a GAD‑7 CSV if missing
            if (phq9CsvFile == null || !phq9CsvFile.exists()) {
                Log.d(TAG, "Anxiety assessment – creating GAD‑7 CSV with score $gad7Score")
                val freshCsv = createCsvFile(context, anonymousId, "anxiety", gad7Score)
                if (freshCsv != null) {
                    phq9CsvFile = freshCsv
                    phq9CsvPath = freshCsv.absolutePath
                    filePrefs.edit().putString("phq9_csv_path", phq9CsvPath).apply()
                    Log.d(TAG, "Fresh GAD‑7 CSV created: ${freshCsv.absolutePath}")
                }
            }
        }

        if (videoFile == null || !videoFile.exists()) {
            onError("Video file not found. Please retake the assessment.")
            return
        }

        val finalRegistrationId = when {
            !registrationId.isNullOrBlank() -> registrationId
            else -> {
                val userPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                userPrefs.getString("registration_id", null) ?: anonymousId
            }
        }

        Log.d(TAG, "Final file list:")
        Log.d(TAG, "  Video: ${videoFile.absolutePath} (${videoFile.length()} bytes)")
        Log.d(TAG, "  AU CSV: ${auCsvFile?.absolutePath} (${auCsvFile?.length() ?: 0} bytes)")
        Log.d(TAG, "  Questionnaire CSV: ${phq9CsvFile?.absolutePath} (${phq9CsvFile?.length() ?: 0} bytes)")

        coroutineScope.launch {
            try {
                val serverClient = SimpleServerClient(context)

                // Build AssessmentData for the upload
                var assessmentData = AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = assessmentType,
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,
                    phq9CsvFile = if (assessmentType == "depression") phq9CsvFile else null,
                    gad7CsvFile = if (assessmentType == "anxiety") phq9CsvFile else null,
                    email = email ?: "",
                    registrationId = finalRegistrationId,
                    gad7Score = gad7Score,
                    phqScore = phqScore,
                    aiRawScore = aiRawScore,
                    questionnaireScore = if (assessmentType == "depression") phqScore else gad7Score
                )

                // ✅ EMERGENCY SAFETY NET: if depression and still no CSV, create one on the spot
                if (assessmentType == "depression" && assessmentData.phq9CsvFile == null) {
                    Log.e(TAG, "EMERGENCY: phq9CsvFile is null right before upload! Creating one now.")
                    val emergencyCsv = createCsvFile(context, anonymousId, "depression", phqScore)
                    if (emergencyCsv != null) {
                        assessmentData = assessmentData.copy(phq9CsvFile = emergencyCsv)
                        Log.d(TAG, "Emergency CSV attached: ${emergencyCsv.absolutePath}")
                    } else {
                        Log.e(TAG, "Failed to create emergency CSV – upload will proceed without it.")
                    }
                }

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
            } catch (e: Exception) {
                Log.e(TAG, "Exception during upload: ${e.message}", e)
                onError("Upload failed: ${e.message}")
            }
        }
    }

    private fun createCsvFile(
        context: Context,
        anonymousId: String,
        assessmentType: String,
        score: Int
    ): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "unifiedapp")
            val userFolder = File(appFolder, anonymousId)
            if (!userFolder.exists()) userFolder.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val prefix = if (assessmentType == "depression") "PHQ9" else "GAD7"
            val fileName = "${anonymousId}_${assessmentType}_${prefix}_${timestamp}.csv"
            val csvFile = File(userFolder, fileName)

            val csvContent = buildString {
                appendLine("question_index,question_text,answer_score")
                appendLine("0,\"Total Score\",$score")
                appendLine("1,\"Assessment completed\",1")
            }
            csvFile.writeText(csvContent)

            Log.d(TAG, "CSV file created: ${csvFile.absolutePath}, size=${csvFile.length()}, score=$score")
            csvFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create CSV file: ${e.message}")
            null
        }
    }
}