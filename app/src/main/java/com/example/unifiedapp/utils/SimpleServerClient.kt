package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class SimpleServerClient(private val context: Context) {

    companion object {
        private const val TAG = "SimpleServerClient"
        private const val SERVER_IP = "203.110.243.202"
        private const val SERVER_PORT = 8000
        private const val LINE_FEED = "\r\n"
    }

    private val baseUrl = "http://$SERVER_IP:$SERVER_PORT"

    data class AssessmentData(
        val anonymousId: String,
        val age: Int,
        val assessmentType: String,
        val videoFile: File?,
        val auCsvFile: File?,
        val phq9CsvFile: File?,
        val aiRawScore: Float?,
        val email: String?,
        val registrationId: String?,
        val gad7Score: Int = 0,
        val phqScore: Int = 0
    )

    interface UploadCallback {
        fun onProgress(progress: Int, message: String)
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    suspend fun checkServerConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun uploadAnonymousAssessment(
        assessmentData: AssessmentData,
        callback: UploadCallback
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                withContext(Dispatchers.Main) {
                    callback.onProgress(10, "Preparing upload...")
                }

                val url = URL("$baseUrl/api/student/assessment")
                val boundary = "Boundary-" + System.currentTimeMillis()

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    useCaches = false
                    connectTimeout = 60000
                    readTimeout = 60000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    setRequestProperty("Accept", "application/json")
                }

                val outputStream = connection.outputStream
                val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))

                fun addFormField(name: String, value: String) {
                    writer.append("--$boundary").append(LINE_FEED)
                    writer.append("Content-Disposition: form-data; name=\"$name\"").append(LINE_FEED)
                    writer.append(LINE_FEED)
                    writer.append(value).append(LINE_FEED)
                    writer.flush()
                }

                fun addFilePart(fieldName: String, uploadFile: File, mimeType: String) {
                    writer.append("--$boundary").append(LINE_FEED)
                    writer.append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${uploadFile.name}\"").append(LINE_FEED)
                    writer.append("Content-Type: $mimeType").append(LINE_FEED)
                    writer.append(LINE_FEED)
                    writer.flush()

                    val inputStream = FileInputStream(uploadFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    inputStream.close()

                    writer.append(LINE_FEED)
                    writer.flush()
                }

                val finalRegId = assessmentData.registrationId?.takeIf { it.isNotBlank() } ?: assessmentData.anonymousId

                Log.d("UPLOAD_DEBUG", "Sending registration_id: $finalRegId")
                addFormField("registration_id", finalRegId)
                addFormField("anonymous_id", assessmentData.anonymousId)
                addFormField("age", assessmentData.age.toString())
                addFormField("assessment_type", assessmentData.assessmentType)

                if (assessmentData.assessmentType.equals("depression", ignoreCase = true)) {
                    addFormField("phq_score", assessmentData.phqScore.toString())
                } else if (assessmentData.assessmentType.equals("anxiety", ignoreCase = true)) {
                    addFormField("gad_score", assessmentData.gad7Score.toString())
                }

                assessmentData.aiRawScore?.let {
                    addFormField("ai_raw_score", it.toString())
                }
                assessmentData.email?.let {
                    addFormField("email", it)
                }

                withContext(Dispatchers.Main) {
                    callback.onProgress(30, "Adding files...")
                }

                assessmentData.videoFile?.let {
                    addFilePart("video", it, "video/mp4")
                    withContext(Dispatchers.Main) {
                        callback.onProgress(50, "Video added")
                    }
                }

                assessmentData.auCsvFile?.let {
                    addFilePart("au_csv", it, "text/csv")
                    withContext(Dispatchers.Main) {
                        callback.onProgress(70, "AU data added")
                    }
                }

                assessmentData.phq9CsvFile?.let {
                    addFilePart("phq_csv", it, "text/csv")
                    withContext(Dispatchers.Main) {
                        callback.onProgress(85, "Questionnaire data added")
                    }
                }

                writer.append("--$boundary--").append(LINE_FEED)
                writer.flush()
                writer.close()

                withContext(Dispatchers.Main) {
                    callback.onProgress(95, "Uploading...")
                }

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    Log.d(TAG, "Server response: $response")
                    withContext(Dispatchers.Main) {
                        callback.onSuccess("Assessment uploaded successfully")
                        callback.onProgress(100, "Done")
                    }
                    // Clean up local files
                    assessmentData.videoFile?.delete()
                    assessmentData.auCsvFile?.delete()
                    assessmentData.phq9CsvFile?.delete()
                } else {
                    val errorStream = connection.errorStream ?: connection.inputStream
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = errorReader.readText()
                    errorReader.close()
                    val errorMsg = "Server error $responseCode: $responseMessage\n$errorResponse"
                    Log.e(TAG, errorMsg)
                    withContext(Dispatchers.Main) {
                        callback.onError(errorMsg)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Network error: ${e.message}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }
}