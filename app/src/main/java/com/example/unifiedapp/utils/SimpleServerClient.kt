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

    // Assessment Upload Classes with registrationId
    data class AssessmentData(
        val anonymousId: String,
        val age: Int,
        val assessmentType: String,
        val videoFile: File?,
        val auCsvFile: File?,
        val phq9CsvFile: File?,
        val aiRawScore: Float?,
        val email: String?,
        val registrationId: String?  // Registration ID (roll number)
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

    // Anonymous User Registration
    suspend fun registerAnonymousUser(anonymousId: String, age: Int, assessmentType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/auth/register")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("anonymous_id", anonymousId)
                    put("age", age)
                    put("assessment_type", assessmentType)
                    put("user_type", "student")
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = if (responseCode == 200 || responseCode == 201) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                }

                connection.disconnect()

                Log.d(TAG, "Registration response: HTTP $responseCode - $response")
                responseCode == 200 || responseCode == 201
            } catch (e: Exception) {
                Log.e(TAG, "Anonymous registration failed: ${e.message}")
                false
            }
        }
    }

    // Model Update Methods
    suspend fun checkForModelUpdates(callback: ModelUpdateCallback) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking for depression model updates...")

                val currentDepressionVersion = getCurrentDepressionModelVersion()
                val currentDepressionChecksum = getCurrentDepressionModelChecksum()

                val url = URL("$baseUrl/api/models/check-updates")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("current_versions", JSONObject().apply {
                        put("depression", JSONObject().apply {
                            put("version", currentDepressionVersion)
                            put("checksum", currentDepressionChecksum)
                        })
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }

                    val updateInfo = JSONObject(response)
                    val updatesAvailable = updateInfo.optBoolean("updates_available", false)

                    Log.d(TAG, "Updates available: $updatesAvailable")

                    if (updatesAvailable) {
                        val updates = updateInfo.optJSONObject("updates")
                        val depressionUpdate = updates?.optJSONObject("depression")

                        if (depressionUpdate != null) {
                            val modelUpdateInfo = ModelUpdateInfo(
                                version = depressionUpdate.optString("latest_version", "1.0.1"),
                                downloadUrl = "$baseUrl${depressionUpdate.optString("download_url")}",
                                sizeKb = depressionUpdate.optInt("size_kb", 0),
                                checksum = depressionUpdate.optString("checksum", "")
                            )
                            Log.d(TAG, "Depression model update available: ${modelUpdateInfo.version}")
                            withContext(Dispatchers.Main) {
                                callback.onUpdateAvailable(modelUpdateInfo)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback.onNoUpdatesAvailable()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            callback.onNoUpdatesAvailable()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onUpdateError("Server returned: $responseCode")
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onUpdateError("Update check failed: ${e.message}")
                }
            }
        }
    }

    suspend fun downloadAndInstallDepressionModel(updateInfo: ModelUpdateInfo, callback: ModelUpdateCallback) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    callback.onUpdateProgress(0, "Starting download...")
                }
                Log.d(TAG, "Downloading depression model from: ${updateInfo.downloadUrl}")

                val url = URL(updateInfo.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    withContext(Dispatchers.Main) {
                        callback.onUpdateProgress(10, "Downloading model...")
                    }

                    val contentLength = connection.contentLength
                    val inputStream = connection.inputStream

                    val internalDir = context.filesDir
                    val downloadedModel = File(internalDir, "depression_model_downloaded.tflite")

                    FileOutputStream(downloadedModel).use { output ->
                        val buffer = ByteArray(4096)
                        var totalBytesRead = 0
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = 10 + ((totalBytesRead.toFloat() / contentLength.toFloat()) * 80).toInt()
                                withContext(Dispatchers.Main) {
                                    callback.onUpdateProgress(progress, "Downloading model...")
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        callback.onUpdateProgress(90, "Installing model...")
                    }

                    // Save version info
                    val versionFile = File(internalDir, "depression_model_version.txt")
                    versionFile.writeText(updateInfo.version)

                    // Save checksum
                    val checksumFile = File(internalDir, "depression_model_checksum.txt")
                    checksumFile.writeText(updateInfo.checksum)

                    Log.d(TAG, "Depression model downloaded successfully: ${downloadedModel.absolutePath}")

                    withContext(Dispatchers.Main) {
                        callback.onUpdateProgress(100, "Model updated successfully!")
                        callback.onUpdateComplete(true, "Depression model updated to ${updateInfo.version}")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onUpdateError("Download failed: $responseCode")
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Model download failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onUpdateError("Download failed: ${e.message}")
                }
            }
        }
    }

    // Helper methods for current model info
    private fun getCurrentDepressionModelVersion(): String {
        return try {
            val versionFile = File(context.filesDir, "depression_model_version.txt")
            if (versionFile.exists()) {
                versionFile.readText().trim()
            } else {
                "ver0.00.00"
            }
        } catch (e: Exception) {
            "ver0.00.00"
        }
    }

    private fun getCurrentDepressionModelChecksum(): String {
        return try {
            val checksumFile = File(context.filesDir, "depression_model_checksum.txt")
            if (checksumFile.exists()) {
                checksumFile.readText().trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    // Assessment Upload Methods with registrationId field
    suspend fun uploadAnonymousAssessment(assessmentData: AssessmentData, callback: UploadCallback) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    callback.onProgress(0, "Preparing upload...")
                }

                Log.d(TAG, "═══════════════════════════════════════════════════════")
                Log.d(TAG, "Starting upload to server...")
                Log.d(TAG, "Anonymous ID: ${assessmentData.anonymousId}")
                Log.d(TAG, "Registration ID: ${assessmentData.registrationId}")
                Log.d(TAG, "Assessment Type: ${assessmentData.assessmentType}")
                Log.d(TAG, "Video file exists: ${assessmentData.videoFile?.exists()}")
                Log.d(TAG, "AU CSV file exists: ${assessmentData.auCsvFile?.exists()}")  // CRITICAL
                Log.d(TAG, "PHQ-9 file exists: ${assessmentData.phq9CsvFile?.exists()}")  // CRITICAL
                Log.d(TAG, "═══════════════════════════════════════════════════════")

                // Validate required files before proceeding
                if (assessmentData.auCsvFile == null || !assessmentData.auCsvFile.exists()) {
                    withContext(Dispatchers.Main) {
                        callback.onError("Action Units CSV file is required but missing")
                    }
                    return@withContext
                }

                if (assessmentData.phq9CsvFile == null || !assessmentData.phq9CsvFile.exists()) {
                    withContext(Dispatchers.Main) {
                        callback.onError("PHQ-9 CSV file is required but missing")
                    }
                    return@withContext
                }

                val boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "")

                val url = URL("$baseUrl/api/student/upload-assessment")
                Log.d(TAG, "Upload URL: $url")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 60000
                connection.readTimeout = 60000

                val outputStream = connection.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                // Add ALL required text fields
                addFormField(writer, boundary, "anonymous_id", assessmentData.anonymousId)
                addFormField(writer, boundary, "registration_id", assessmentData.registrationId ?: "")
                addFormField(writer, boundary, "age", assessmentData.age.toString())
                addFormField(writer, boundary, "assessment_type", assessmentData.assessmentType) // "depression"

                // Optional fields
                assessmentData.aiRawScore?.let {
                    addFormField(writer, boundary, "ai_raw_score", it.toString())
                }

                assessmentData.email?.let {
                    addFormField(writer, boundary, "email", it)
                }

                // Add default values for optional fields
                addFormField(writer, boundary, "app_version", "1.0.0")
                addFormField(writer, boundary, "phone_model", android.os.Build.MODEL)
                addFormField(writer, boundary, "questionnaire_data", "{}")

                var progressStep = 0
                val totalFiles = 3 // video, au_csv, phq_csv

                // 1. Add video file (REQUIRED)
                assessmentData.videoFile?.let { file ->
                    if (file.exists()) {
                        progressStep++
                        addFileField(writer, outputStream, boundary, "video", file, "video/mp4")
                        val progressPercent = (progressStep * 90 / totalFiles)
                        withContext(Dispatchers.Main) {
                            callback.onProgress(progressPercent, "Uploading video...")
                        }
                        Log.d(TAG, "✅ Video file added")
                    }
                }

                // 2. Add AU CSV file (REQUIRED by server)
                assessmentData.auCsvFile?.let { file ->
                    if (file.exists()) {
                        progressStep++
                        addFileField(writer, outputStream, boundary, "au_csv", file, "text/csv")
                        val progressPercent = (progressStep * 90 / totalFiles)
                        withContext(Dispatchers.Main) {
                            callback.onProgress(progressPercent, "Uploading AU data...")
                        }
                        Log.d(TAG, "✅ AU CSV file added")
                    }
                }

                // 3. Add PHQ-9 CSV file (REQUIRED for depression)
                assessmentData.phq9CsvFile?.let { file ->
                    if (file.exists()) {
                        progressStep++
                        addFileField(writer, outputStream, boundary, "phq_csv", file, "text/csv")
                        val progressPercent = (progressStep * 90 / totalFiles)
                        withContext(Dispatchers.Main) {
                            callback.onProgress(progressPercent, "Uploading PHQ-9 data...")
                        }
                        Log.d(TAG, "✅ PHQ-9 CSV file added")
                    }
                }

                // End multipart
                writer.append("--$boundary--").append("\r\n")
                writer.close()

                val responseCode = connection.responseCode
                Log.d(TAG, "Server response code: $responseCode")

                val response = if (responseCode == 200 || responseCode == 201) {
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

                if (responseCode == 200 || responseCode == 201) {
                    withContext(Dispatchers.Main) {
                        callback.onProgress(100, "Upload completed!")
                        callback.onSuccess("Assessment data uploaded successfully")
                    }
                } else {
                    // Parse error to see what's missing
                    val errorMessage = when {
                        response.contains("au_csv") -> "AU CSV file is required by server"
                        response.contains("phq_csv") -> "PHQ-9 CSV file is required by server"
                        response.contains("registration_id") -> "Registration ID is required"
                        else -> "Upload failed (HTTP $responseCode): $response"
                    }
                    withContext(Dispatchers.Main) {
                        callback.onError(errorMessage)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback.onError("Upload failed: ${e.message}")
                }
            }
        }
    }

    private fun addFormField(writer: PrintWriter, boundary: String, name: String, value: String) {
        writer.append("--$boundary").append("\r\n")
        writer.append("Content-Disposition: form-data; name=\"$name\"").append("\r\n")
        writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n")
        writer.append("\r\n")
        writer.append(value).append("\r\n")
        writer.flush()
    }

    private fun addFileField(writer: PrintWriter, outputStream: OutputStream, boundary: String,
                             fieldName: String, file: File, mimeType: String) {
        writer.append("--$boundary").append("\r\n")
        writer.append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"").append("\r\n")
        writer.append("Content-Type: $mimeType").append("\r\n")
        writer.append("Content-Transfer-Encoding: binary").append("\r\n")
        writer.append("\r\n")
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

    // Get server status info
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