package com.example.unifiedapp.ui.views

import android.annotation.SuppressLint
import android.content.Context
import com.example.unifiedapp.ui.remote.ApiClient
import com.example.unifiedapp.ui.repository.EmailRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import com.example.unifiedapp.ui.remote.SimpleServerClient
import com.example.unifiedapp.ui.vision.FaceLandmarkerHelper.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

private var userAge: Int = 22
private var userRollNo: String = "DEMO123"

private var anonymousId: String = ""

private var lastVideoFilePath: String? = null
private var lastAuCsvPath: String? = null

private var assessmentType: String? = "anxiety"
private var lastGad7CsvPath: String? = null
private var lastBaiCsvPath: String? = null


data class AssessmentState(
    val isRecording: Boolean = false,
    val sessionId: String = "",
    val frameCount: Int = 0,
    val anxietyPrediction: String = "Pending", // Stores AI result (e.g., "High", "Low")
    val anxietyConfidence: Float = 0f,         // Stores AI confidence (0.0 - 1.0)
    val anxietyScore: Int = 0                  // Stores the raw score (0-21) if your model provides it
)


data class QuestionnaireResponse(
    val selectedOption: Int,  // Index of selected option (0-3)
    val score: Int,           // Score for this question (0-3)
    val timestamp: Long       // Timestamp when answered
)

// Data class to hold the AI results
data class QuizUiState(
    val anxietyPrediction: String = "No Data",
    val anxietyScore: Int = 0,
    val anxietyConfidence: Int = 0,
    val serverStatus: String = "Unknown"
)

class QuizResultViewModel(
    @SuppressLint("StaticFieldLeak") private val context: Context,
    private val repository: EmailRepository = EmailRepository(),

    ) : ViewModel() {

    private val serverClient by lazy { SimpleServerClient(context) }
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
    var isServerEnabled: Boolean = false
    private val _status = MutableStateFlow<EmailStatus>(EmailStatus.Idle)
    val status: StateFlow<EmailStatus> = _status

    private val userPreferences = UserPreferences(context)

    val userData: StateFlow<UserData> = userPreferences.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserData(
                isLoggedIn = false,
                name = "",
                email = "",
                gender = "",
                age = 0,
                id = "",
                TOKEN = ""
            )
        )

    // Call this from your Assessment logic before navigating to results
    fun setResults(prediction: String, score: Int, confidence: Int) {
        _uiState.value = QuizUiState(
            prediction, score, confidence,
            serverStatus =isServerEnabled.toString(),
        )
    }

    private fun registerAnonymousUser() {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    serverClient.registerAnonymousUser(anonymousId, userAge, "anxiety")
                }

                if (success) {
                    Log.d(TAG, "Anonymous user registered successfully: $anonymousId")
                } else {
                    Log.w(TAG, "Anonymous registration failed, continuing with local assessment")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Anonymous registration error: ${e.message}")
            }
        }
    }

    fun initializeServerClient() {
        viewModelScope.launch {
            val isConnected = withContext(Dispatchers.IO) {
                serverClient.checkServerConnection()
            }
            isServerEnabled = isConnected
            val statusText = if (isConnected) "Connected" else "Offline"
            _uiState.value = _uiState.value.copy(serverStatus = statusText)

            // Register anonymous user if ID exists
            if (isConnected && anonymousId.isNotEmpty()) {
                registerAnonymousUser()
            }
        }
    }

    fun sendEmail(report: QuizReportDto) {
        _status.value = EmailStatus.Sending


        viewModelScope.launch {
            try {
                val assessmentData = AssessmentData(
                    anonymousId = anonymousId,
                    age = userAge,
                    email=report.email,
                    registrationId ="",
                    assessmentType = "Anxiety",
                    videoFile = lastVideoFilePath?.let { File(it) },
                    auCsvFile = lastAuCsvPath?.let { File(it) },
                    gad7CsvFile = lastGad7CsvPath?.let { File(it) },
                )

                withContext(Dispatchers.IO) {
                    serverClient.uploadAnonymousAssessment(assessmentData, object : SimpleServerClient.UploadCallback {
                        override fun onProgress(progress: Int, message: String) {

                            Log.d("SERVER","Upload in progress......}")
                        }
                        override fun onSuccess(message: String) {

                            Log.d("SERVER","successfully uplaoded}")
                        }
                        override fun onError(error: String) {

                            Log.d("SERVER","Failed to prepare upload: ${error}")
                        }
                    })
                }
            } catch (e: Exception) {

                Log.d("SERVER","Failed to prepare upload: ${e.message}")

            }
        }

        viewModelScope.launch {
            Log.d("EMAIL", "sendEmail() called with ${report.email}")
            try {
                // FIXED: Use 'ApiClient.emailApi' here
                val response = ApiClient.emailApi.sendQuizReport(report)

                if (response.isSuccessful) {
                    _status.value = EmailStatus.Success
                    println("Email sent successfully: ${response.code()}")
                } else {
                    _status.value = EmailStatus.Error
                    println("Server error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _status.value = EmailStatus.Error
                Log.e("EMAIL", "Error sending email", e)
                e.printStackTrace()
            }
        }
    }

}


sealed class EmailStatus {
    object Idle : EmailStatus()
    object Sending : EmailStatus()
    object Success : EmailStatus()
    object Error : EmailStatus()
}
