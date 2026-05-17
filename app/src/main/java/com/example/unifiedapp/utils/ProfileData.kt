package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.io.OutputStreamWriter

// Add these missing imports for the StatusMessage composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// In any screen file, use this import:
import com.example.unifiedapp.utils.UserSessionHelper

// ============ DATA CLASSES ============

data class UserProfile(
    val name: String,
    val gender: String,
    val email: String,
    val age: Int,
    val registration_id: String,
    val anonymousId: String = ""
)

data class UserRegister(
    val registration_id: String,
    val password: String,
    val name: String,
    val gender: String,
    val email: String,
    val age: Int
)

data class UserLogin(
    val registration_id: String,
    val password: String
)

data class AssessmentHistoryItem(
    val id: Int,
    val assessmentType: String,
    val phqScore: Int?,
    val phq8Score: Int?,
    val aiScore: Int?,
    val aiConfidence: Float?,
    val aiPrediction: String?,
    val createdAt: String,
    val videoCount: Int,
    val files: AssessmentFiles?
)

data class AssessmentFiles(
    val videoPath: String?,
    val auCsvPath: String?,
    val phq9CsvPath: String?
)

data class DeleteResponse(
    val success: Boolean,
    val message: String,
    val assessmentId: Int? = null
)

data class DeleteAccountResponse(
    val success: Boolean,
    val message: String,
    val deletedSummary: DeleteSummary? = null
)

data class DeleteSummary(
    val studentId: String? = null,
    val assessmentsDeleted: Int = 0,
    val videosDeleted: Int = 0,
    val filesDeleted: Int = 0
)

enum class AuthMode { LOGIN, SIGNUP }

// ============ USER SESSION MANAGEMENT ============



// ============ VALIDATION FUNCTIONS ============

fun validateRegistrationId(regId: String): String? {
    return when {
        regId.isBlank() -> "Registration ID is required"
        regId.contains(" ") -> "Registration ID cannot contain spaces"
        else -> null
    }
}

fun validatePassword(password: String): String? {
    return when {
        password.isBlank() -> "Password is required"
        password.length < 6 -> "Password must be at least 6 characters"
        password.contains(" ") -> "Password cannot contain spaces"
        else -> null
    }
}

fun validateName(name: String): String? {
    return when {
        name.isBlank() -> "Name is required"
        name.length < 2 -> "Name must be at least 2 characters"
        else -> null
    }
}

fun validateEmail(email: String): String? {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return when {
        email.isBlank() -> "Email is required"
        email.contains(" ") -> "Email cannot contain spaces"
        !emailRegex.matches(email) -> "Enter a valid email address"
        else -> null
    }
}

fun validateAge(age: String): String? {
    val ageInt = age.toIntOrNull()
    return when {
        age.isBlank() -> "Age is required"
        ageInt == null -> "Please enter a valid number"
        ageInt < 18 -> "You must be 18 or older"
        ageInt > 120 -> "Please enter a valid age"
        else -> null
    }
}

fun validateConfirmPassword(password: String, confirmPassword: String): String? {
    return when {
        confirmPassword.isBlank() -> "Please confirm your password"
        password != confirmPassword -> "Passwords don't match"
        else -> null
    }
}

// ============ HELPER FUNCTIONS ============

fun loadSavedUser(context: Context): UserProfile? {
    val session = UserSessionHelper.getUserData(context)
    return if (session.isLoggedIn) {
        UserProfile(
            name = session.name,
            gender = session.gender,
            email = session.email,
            age = session.age,
            registration_id = session.registrationId,
            anonymousId = session.anonymousId
        )
    } else {
        null
    }
}

fun saveUser(context: Context, profile: UserProfile) {
    UserSessionHelper.saveUserData(
        context,
        UserSessionHelper.UserData(
            name = profile.name,
            gender = profile.gender,
            email = profile.email,
            age = profile.age,
            registrationId = profile.registration_id,
            anonymousId = profile.anonymousId,
            isLoggedIn = true
        )
    )
}

fun logoutUser(context: Context) {
    UserSessionHelper.logout(context)  // This exists!
}

fun generateAnonymousId(name: String, rollNo: String): String {
    return try {
        val input = "${name.lowercase(Locale.getDefault()).trim()}_${rollNo.lowercase(Locale.getDefault()).trim()}"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray())
        val hexString = hash.joinToString("") { "%02x".format(it) }
        "STU_${hexString.take(8).uppercase(Locale.getDefault())}"
    } catch (e: Exception) {
        "STU_${System.currentTimeMillis().toString().takeLast(8)}"
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val date = inputFormat.parse(dateString)
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}

// ============ API FUNCTIONS ============

suspend fun deleteUserAccount(
    registrationId: String
): DeleteAccountResponse {
    return try {
        Log.d("PrivacyData", "Deleting account for: $registrationId")
        val url = URL("http://203.110.243.202:8000/api/student/delete-all/${registrationId}")
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

        return when (responseCode) {
            in 200..299 -> {
                val json = try {
                    JSONObject(response)
                } catch (e: Exception) {
                    JSONObject()
                }

                val summary = if (json.has("deleted_summary")) {
                    val summaryJson = json.getJSONObject("deleted_summary")
                    DeleteSummary(
                        studentId = summaryJson.optString("student_id", registrationId),
                        assessmentsDeleted = summaryJson.optInt("assessments_deleted", 0),
                        videosDeleted = summaryJson.optInt("videos_deleted", 0),
                        filesDeleted = summaryJson.optInt("files_deleted", 0)
                    )
                } else {
                    DeleteSummary()
                }

                DeleteAccountResponse(
                    success = true,
                    message = json.optString("message", "Account deleted successfully"),
                    deletedSummary = summary
                )
            }
            404 -> DeleteAccountResponse(success = false, message = "User not found. You may have been already deleted.")
            401 -> DeleteAccountResponse(success = false, message = "Unauthorized. Please login again.")
            else -> {
                val errorMsg = try {
                    JSONObject(response).optString("detail", "Failed to delete account (Code: $responseCode)")
                } catch (e: Exception) {
                    "Server error (code: $responseCode)"
                }
                DeleteAccountResponse(success = false, message = errorMsg)
            }
        }
    } catch (e: java.net.ConnectException) {
        DeleteAccountResponse(success = false, message = "Cannot connect to server. Check your internet connection.")
    } catch (e: java.net.SocketTimeoutException) {
        DeleteAccountResponse(success = false, message = "Connection timeout. Server is not responding.")
    } catch (e: Exception) {
        DeleteAccountResponse(success = false, message = "Connection failed: ${e.message}")
    }
}

suspend fun fetchAssessmentHistory(registrationId: String): List<AssessmentHistoryItem> {
    return try {
        val url = URL("http://203.110.243.202:8000/api/student/${registrationId}/assessments")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        val response = if (responseCode == 200) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        connection.disconnect()

        // ✅ LOG the raw response (important for debugging)
        Log.d("AssessmentHistory", "Response code: $responseCode")
        Log.d("AssessmentHistory", "Raw response: $response")

        if (responseCode == 200) {
            val json = JSONObject(response)
            val assessmentsArray = json.getJSONArray("assessments")
            val count = assessmentsArray.length()
            Log.d("AssessmentHistory", "Number of assessments in response: $count")

            List(count) { index ->
                val item = assessmentsArray.getJSONObject(index)

                val id = item.getInt("id")
                val createdAt = item.getString("created_at")
                val videoCount = item.getInt("video_count")

                val phqScore = if (item.has("phq_score") && !item.isNull("phq_score")) {
                    item.getInt("phq_score")
                } else null

                val phq8Score = if (item.has("phq8_score") && !item.isNull("phq8_score")) {
                    item.getInt("phq8_score")
                } else null

                val aiScore = if (item.has("ai_score") && !item.isNull("ai_score")) {
                    item.getInt("ai_score")
                } else null

                val aiConfidence = if (item.has("ai_confidence") && !item.isNull("ai_confidence")) {
                    item.getDouble("ai_confidence").toFloat()
                } else null

                val aiPrediction = if (item.has("ai_prediction") && !item.isNull("ai_prediction")) {
                    item.getString("ai_prediction")
                } else null

                val files = if (item.has("files") && !item.isNull("files")) {
                    val filesObj = item.getJSONObject("files")
                    AssessmentFiles(
                        videoPath = if (filesObj.has("video_path")) filesObj.getString("video_path") else null,
                        auCsvPath = if (filesObj.has("au_csv_path")) filesObj.getString("au_csv_path") else null,
                        phq9CsvPath = if (filesObj.has("phq9_csv_path")) filesObj.getString("phq9_csv_path") else null
                    )
                } else null

                AssessmentHistoryItem(
                    id = id,
                    assessmentType = "Mental Health Assessment",
                    phqScore = phqScore,
                    phq8Score = phq8Score,
                    aiScore = aiScore,
                    aiConfidence = aiConfidence,
                    aiPrediction = aiPrediction,
                    createdAt = createdAt,
                    videoCount = videoCount,
                    files = files
                )
            }
        } else {
            Log.e("AssessmentHistory", "Non-200 response code: $responseCode")
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("AssessmentHistory", "Exception in fetchAssessmentHistory", e)
        emptyList()
    }
}


suspend fun deleteSingleAssessment(assessmentId: Int): DeleteResponse {
    return try {
        val url = URL("http://203.110.243.202:8000/api/assessment/${assessmentId}")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode

        connection.disconnect()

        if (responseCode in 200..299) {
            DeleteResponse(
                success = true,
                message = "Assessment deleted successfully",
                assessmentId = assessmentId
            )
        } else {
            DeleteResponse(
                success = false,
                message = "Failed to delete assessment (Code: $responseCode)",
                assessmentId = assessmentId
            )
        }
    } catch (e: Exception) {
        DeleteResponse(
            success = false,
            message = "Connection failed: ${e.message}",
            assessmentId = assessmentId
        )
    }
}

suspend fun performLogin(
    context: Context,
    registrationId: String,
    password: String
): Pair<UserProfile?, String?> {
    try {
        val url = URL("http://203.110.243.202:8000/login-user")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

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
            try {
                val json = JSONObject(response)
                val name = json.getString("name")
                val gender = json.getString("gender")
                val email = json.getString("email")
                val age = json.getInt("age")

                val serverAnonymousId = if (json.has("anonymous_id")) {
                    json.getString("anonymous_id")
                } else {
                    ""
                }

                val profile = UserProfile(
                    name = name,
                    gender = gender,
                    email = email,
                    age = age,
                    registration_id = registrationId,
                    anonymousId = serverAnonymousId
                )

                return Pair(profile, null)

            } catch (e: Exception) {
                return Pair(null, "Server error")
            }
        } else if (responseCode == 401) {
            return Pair(null, "Invalid registration ID or password")
        } else {
            return Pair(null, "Login failed. Please try again.")
        }
    } catch (e: java.net.ConnectException) {
        return Pair(null, "Cannot connect to server")
    } catch (e: java.net.SocketTimeoutException) {
        return Pair(null, "Connection timeout")
    } catch (e: Exception) {
        return Pair(null, "Connection failed")
    }
}

suspend fun performSignup(
    registrationId: String,
    password: String,
    name: String,
    gender: String,
    email: String,
    age: Int
): Pair<UserProfile?, String?> {
    try {
        val url = URL("http://203.110.243.202:8000/register-user")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val requestBody = JSONObject().apply {
            put("registration_id", registrationId)
            put("password", password)
            put("name", name)
            put("gender", gender)
            put("email", email)
            put("age", age)
            put("roll_no", registrationId)
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
            val profile = UserProfile(
                name = name,
                gender = gender,
                email = email,
                age = age,
                registration_id = registrationId,
                anonymousId = ""
            )
            return Pair(profile, null)
        } else if (responseCode == 400) {
            if (response.contains("already exists", ignoreCase = true)) {
                return Pair(null, "Registration ID already exists")
            } else {
                return Pair(null, "Registration failed")
            }
        } else {
            return Pair(null, "Registration failed. Please try again.")
        }

    } catch (e: java.net.ConnectException) {
        return Pair(null, "Cannot connect to server")
    } catch (e: java.net.SocketTimeoutException) {
        return Pair(null, "Connection timeout")
    } catch (e: Exception) {
        return Pair(null, "Connection failed")
    }
}

// ============ STATUS MESSAGE COMPOSABLE ============
// Define colors locally to avoid dependency on theme
private val ColorError = Color(0xFFEF4444)
private val ColorSuccess = Color(0xFF10B981)

@Composable
fun StatusMessage(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isError)
            ColorError.copy(alpha = 0.1f)
        else
            ColorSuccess.copy(alpha = 0.1f),
        border = BorderStroke(
            1.dp,
            if (isError)
                ColorError.copy(alpha = 0.3f)
            else
                ColorSuccess.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) ColorError else ColorSuccess,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                message,
                modifier = Modifier.weight(1f),
                color = if (isError) ColorError else ColorSuccess,
                fontSize = 13.sp
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = if (isError) ColorError else ColorSuccess,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}