package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class SimpleServerClient(private val context: Context) {

    companion object {
        private const val TAG = "SimpleServerClient"
        private const val SERVER_IP = "203.110.243.202"
        private const val SERVER_PORT = 8000
    }

    private val baseUrl = "http://$SERVER_IP:$SERVER_PORT"

    // Model Update Classes
    data class ModelUpdateInfo(
        val version: String,
        val downloadUrl: String,
        val sizeKb: Int,
        val checksum: String
    )

    interface ModelUpdateCallback {
        fun onUpdateAvailable(updateInfo: ModelUpdateInfo)
        fun onUpdateProgress(progress: Int, message: String)
        fun onUpdateComplete(success: Boolean, message: String)
        fun onNoUpdatesAvailable()
        fun onUpdateError(error: String)
    }

    // Assessment Upload Classes
    data class AssessmentData(
        val anonymousId: String,
        val age: Int,
        val assessmentType: String,
        val videoFile: File?,
        val auCsvFile: File?,
        val phq9CsvFile: File?,
        val aiRawScore: Float?,
        val email: String?,
        val registrationId: String?
    )

    interface UploadCallback {
        fun onProgress(progress: Int, message: String)
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    // Server Connection Check
    suspend fun checkServerConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                connection.disconnect()
                Log.d(TAG, "Server connection check: HTTP $responseCode")
                responseCode == 200
            } catch (e: Exception) {
                Log.e(TAG, "Server connection failed: ${e.message}")
                false
            }
        }
    }

    // ============================================================
    // FIXED: Use the correct endpoint /api/student/assessment
    // ============================================================
    suspend fun uploadAnonymousAssessment(
        assessmentData: AssessmentData,
        callback: UploadCallback
    ) {
        withContext(Dispatchers.IO) {
            try {
                callback.onProgress(0, "Preparing upload...")

                Log.d(TAG, "═══════════════════════════════════════════════════════")
                Log.d(TAG, "🚀 Starting upload to server...")
                Log.d(TAG, "Anonymous ID: ${assessmentData.anonymousId}")
                Log.d(TAG, "Registration ID: ${assessmentData.registrationId}")
                Log.d(TAG, "Assessment Type: ${assessmentData.assessmentType}")
                Log.d(TAG, "Video file exists: ${assessmentData.videoFile?.exists()}")
                Log.d(TAG, "AU CSV file exists: ${assessmentData.auCsvFile?.exists()}")
                Log.d(TAG, "PHQ-9 file exists: ${assessmentData.phq9CsvFile?.exists()}")
                Log.d(TAG, "═══════════════════════════════════════════════════════")

                // Validate required files
                if (assessmentData.videoFile == null || !assessmentData.videoFile.exists()) {
                    callback.onError("Video file is required but missing")
                    return@withContext
                }

                // ✅ FIXED: Use the correct endpoint
                val url = URL("$baseUrl/api/student/assessment")
                Log.d(TAG, "Upload URL: $url")

                val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 60000
                connection.readTimeout = 60000

                val outputStream = connection.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                // Helper functions
                fun addField(name: String, value: String) {
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    writer.append(value).append("\r\n")
                    writer.flush()
                }

                fun addFile(name: String, file: File, mimeType: String) {
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n")
                    writer.append("Content-Type: $mimeType\r\n\r\n")
                    writer.flush()

                    FileInputStream(file).use { input ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }
                    writer.append("\r\n")
                    writer.flush()
                }

                // Add form fields (matching backend expectations)
                addField("anonymous_id", assessmentData.anonymousId)

                // ✅ CRITICAL FIX: Ensure registration_id is never null or blank
                val safeRegistrationId = if (assessmentData.registrationId.isNullOrBlank()) {
                    assessmentData.anonymousId
                } else {
                    assessmentData.registrationId
                }
                addField("registration_id", safeRegistrationId)

                addField("age", assessmentData.age.toString())
                addField("assessment_type", assessmentData.assessmentType)

                // Add questionnaire scores based on assessment type
                if (assessmentData.assessmentType == "depression") {
                    addField("phq_score", "0")  // Will be calculated from CSV
                } else {
                    addField("gad_score", "0")
                }

                assessmentData.aiRawScore?.let {
                    addField("anxiety_score", it.toString())
                    addField("depression_score", it.toString())
                }

                // Add files
                addFile("video", assessmentData.videoFile, "video/mp4")

                if (assessmentData.auCsvFile != null && assessmentData.auCsvFile.exists()) {
                    addFile("au_csv", assessmentData.auCsvFile, "text/csv")
                    Log.d(TAG, "✅ AU CSV file added")
                }

                if (assessmentData.phq9CsvFile != null && assessmentData.phq9CsvFile.exists()) {
                    addFile("phq_csv", assessmentData.phq9CsvFile, "text/csv")
                    Log.d(TAG, "✅ PHQ-9 CSV file added")
                }

                // End multipart
                writer.append("--$boundary--\r\n")
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                Log.d(TAG, "Server response code: $responseCode")

                val response = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                            reader.readText()
                        }
                    } catch (e: Exception) {
                        "HTTP $responseCode"
                    }
                }

                Log.d(TAG, "Server response: $response")
                connection.disconnect()

                if (responseCode in 200..299) {
                    callback.onProgress(100, "Upload completed!")
                    callback.onSuccess("Assessment data uploaded successfully")

                    // Delete local files after successful upload
                    assessmentData.videoFile?.delete()
                    assessmentData.auCsvFile?.delete()
                    assessmentData.phq9CsvFile?.delete()
                    Log.d(TAG, "Local files deleted")
                } else {
                    callback.onError("Upload failed (HTTP $responseCode): $response")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                callback.onError("Upload failed: ${e.message}")
            }
        }
    }

    // Test server connection
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val responseCode = connection.responseCode
                connection.disconnect()
                Log.d(TAG, "Connection test: HTTP $responseCode")
                responseCode == 200
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed: ${e.message}")
                false
            }
        }
    }

    suspend fun getServerStatus(): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                    connection.disconnect()
                    "Server Online: $response"
                } else {
                    connection.disconnect()
                    "Server Error: $responseCode"
                }
            } catch (e: Exception) {
                "Server Offline: ${e.message}"
            }
        }
    }
}
