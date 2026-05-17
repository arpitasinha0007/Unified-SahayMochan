package com.example.unifiedapp.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Unified API Service for MochanApp
 * Base URL: http://203.110.243.202:8000
 */
object ApiService {

    private const val TAG = "ApiService"
    private const val BASE_URL = "http://203.110.243.202:8000"

    // ==================== AUTHENTICATION ENDPOINTS ====================

    /**
     * Send OTP for phone verification during registration
     */
    suspend fun sendPhoneOtp(phoneNumber: String): ApiResponse<OtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/send-phone-otp")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("phone_number", phoneNumber)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    ApiResponse.Success(OtpResponse(
                        verificationId = json.optString("verification_id", ""),
                        expiresIn = json.optInt("expires_in", 600),
                        testMode = json.optBoolean("test_mode", false),
                        demoOtp = json.optString("demo_otp", null)
                    ))
                } else {
                    ApiResponse.Error("Failed to send OTP: $responseCode")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * Verify OTP for phone verification
     */
    suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): ApiResponse<VerifyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/verify-phone-otp")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("phone_number", phoneNumber)
                    put("otp_code", otpCode)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    ApiResponse.Success(VerifyResponse(
                        verified = json.optBoolean("verified", false),
                        phoneNumber = json.optString("phone_number", phoneNumber),
                        message = json.optString("message", "")
                    ))
                } else {
                    ApiResponse.Error("Invalid OTP or verification failed")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * Register new user with OTP verification
     */
    suspend fun registerUser(
        registrationId: String,
        password: String,
        name: String,
        gender: String,
        email: String,
        age: Int,
        phoneNumber: String,
        parentName: String? = null,
        parentEmail: String? = null,
        isUnderage: Boolean = age < 18
    ): ApiResponse<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/register-user")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("registration_id", registrationId)
                    put("password", password)
                    put("roll_no", registrationId)
                    put("name", name)
                    put("gender", gender)
                    put("email", email)
                    put("age", age)
                    put("phone_no", phoneNumber)

                    if (isUnderage && parentName != null && parentEmail != null) {
                        put("parent_name", parentName)
                        put("parent_email", parentEmail)
                        put("is_underage", true)
                    }
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                connection.disconnect()

                if (responseCode in 200..299) {
                    val json = JSONObject(response)
                    ApiResponse.Success(RegisterResponse(
                        success = true,
                        message = json.optString("message", "Registration successful"),
                        registrationId = json.optString("registration_id", registrationId),
                        userId = json.optString("user_id", ""),
                        phoneNumber = json.optString("phone_no", phoneNumber),
                        trials = json.optJSONObject("trials")?.let {
                            TrialsInfo(
                                depression = it.optInt("depression", 20),
                                anxiety = it.optInt("anxiety", 20)
                            )
                        },
                        parent = json.optJSONObject("parent")?.let {
                            ParentInfo(
                                name = it.optString("parent_name", ""),
                                email = it.optString("parent_email", "")
                            )
                        }
                    ))
                } else if (responseCode == 400 && response.contains("already exists")) {
                    ApiResponse.Error("Registration ID already exists")
                } else {
                    ApiResponse.Error("Registration failed: $response")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * Login user
     */
    suspend fun loginUser(registrationId: String, password: String): ApiResponse<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/login-user")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("registration_id", registrationId)
                    put("password", password)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                connection.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    ApiResponse.Success(LoginResponse(
                        success = true,
                        name = json.optString("name", ""),
                        gender = json.optString("gender", ""),
                        email = json.optString("email", ""),
                        age = json.optInt("age", 0),
                        phoneNumber = json.optString("phone_no", ""),
                        userId = json.optString("user_id", ""),
                        registrationId = json.optString("registration_id", registrationId),
                        trials = json.optJSONObject("trials")?.let { trialsJson ->
                            TrialsInfo(
                                depression = trialsJson.optJSONObject("depression")?.optInt("remaining", 20) ?: 20,
                                anxiety = trialsJson.optJSONObject("anxiety")?.optInt("remaining", 20) ?: 20
                            )
                        },
                        parent = json.optJSONObject("parent")?.let {
                            ParentInfo(
                                name = it.optString("parent_name", ""),
                                email = it.optString("parent_email", "")
                            )
                        }
                    ))
                } else if (responseCode == 401) {
                    ApiResponse.Error("Invalid registration ID or password")
                } else {
                    ApiResponse.Error("Login failed: $response")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    // ==================== TRIAL MANAGEMENT ENDPOINTS ====================

    /**
     * Get trials info for user
     */
    suspend fun getTrialsInfo(registrationId: String): ApiResponse<TrialsInfoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    connection.disconnect()

                    ApiResponse.Success(TrialsInfoResponse(
                        exists = json.optBoolean("exists", false),
                        depressionRemaining = json.optInt("depression_trials_remaining", 0),
                        anxietyRemaining = json.optInt("anxiety_trials_remaining", 0),
                        canTakeDepression = json.optBoolean("can_take_depression", false),
                        canTakeAnxiety = json.optBoolean("can_take_anxiety", false)
                    ))
                } else {
                    ApiResponse.Error("Failed to get trials info")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * Use a depression trial (call when viewing results)
     */
    suspend fun useDepressionTrial(registrationId: String, assessmentId: String? = null): ApiResponse<TrialUseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/use-trial")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val requestBody = JSONObject().apply {
                    put("registration_id", registrationId)
                    put("assessment_type", "depression")
                    assessmentId?.let { put("assessment_id", it) }
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    ApiResponse.Success(TrialUseResponse(
                        success = json.optBoolean("success", false),
                        message = json.optString("message", ""),
                        trialsRemaining = json.optInt("trials_remaining", 0)
                    ))
                } else {
                    ApiResponse.Error("Failed to use trial")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    // ==================== ASSESSMENT ENDPOINTS ====================

    /**
     * Upload assessment data
     */
    suspend fun uploadAssessment(
        anonymousId: String,
        registrationId: String,
        age: Int,
        aiRawScore: Float?,
        email: String?,
        videoFile: java.io.File,
        auCsvFile: java.io.File,
        phq9CsvFile: java.io.File
    ): ApiResponse<UploadResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
                val url = URL("$BASE_URL/api/student/upload-assessment")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true
                connection.connectTimeout = 60000
                connection.readTimeout = 60000

                val outputStream = connection.outputStream
                val writer = java.io.PrintWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"), true)

                // Add text fields
                fun addField(name: String, value: String) {
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"\r\n")
                    writer.append("Content-Type: text/plain; charset=UTF-8\r\n")
                    writer.append("\r\n")
                    writer.append(value).append("\r\n")
                    writer.flush()
                }

                fun addFile(name: String, file: java.io.File, mimeType: String) {
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n")
                    writer.append("Content-Type: $mimeType\r\n")
                    writer.append("\r\n")
                    writer.flush()

                    file.inputStream().use { input ->
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

                addField("anonymous_id", anonymousId)
                addField("registration_id", registrationId)
                addField("age", age.toString())
                addField("assessment_type", "depression")
                aiRawScore?.let { addField("ai_raw_score", it.toString()) }
                email?.let { addField("email", it) }

                addFile("video", videoFile, "video/mp4")
                addFile("au_csv", auCsvFile, "text/csv")
                addFile("phq_csv", phq9CsvFile, "text/csv")

                writer.append("--$boundary--\r\n")
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                connection.disconnect()

                if (responseCode in 200..299) {
                    ApiResponse.Success(UploadResponse(
                        success = true,
                        message = "Assessment uploaded successfully",
                        assessmentId = JSONObject(response).optString("assessment_id", "")
                    ))
                } else {
                    ApiResponse.Error("Upload failed: $response")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Upload failed: ${e.message}")
            }
        }
    }

    // ==================== ASSESSMENT HISTORY ENDPOINTS ====================

    /**
     * Get assessment history for a user
     */
    suspend fun getAssessmentHistory(registrationId: String): ApiResponse<List<AssessmentHistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/student/${registrationId}/assessments")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val assessmentsArray = json.getJSONArray("assessments")
                    connection.disconnect()

                    val items = mutableListOf<AssessmentHistoryItem>()
                    for (i in 0 until assessmentsArray.length()) {
                        val item = assessmentsArray.getJSONObject(i)
                        items.add(AssessmentHistoryItem(
                            id = item.getInt("id"),
                            createdAt = item.getString("created_at"),
                            phqScore = if (item.has("phq_score") && !item.isNull("phq_score")) item.getInt("phq_score") else null,
                            aiScore = if (item.has("ai_score") && !item.isNull("ai_score")) item.getInt("ai_score") else null,
                            videoCount = item.getInt("video_count")
                        ))
                    }
                    ApiResponse.Success(items)
                } else {
                    ApiResponse.Success(emptyList())
                }
            } catch (e: Exception) {
                ApiResponse.Error("Failed to fetch history: ${e.message}")
            }
        }
    }

    /**
     * Delete a single assessment
     */
    suspend fun deleteAssessment(assessmentId: Int): ApiResponse<DeleteResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/assessment/$assessmentId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                connection.disconnect()

                if (responseCode in 200..299) {
                    ApiResponse.Success(DeleteResponse(
                        success = true,
                        message = "Assessment deleted successfully"
                    ))
                } else {
                    ApiResponse.Error("Failed to delete assessment")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * Delete user account (ALL data)
     */
    suspend fun deleteUserAccount(registrationId: String): ApiResponse<DeleteAccountResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/student/delete-all/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                connection.disconnect()

                if (responseCode in 200..299) {
                    val json = JSONObject(response)
                    val summary = json.optJSONObject("deleted_summary")
                    ApiResponse.Success(DeleteAccountResponse(
                        success = true,
                        message = json.optString("message", "Account deleted successfully"),
                        assessmentsDeleted = summary?.optInt("assessments_deleted", 0) ?: 0,
                        videosDeleted = summary?.optInt("videos_deleted", 0) ?: 0
                    ))
                } else {
                    ApiResponse.Error("Failed to delete account: $response")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }

    // ==================== EMAIL REPORT ENDPOINTS ====================

    /**
     * Send assessment report via email
     */
    suspend fun sendReportByEmail(
        toEmail: String,
        userName: String,
        assessmentType: String,
        severity: String,
        aiPrediction: String? = null
    ): ApiResponse<EmailReportResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/send-report-by-email")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("to_email", toEmail)
                    put("user_name", userName)
                    put("assessment_type", assessmentType)
                    put("severity", severity)
                    aiPrediction?.let { put("ai_prediction", it) }
                    put("assessment_date", java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    ApiResponse.Success(EmailReportResponse(
                        success = true,
                        message = json.optString("message", "Email sent successfully")
                    ))
                } else {
                    ApiResponse.Error("Failed to send email")
                }
            } catch (e: Exception) {
                ApiResponse.Error("Connection failed: ${e.message}")
            }
        }
    }
}

// ==================== RESPONSE DATA CLASSES ====================

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String) : ApiResponse<Nothing>()
}

data class OtpResponse(
    val verificationId: String,
    val expiresIn: Int,
    val testMode: Boolean,
    val demoOtp: String?
)

data class VerifyResponse(
    val verified: Boolean,
    val phoneNumber: String,
    val message: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val registrationId: String,
    val userId: String,
    val phoneNumber: String,
    val trials: TrialsInfo?,
    val parent: ParentInfo?
)

data class LoginResponse(
    val success: Boolean,
    val name: String,
    val gender: String,
    val email: String,
    val age: Int,
    val phoneNumber: String,
    val userId: String,
    val registrationId: String,
    val trials: TrialsInfo?,
    val parent: ParentInfo?
)

data class TrialsInfo(
    val depression: Int,
    val anxiety: Int
)

data class ParentInfo(
    val name: String,
    val email: String
)

data class TrialsInfoResponse(
    val exists: Boolean,
    val depressionRemaining: Int,
    val anxietyRemaining: Int,
    val canTakeDepression: Boolean,
    val canTakeAnxiety: Boolean
)

data class TrialUseResponse(
    val success: Boolean,
    val message: String,
    val trialsRemaining: Int
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val assessmentId: String
)

data class AssessmentHistoryItem(
    val id: Int,
    val createdAt: String,
    val phqScore: Int?,
    val aiScore: Int?,
    val videoCount: Int
)

data class DeleteResponse(
    val success: Boolean,
    val message: String
)

data class DeleteAccountResponse(
    val success: Boolean,
    val message: String,
    val assessmentsDeleted: Int,
    val videosDeleted: Int
)

data class EmailReportResponse(
    val success: Boolean,
    val message: String
)