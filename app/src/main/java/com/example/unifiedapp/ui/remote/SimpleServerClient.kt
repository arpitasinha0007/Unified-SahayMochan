package com.example.unifiedapp.ui.remote

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
                connection.disconnect()
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
                Log.d(TAG, "Checking for model updates...")
                val currentAnxietyVersion = getCurrentAnxietyModelVersion()
                val currentAnxietyChecksum = getCurrentAnxietyModelChecksum()
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
                        put("anxiety", JSONObject().apply {
                            put("version", currentAnxietyVersion)
                            put("checksum", currentAnxietyChecksum)
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
                        val anxietyUpdate = updates?.optJSONObject("anxiety")
                        if (anxietyUpdate != null) {
                            val modelUpdateInfo = ModelUpdateInfo(
                                version = anxietyUpdate.optString("latest_version", "1.0.1"),
                                downloadUrl = "$baseUrl${anxietyUpdate.optString("download_url")}",
                                sizeKb = anxietyUpdate.optInt("size_kb", 0),
                                checksum = anxietyUpdate.optString("checksum", "")
                            )
                            Log.d(TAG, "Anxiety model update available: ${modelUpdateInfo.version}")
                            withContext(Dispatchers.Main) { callback.onUpdateAvailable(modelUpdateInfo) }
                        } else {
                            withContext(Dispatchers.Main) { callback.onNoUpdatesAvailable() }
                        }
                    } else {
                        withContext(Dispatchers.Main) { callback.onNoUpdatesAvailable() }
                    }
                } else {
                    withContext(Dispatchers.Main) { callback.onUpdateError("Server returned: $responseCode") }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                withContext(Dispatchers.Main) { callback.onUpdateError("Update check failed: ${e.message}") }
            }
        }
    }

    suspend fun downloadAndInstallAnxietyModel(updateInfo: ModelUpdateInfo, callback: ModelUpdateCallback) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { callback.onUpdateProgress(0, "Starting download...") }
                Log.d(TAG, "Downloading anxiety model from: ${updateInfo.downloadUrl}")
                val url = URL(updateInfo.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    withContext(Dispatchers.Main) { callback.onUpdateProgress(10, "Downloading model...") }
                    val contentLength = connection.contentLength
                    val inputStream = connection.inputStream
                    val internalDir = context.filesDir
                    val downloadedModel = File(internalDir, "anxiety_model_downloaded.tflite")
                    FileOutputStream(downloadedModel).use { output ->
                        val buffer = ByteArray(4096)
                        var totalBytesRead = 0
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = 10 + ((totalBytesRead.toFloat() / contentLength.toFloat()) * 80).toInt()
                                withContext(Dispatchers.Main) { callback.onUpdateProgress(progress, "Downloading model...") }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) { callback.onUpdateProgress(90, "Installing model...") }
                    val versionFile = File(internalDir, "model_version.txt")
                    versionFile.writeText(updateInfo.version)
                    val checksumFile = File(internalDir, "model_checksum.txt")
                    checksumFile.writeText(updateInfo.checksum)
                    Log.d(TAG, "Anxiety model downloaded successfully: ${downloadedModel.absolutePath}")
                    withContext(Dispatchers.Main) {
                        callback.onUpdateProgress(100, "Model updated successfully!")
                        callback.onUpdateComplete(true, "Anxiety model updated to ${updateInfo.version}")
                    }
                } else {
                    withContext(Dispatchers.Main) { callback.onUpdateError("Download failed: $responseCode") }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed: ${e.message}")
                withContext(Dispatchers.Main) { callback.onUpdateError("Download failed: ${e.message}") }
            }
        }
    }

    private fun getCurrentAnxietyModelVersion(): String {
        return try {
            val versionFile = File(context.filesDir, "model_version.txt")
            if (versionFile.exists()) versionFile.readText().trim() else "ver0.00.00"
        } catch (e: Exception) { "ver0.00.00" }
    }

    private fun getCurrentAnxietyModelChecksum(): String {
        return try {
            val checksumFile = File(context.filesDir, "model_checksum.txt")
            if (checksumFile.exists()) checksumFile.readText().trim() else ""
        } catch (e: Exception) { "" }
    }

    // 🔹 Emergency fallback to create a questionnaire CSV if missing
    private fun ensureQuestionnaireFile(assessmentType: String, score: Int): File? {
        return try {
            val prefix = if (assessmentType.equals("depression", ignoreCase = true)) "PHQ9" else "GAD7"
            val tempFile = File(context.cacheDir, "temp_${prefix}_${System.currentTimeMillis()}.csv")
            val content = "question_index,question_text,answer_score\n0,\"Total Score\",$score\n1,\"Assessment completed\",1"
            tempFile.writeText(content)
            Log.d(TAG, "⚠️ Created emergency $prefix CSV: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create emergency CSV: ${e.message}")
            null
        }
    }

    // Assessment Upload Methods
    suspend fun uploadAnonymousAssessment(
        assessmentData: com.example.unifiedapp.ui.views.AssessmentData,
        callback: UploadCallback
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("UPLOAD_DEBUG", "========== UPLOAD START ==========")
                Log.d("UPLOAD_DEBUG", "AnonymousId: ${assessmentData.anonymousId}")
                Log.d("UPLOAD_DEBUG", "Age: ${assessmentData.age}")
                Log.d("UPLOAD_DEBUG", "Assessment Type: ${assessmentData.assessmentType}")
                Log.d("UPLOAD_DEBUG", "Video exists: ${assessmentData.videoFile?.exists()}")
                Log.d("UPLOAD_DEBUG", "AU CSV exists: ${assessmentData.auCsvFile?.exists()}")
                Log.d("UPLOAD_DEBUG", "GAD7 CSV exists: ${assessmentData.gad7CsvFile?.exists()}")
                Log.d("UPLOAD_DEBUG", "Video file path: ${assessmentData.videoFile?.absolutePath}")
                Log.d("UPLOAD_DEBUG", "Video file size: ${assessmentData.videoFile?.length()}")
                Log.d("UPLOAD_DEBUG", "Registration Id: ${assessmentData.registrationId}")
                Log.d("UPLOAD_DEBUG", "GAD-7 Score: ${assessmentData.gad7Score}")
                Log.d("UPLOAD_DEBUG", "Questionnaire Score: ${assessmentData.questionnaireScore}")

                assessmentData.videoFile?.let { Log.d("UPLOAD_DEBUG", "Video size: ${it.length()} bytes") }
                assessmentData.auCsvFile?.let { Log.d("UPLOAD_DEBUG", "AU CSV size: ${it.length()} bytes") }
                assessmentData.gad7CsvFile?.let { Log.d("UPLOAD_DEBUG", "GAD7 CSV size: ${it.length()} bytes") }

                withContext(Dispatchers.Main) { callback.onProgress(0, "Preparing upload...") }

                val boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "")
                val fullUrl = "$baseUrl/api/student/upload-assessment"
                Log.d("UPLOAD_DEBUG", "Connecting to: $fullUrl")

                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 60000
                connection.readTimeout = 60000

                val outputStream = connection.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                fun addFormField(name: String, value: String) {
                    writer.append("--$boundary").append("\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"").append("\r\n")
                    writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n")
                    writer.append("\r\n")
                    writer.append(value).append("\r\n")
                    writer.flush()
                }

                fun addFileField(fieldName: String, file: File, mimeType: String) {
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

                // Text fields
                addFormField("registration_id", assessmentData.registrationId ?: assessmentData.anonymousId)
                addFormField("anonymous_id", assessmentData.anonymousId)
                addFormField("age", assessmentData.age.toString())
                addFormField("assessment_type", assessmentData.assessmentType)
                addFormField("gad7_score", assessmentData.gad7Score.toString())
                addFormField("questionnaire_score", assessmentData.questionnaireScore.toString())

                var progressStep = 0
                val totalFiles = listOfNotNull(
                    assessmentData.videoFile,
                    assessmentData.auCsvFile,
                    assessmentData.gad7CsvFile
                ).size
                Log.d("UPLOAD_DEBUG", "Total files to upload: $totalFiles")

                // Video
                assessmentData.videoFile?.let { file ->
                    if (file.exists()) {
                        progressStep++
                        Log.d("UPLOAD_DEBUG", "Uploading VIDEO...")
                        addFileField("video", file, "video/mp4")
                        withContext(Dispatchers.Main) {
                            callback.onProgress((progressStep * 90 / totalFiles), "Video uploaded...")
                        }
                    }
                }

                // AU CSV
                assessmentData.auCsvFile?.let { file ->
                    if (file.exists()) {
                        progressStep++
                        Log.d("UPLOAD_DEBUG", "Uploading AU CSV...")
                        addFileField("au_csv", file, "text/csv")
                        withContext(Dispatchers.Main) {
                            callback.onProgress((progressStep * 90 / totalFiles), "AU data uploaded...")
                        }
                    }
                }

                // 🔹 Questionnaire CSV (emergency fallback)
                val type = assessmentData.assessmentType
                val score = if (type.equals("depression", ignoreCase = true)) assessmentData.phqScore else assessmentData.gad7Score
                var csvFile = assessmentData.gad7CsvFile
                if (csvFile == null || !csvFile.exists() || csvFile.length() == 0L) {
                    csvFile = ensureQuestionnaireFile(type, score)
                }
                if (csvFile != null) {
                    val fieldName = if (type.equals("depression", ignoreCase = true)) "phq_csv" else "gad_csv"
                    progressStep++
                    Log.d("UPLOAD_DEBUG", "Uploading ${fieldName.uppercase()}...")
                    addFileField(fieldName, csvFile, "text/csv")
                    withContext(Dispatchers.Main) {
                        callback.onProgress((progressStep * 90 / totalFiles), "Questionnaire data uploaded...")
                    }
                } else {
                    Log.e(TAG, "Failed to attach questionnaire CSV")
                }

                writer.append("--$boundary--").append("\r\n")
                writer.close()

                Log.d("UPLOAD_DEBUG", "Waiting for server response...")
                val responseCode = connection.responseCode
                Log.d("UPLOAD_DEBUG", "Server response code: $responseCode")

                val responseBody = try {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } catch (e: Exception) {
                    try { BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() } }
                    catch (e2: Exception) { "No response body" }
                }

                Log.d("UPLOAD_DEBUG", "Server response body: $responseBody")

                if (responseCode == 200 || responseCode == 201) {
                    withContext(Dispatchers.Main) {
                        callback.onProgress(100, "Upload completed!")
                        callback.onSuccess("Assessment data uploaded successfully")
                    }
                } else {
                    withContext(Dispatchers.Main) { callback.onError("Upload failed: $responseBody") }
                }

                connection.disconnect()
                Log.d("UPLOAD_DEBUG", "========== UPLOAD END ==========")

            } catch (e: Exception) {
                Log.e("UPLOAD_DEBUG", "Upload exception: ${e.message}")
                withContext(Dispatchers.Main) { callback.onError("Upload failed: ${e.message}") }
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

    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/system/status")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                Log.d(TAG, "Server test response: $responseCode")
                connection.disconnect()
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
                val url = URL("$baseUrl/api/system/status")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val statusInfo = JSONObject(response)
                    val stats = statusInfo.optJSONObject("statistics")
                    val totalUsers = stats?.optInt("total_users", 0) ?: 0
                    val totalAssessments = stats?.optInt("total_assessments", 0) ?: 0
                    connection.disconnect()
                    "Server Online: $totalUsers users, $totalAssessments assessments"
                } else {
                    connection.disconnect()
                    "Server Error: $responseCode"
                }
            } catch (e: Exception) {
                "Server Offline: ${e.message}"
            }
        }
    }

    suspend fun sendReportToParentEmail(
        registrationId: String,
        parentEmail: String,
        pdfFile: File,
        subject: String,
        message: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                Log.d(TAG, "Sending report to parent email: $parentEmail")
                Log.d(TAG, "PDF file size: ${pdfFile.length()} bytes")
                val url = URL("$baseUrl/api/send-email-with-pdf")
                connection = url.openConnection() as HttpURLConnection
                val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                val outputStream = connection.outputStream
                val writer = java.io.PrintWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"), true)

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"registration_id\"\r\n\r\n")
                writer.append(registrationId).append("\r\n")
                writer.flush()

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"subject\"\r\n\r\n")
                writer.append(subject).append("\r\n")
                writer.flush()

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"message\"\r\n\r\n")
                writer.append(message).append("\r\n")
                writer.flush()

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"pdf_file\"; filename=\"${pdfFile.name}\"\r\n")
                writer.append("Content-Type: application/pdf\r\n\r\n")
                writer.flush()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                java.io.FileInputStream(pdfFile).use { inputStream ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
                outputStream.flush()

                writer.append("\r\n")
                writer.append("--$boundary--\r\n")
                writer.flush()

                val responseCode = connection.responseCode
                Log.d(TAG, "Email send response code: $responseCode")
                val responseBody = try {
                    java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream)).use { it.readText() }
                } catch (e: Exception) {
                    try { java.io.BufferedReader(java.io.InputStreamReader(connection.errorStream)).use { it.readText() } }
                    catch (e2: Exception) { "" }
                }
                Log.d(TAG, "Email send response: $responseBody")
                responseCode == 200 || responseCode == 201
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send email", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }
}