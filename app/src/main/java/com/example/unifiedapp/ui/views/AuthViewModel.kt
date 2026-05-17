package com.example.unifiedapp.ui.views

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.unifiedapp.ui.remote.ApiClient
import com.example.unifiedapp.ui.repository.RegisterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.unifiedapp.ui.views.UseTrialRequest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import com.google.gson.Gson
import com.google.gson.GsonBuilder

// ── Shared sealed states ──────────────────────────────────────────────────────
sealed class AssessmentListState {
    object Idle : AssessmentListState()
    object Loading : AssessmentListState()
    data class Success(val assessments: List<Assessment_Data>) : AssessmentListState()
    data class Error(val message: String) : AssessmentListState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val message: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

// ── Trial state for anxiety assessments ─────────────────────────────────────
sealed class TrialState {
    object Idle : TrialState()
    object Loading : TrialState()
    data class CanProceed(val remaining: Int, val total: Int) : TrialState()
    data class Blocked(val message: String) : TrialState()
    data class Error(val message: String) : TrialState()
}

// ── OTP States (KEPT ONLY for password reset) ───────────────────────────────
sealed class OtpState {
    object Idle : OtpState()
    object Loading : OtpState()
    data class OtpSent(val verificationId: String, val message: String) : OtpState()
    data class Verified(val message: String) : OtpState()
    data class Error(val message: String) : OtpState()
}

// ── Password Reset States ───────────────────────────────────────────────────
sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    data class OtpSent(val verificationId: String) : PasswordResetState()
    data class Success(val message: String) : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

// ── Registration Flow State (SIMPLIFIED - no phone verification) ─────────────
sealed class RegistrationFlowState {
    object Idle : RegistrationFlowState()
    data class Error(val message: String) : RegistrationFlowState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val prefs = UserPreferences(app)
    private val repository = RegisterRepository(ApiClient.authApi)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Login State
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // Register State
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    val registerResult = MutableLiveData<String>()

    // Registration Flow State (Simplified)
    private val _registrationFlowState = MutableStateFlow<RegistrationFlowState>(RegistrationFlowState.Idle)
    val registrationFlowState: StateFlow<RegistrationFlowState> = _registrationFlowState.asStateFlow()

    // Assessment list states
    private val _assessmentListState = MutableStateFlow<AssessmentListState>(AssessmentListState.Idle)
    val assessmentListState: StateFlow<AssessmentListState> = _assessmentListState.asStateFlow()

    private val _assessments = MutableStateFlow<List<Assessment_Data>>(emptyList())
    val assessments: StateFlow<List<Assessment_Data>> = _assessments.asStateFlow()

    // Delete states
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _deleteAllState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteAllState: StateFlow<DeleteState> = _deleteAllState.asStateFlow()

    // Trial state (anxiety only)
    private val _trialState = MutableStateFlow<TrialState>(TrialState.Idle)
    val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

    // OTP State (Kept for password reset only)
    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    // Password Reset State
    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    // Flag to track if this is just a count check (not user-initiated assessment)
    private var isJustCheckingCount = false

    init {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            val isLoggedIn = userData.isLoggedIn
            Log.d("AUTH_DEBUG", "Init - User logged in: $isLoggedIn")

            if (isLoggedIn) {
                prefs.registrationId.collect { id ->
                    Log.d("AUTH_DEBUG", "Init - registrationId collected = '$id'")
                    if (!id.isNullOrBlank()) {
                        Log.d("AUTH_DEBUG", "Init - Calling loadAssessments with id = $id")
                        loadAssessments(id)
                    } else {
                        Log.d("AUTH_DEBUG", "Init - registrationId is null or blank even though user is logged in")
                    }
                }
            } else {
                Log.d("AUTH_DEBUG", "Init - User not logged in yet - skipping assessment load")
            }
        }
    }

    fun loadAssessmentsIfLoggedIn() {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            val isLoggedIn = userData.isLoggedIn
            val id = prefs.registrationId.first()
            Log.d("AUTH_DEBUG", "loadAssessmentsIfLoggedIn - isLoggedIn: $isLoggedIn, id: $id")
            if (isLoggedIn && !id.isNullOrBlank()) {
                Log.d("AUTH_DEBUG", "User is logged in, loading assessments for id: $id")
                loadAssessments(id)
            } else {
                Log.d("AUTH_DEBUG", "User not logged in or no ID - skipping assessment load")
            }
        }
    }

    fun reloadAssessments() {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            val isLoggedIn = userData.isLoggedIn
            val id = prefs.registrationId.first()
            if (isLoggedIn && !id.isNullOrBlank()) {
                loadAssessments(id)
            } else {
                Log.d("AUTH_DEBUG", "reloadAssessments - User not logged in")
                _assessmentListState.value = AssessmentListState.Idle
            }
        }
    }

    fun loadAssessments(studentId: String) {
        Log.d("AUTH_DEBUG", "loadAssessments called with studentId = '$studentId'")
        viewModelScope.launch {
            _assessmentListState.value = AssessmentListState.Loading
            repository.getStudentAssessments(studentId)
                .onSuccess { response ->
                    Log.d("AUTH_DEBUG", "✅ API SUCCESS - total_assessments = ${response.totalAssessments}")
                    _assessments.value = response.assessments
                    _assessmentListState.value = AssessmentListState.Success(response.assessments)
                }
                .onFailure { error ->
                    Log.e("AUTH_DEBUG", "❌ API FAILED: ${error.message}")
                    _assessmentListState.value = AssessmentListState.Error(error.message ?: "Failed to load")
                }
        }
    }

    // ✅ SIMPLIFIED REGISTRATION - WITH ENHANCED LOGGING
    fun registerWithoutPhone(
        registrationId: String,
        password: String,
        name: String,
        gender: String,
        email: String,
        age: Int,
        roll_no: String
    ) {
        viewModelScope.launch {
            try {
                Log.d("AUTH_DEBUG", "========================================")
                Log.d("AUTH_DEBUG", "REGISTRATION START")
                Log.d("AUTH_DEBUG", "========================================")
                Log.d("AUTH_DEBUG", "registrationId: '$registrationId'")
                Log.d("AUTH_DEBUG", "name: '$name'")
                Log.d("AUTH_DEBUG", "gender: '$gender'")
                Log.d("AUTH_DEBUG", "email: '$email'")
                Log.d("AUTH_DEBUG", "age: $age")
                Log.d("AUTH_DEBUG", "roll_no: '$roll_no'")
                Log.d("AUTH_DEBUG", "password length: ${password.length}")

                _registerState.value = RegisterState.Loading

                val request = RegisterRequest(
                    registration_id = registrationId,
                    password = password,
                    name = name,
                    gender = gender,
                    email = email,
                    age = age,
                    roll_no = roll_no,
                    is_underage = false,
                    parent_name = null,
                    parent_email = null,
                    phone_no = null
                )

                // Print full JSON request
                val jsonRequest = gson.toJson(request)
                Log.d("AUTH_DEBUG", "========================================")
                Log.d("AUTH_DEBUG", "REQUEST JSON:")
                Log.d("AUTH_DEBUG", jsonRequest)
                Log.d("AUTH_DEBUG", "========================================")

                // Log the API endpoint being called
                Log.d("AUTH_DEBUG", "Calling API endpoint: /register-user")
                Log.d("AUTH_DEBUG", "Base URL: ${ApiClient.BASE_URL}")

                val response = ApiClient.authApi.register(request)

                Log.d("AUTH_DEBUG", "========================================")
                Log.d("AUTH_DEBUG", "RESPONSE RECEIVED")
                Log.d("AUTH_DEBUG", "Response code: ${response.code()}")
                Log.d("AUTH_DEBUG", "Response message: ${response.message()}")
                Log.d("AUTH_DEBUG", "========================================")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("AUTH_DEBUG", "✅ Response successful!")
                    Log.d("AUTH_DEBUG", "Response body: ${gson.toJson(body)}")

                    if (body != null) {
                        Log.d("AUTH_DEBUG", "✅ User registration successful")
                        Log.d("AUTH_DEBUG", "User ID: ${body.user_id}")
                        Log.d("AUTH_DEBUG", "Registration ID: ${body.registration_id}")
                        Log.d("AUTH_DEBUG", "Message: ${body.message}")

                        val userId = body.user_id ?: registrationId

                        prefs.login(
                            name = name,
                            email = email,
                            gender = gender,
                            id = registrationId,
                            age = age,
                            token = "dummy_token",
                            userId = userId,
                            isUnderage = false,
                            parentName = "",
                            parentEmail = "",
                            phoneNumber = null
                        )

                        val savedId = prefs.registrationId.first()
                        Log.d("AUTH_DEBUG", "✅ Saved registrationId after registration: $savedId")
                        loadAssessmentsIfLoggedIn()

                        _registerState.value = RegisterState.Success(body.message)
                        registerResult.value = body.message

                        resetRegistrationFlow()
                    } else {
                        Log.e("AUTH_DEBUG", "❌ Response body is null")
                        _registerState.value = RegisterState.Error("Empty response from server")
                        registerResult.value = "Empty response from server"
                    }
                } else {
                    Log.e("AUTH_DEBUG", "❌ Registration failed with code: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("AUTH_DEBUG", "Error body raw: $errorBody")

                    // Try to parse the error as JSON
                    var parsedErrorMsg = "Invalid data. Please check all fields."
                    try {
                        if (!errorBody.isNullOrBlank()) {
                            val errorJson = gson.fromJson(errorBody, Map::class.java)
                            parsedErrorMsg = errorJson["detail"]?.toString()
                                ?: errorJson["message"]?.toString()
                                        ?: errorBody
                            Log.d("AUTH_DEBUG", "Parsed error message: $parsedErrorMsg")
                        }
                    } catch (e: Exception) {
                        Log.e("AUTH_DEBUG", "Failed to parse error JSON: ${e.message}")
                        parsedErrorMsg = errorBody ?: "Invalid data. Please check all fields."
                    }

                    val errorMsg = when (response.code()) {
                        400 -> {
                            if (parsedErrorMsg.contains("already exists", ignoreCase = true)) {
                                "User already exists. Please login or use different Registration ID."
                            } else if (parsedErrorMsg.contains("18", ignoreCase = true)) {
                                "You must be 18 years or older to register."
                            } else {
                                parsedErrorMsg
                            }
                        }
                        422 -> "Invalid data format. Please check all fields."
                        409 -> "Registration ID already taken"
                        429 -> "Too many attempts. Please try again later."
                        500 -> "Server error. Please try again later."
                        else -> parsedErrorMsg
                    }

                    Log.e("AUTH_DEBUG", "Final error message: $errorMsg")
                    _registerState.value = RegisterState.Error(errorMsg)
                    registerResult.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "❌ Registration exception: ${e.message}", e)
                e.stackTrace.forEach { Log.e("AUTH_DEBUG", "    at ${it}") }
                _registerState.value = RegisterState.Error(e.message ?: "Network error. Please check your connection.")
                registerResult.value = e.message
            }
        }
    }

    // ✅ NEW METHOD: Only fetch trial count without any side effects
    fun fetchTrialCountOnly() {
        viewModelScope.launch {
            isJustCheckingCount = true
            checkAnxietyTrials()
        }
    }

    // ✅ MODIFIED METHOD: Check trials with flag to prevent auto-navigation
    fun checkAnxietyTrials() {
        viewModelScope.launch {
            _trialState.value = TrialState.Loading
            val registrationId = withTimeoutOrNull(2000L) {
                prefs.registrationId.first { it != null }
            }
            if (registrationId.isNullOrBlank()) {
                if (!isJustCheckingCount) {
                    _trialState.value = TrialState.Error("User not logged in (missing registration ID)")
                }
                isJustCheckingCount = false
                return@launch
            }
            try {
                val response = ApiClient.authApi.checkTrials(registrationId, "anxiety")
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.can_proceed) {
                        _trialState.value = TrialState.CanProceed(body.trials_remaining, body.total_trials)
                    } else {
                        val message = body.message ?: "No trials remaining. Please contact support."
                        _trialState.value = TrialState.Blocked(message)
                    }
                } else {
                    if (!isJustCheckingCount) {
                        _trialState.value = TrialState.Error("Failed to check trials: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TRIAL", "checkAnxietyTrials error", e)
                if (!isJustCheckingCount) {
                    _trialState.value = TrialState.Error(e.message ?: "Network error")
                }
            } finally {
                isJustCheckingCount = false
            }
        }
    }

    // Keep the old method for backward compatibility if needed
    fun fetchTrialStatus() {
        fetchTrialCountOnly()
    }

    fun decrementAnxietyTrial(assessmentId: String? = null) {
        viewModelScope.launch {
            val registrationId = prefs.registrationId.first() ?: return@launch
            try {
                val request = UseTrialRequest(registrationId, "anxiety", assessmentId)
                ApiClient.authApi.useTrial(request)
            } catch (e: Exception) {
                Log.e("TRIAL", "decrementAnxietyTrial error", e)
            }
        }
    }

    fun resetTrialState() {
        _trialState.value = TrialState.Idle
    }

    fun deleteAssessment(assessmentId: Int) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            val result: Result<Unit> = repository.deleteSpecificAssessment(assessmentId)
            result
                .onSuccess {
                    _assessments.value = _assessments.value.filter { it.id != assessmentId }
                    _deleteState.value = DeleteState.Success("Assessment deleted")
                }
                .onFailure { error ->
                    _deleteState.value = DeleteState.Error(error.message ?: "Delete failed")
                }
        }
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            val registrationId = prefs.registrationId.first()
            Log.d("AUTH_DEBUG", "deleteAllUserData - registrationId: $registrationId")
            if (registrationId.isNullOrBlank()) {
                _deleteAllState.value = DeleteState.Error("No registration ID found")
                return@launch
            }
            _deleteAllState.value = DeleteState.Loading
            repository.deleteAllUserData(registrationId)
                .onSuccess { response ->
                    Log.d("AUTH_DEBUG", "✅ Delete all success: ${response.message}")
                    _deleteAllState.value = DeleteState.Success(response.message)
                    logout()
                }
                .onFailure { error ->
                    Log.e("AUTH_DEBUG", "❌ Delete all failed: ${error.message}")
                    _deleteAllState.value = DeleteState.Error(error.message ?: "Delete failed")
                }
        }
    }

    fun resetDeleteAllState() { _deleteAllState.value = DeleteState.Idle }
    fun resetDeleteState() { _deleteState.value = DeleteState.Idle }
    fun clearRegisterState() { _registerState.value = RegisterState.Idle }
    fun resetOtpState() { _otpState.value = OtpState.Idle }
    fun resetPasswordResetState() { _passwordResetState.value = PasswordResetState.Idle }

    fun resetRegistrationFlow() {
        _registrationFlowState.value = RegistrationFlowState.Idle
    }

    // ── PASSWORD RESET METHODS (Kept for forgot password) ─────────────────────

    suspend fun forgotPassword(phoneNumber: String): Response<SendOtpResponse> {
        return try {
            _passwordResetState.value = PasswordResetState.Loading
            val response = ApiClient.authApi.forgotPassword(ForgotPasswordRequest(phoneNumber))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                _passwordResetState.value = PasswordResetState.OtpSent(
                    verificationId = body.verification_id ?: ""
                )
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Phone number not registered"
                    400 -> "Invalid phone number"
                    else -> "Failed to send OTP for password reset"
                }
                _passwordResetState.value = PasswordResetState.Error(errorMsg)
            }
            response
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "Forgot password error: ${e.message}", e)
            _passwordResetState.value = PasswordResetState.Error(e.message ?: "Network error")
            throw e
        }
    }

    suspend fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String): Response<ResetPasswordResponse> {
        return try {
            _passwordResetState.value = PasswordResetState.Loading
            val response = ApiClient.authApi.resetPassword(
                ResetPasswordRequest(phoneNumber, otpCode, newPassword)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                _passwordResetState.value = PasswordResetState.Success(body.message)
            } else {
                _passwordResetState.value = PasswordResetState.Error(
                    response.errorBody()?.string() ?: "Failed to reset password"
                )
            }
            response
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "Reset password error: ${e.message}", e)
            _passwordResetState.value = PasswordResetState.Error(e.message ?: "Password reset failed")
            throw e
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    fun login(id: String, password: String) {
        Log.d("AUTH_DEBUG", "========== LOGIN START ==========")
        Log.d("AUTH_DEBUG", "login() called with id: $id")
        _loginState.value = LoginState.Loading

        ApiClient.authApi.login(LoginRequest(id, password))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    Log.d("AUTH_DEBUG", "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("AUTH_DEBUG", "Response body: $body")

                        if (body != null) {
                            viewModelScope.launch {
                                val parentName = body.parent?.parent_name ?: ""
                                val parentEmail = body.parent?.parent_email ?: ""
                                val isVerified = body.parent?.is_verified ?: false
                                val isUnderage = body.age != null && body.age!! < 18

                                Log.d("AUTH_DEBUG", "========== EXTRACTED VALUES ==========")
                                Log.d("AUTH_DEBUG", "Name: ${body.name}")
                                Log.d("AUTH_DEBUG", "Registration ID: ${body.registration_id}")
                                Log.d("AUTH_DEBUG", "Age: ${body.age}")
                                Log.d("AUTH_DEBUG", "Is Underage: $isUnderage")
                                Log.d("AUTH_DEBUG", "Phone Number: ${body.phone_no}")
                                Log.d("AUTH_DEBUG", "Parent Name: '$parentName'")
                                Log.d("AUTH_DEBUG", "Parent Email: '$parentEmail'")
                                Log.d("AUTH_DEBUG", "Parent Verified: $isVerified")

                                prefs.login(
                                    name = body.name,
                                    email = body.email ?: "",
                                    gender = body.gender ?: "",
                                    id = body.registration_id,
                                    age = body.age ?: 0,
                                    token = body.token ?: "dummy_token",
                                    userId = body.user_id,
                                    isUnderage = isUnderage,
                                    parentName = parentName,
                                    parentEmail = parentEmail,
                                    phoneNumber = body.phone_no ?: ""
                                )

                                delay(500)
                                val savedData = prefs.getCurrentUser()
                                Log.d("AUTH_DEBUG", "Saved isUnderage: ${savedData.isUnderage}")
                                Log.d("AUTH_DEBUG", "Saved parentName: '${savedData.parentName}'")
                                Log.d("AUTH_DEBUG", "Saved parentEmail: '${savedData.parentEmail}'")
                                Log.d("AUTH_DEBUG", "Saved phoneNumber: '${savedData.phoneNumber}'")

                                loadAssessmentsIfLoggedIn()
                                _loginState.value = LoginState.Success

                                Log.d("AUTH_DEBUG", "========== LOGIN COMPLETE ==========")
                            }
                        } else {
                            Log.e("AUTH_DEBUG", "❌ Login failed - empty body")
                            _loginState.value = LoginState.Error("Invalid response from server")
                        }
                    } else {
                        Log.e("AUTH_DEBUG", "❌ Login failed: ${response.code()} - ${response.message()}")
                        val errorBody = response.errorBody()?.string()
                        Log.e("AUTH_DEBUG", "Error body: $errorBody")

                        val errorMsg = when (response.code()) {
                            401 -> "Invalid credentials"
                            404 -> "User not found"
                            403 -> "Account locked. Please contact support."
                            429 -> "Too many attempts. Please try again later."
                            500 -> "Server error"
                            else -> "Login failed: ${response.message()}"
                        }
                        _loginState.value = LoginState.Error(errorMsg)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("AUTH_DEBUG", "❌ Network error: ${t.message}", t)
                    _loginState.value = LoginState.Error("Network error: ${t.message}")
                }
            })
    }

    fun logout() {
        viewModelScope.launch {
            Log.d("AUTH_DEBUG", "Logging out user")
            prefs.logout()
            _assessments.value = emptyList()
            _assessmentListState.value = AssessmentListState.Idle
            _loginState.value = LoginState.Idle
            _otpState.value = OtpState.Idle
            _passwordResetState.value = PasswordResetState.Idle
            resetTrialState()
            resetRegistrationFlow()
        }
    }

    fun getPrefs() = prefs
}