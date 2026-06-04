package com.example.unifiedapp.ui.views

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.unifiedapp.remote.ApiClient
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Shared sealed states (unchanged) ──────────────────────────────
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

sealed class TrialState {
    object Idle : TrialState()
    object Loading : TrialState()
    data class CanProceed(val remaining: Int, val total: Int) : TrialState()
    data class Blocked(val message: String) : TrialState()
    data class Error(val message: String) : TrialState()
}

sealed class OtpState {
    object Idle : OtpState()
    object Loading : OtpState()
    data class OtpSent(val verificationId: String, val message: String) : OtpState()
    data class Verified(val message: String) : OtpState()
    data class Error(val message: String) : OtpState()
}

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    data class OtpSent(val verificationId: String) : PasswordResetState()
    data class Success(val message: String) : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

sealed class RegistrationFlowState {
    object Idle : RegistrationFlowState()
    data class Error(val message: String) : RegistrationFlowState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    sealed class ClinicalSubmissionState {
        object Idle : ClinicalSubmissionState()
        object Loading : ClinicalSubmissionState()
        data class Success(val message: String, val score: Int, val severity: String) : ClinicalSubmissionState()
        data class Error(val message: String) : ClinicalSubmissionState()
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val prefs = UserPreferences(app)
    private val repository = RegisterRepository(ApiClient.authApi)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _clinicianLoginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val clinicianLoginState: StateFlow<LoginState> = _clinicianLoginState.asStateFlow()

    private val _patients = MutableStateFlow<List<PatientItem>>(emptyList())
    val patients: StateFlow<List<PatientItem>> = _patients.asStateFlow()

    private val _submissionState = MutableStateFlow<ClinicalSubmissionState>(ClinicalSubmissionState.Idle)
    val submissionState: StateFlow<ClinicalSubmissionState> = _submissionState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    val registerResult = MutableLiveData<String>()

    private val _registrationFlowState = MutableStateFlow<RegistrationFlowState>(RegistrationFlowState.Idle)
    val registrationFlowState: StateFlow<RegistrationFlowState> = _registrationFlowState.asStateFlow()

    private val _assessmentListState = MutableStateFlow<AssessmentListState>(AssessmentListState.Idle)
    val assessmentListState: StateFlow<AssessmentListState> = _assessmentListState.asStateFlow()
    private val _assessments = MutableStateFlow<List<Assessment_Data>>(emptyList())
    val assessments: StateFlow<List<Assessment_Data>> = _assessments.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()
    private val _deleteAllState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteAllState: StateFlow<DeleteState> = _deleteAllState.asStateFlow()

    private val _trialState = MutableStateFlow<TrialState>(TrialState.Idle)
    val trialState: StateFlow<TrialState> = _trialState.asStateFlow()
    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()
    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    private var isJustCheckingCount = false

    init {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            if (userData.isLoggedIn) {
                prefs.registrationId.collect { id ->
                    if (!id.isNullOrBlank()) loadAssessments(id)
                }
            }
        }
    }

    fun loadAssessmentsIfLoggedIn() {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            val id = prefs.registrationId.first()
            if (userData.isLoggedIn && !id.isNullOrBlank()) loadAssessments(id)
        }
    }

    fun reloadAssessments() {
        viewModelScope.launch {
            val userData = prefs.userData.first()
            val id = prefs.registrationId.first()
            if (userData.isLoggedIn && !id.isNullOrBlank()) loadAssessments(id)
            else _assessmentListState.value = AssessmentListState.Idle
        }
    }

    fun loadAssessments(studentId: String) {
        viewModelScope.launch {
            _assessmentListState.value = AssessmentListState.Loading
            repository.getStudentAssessments(studentId)
                .onSuccess { response ->
                    _assessments.value = response.assessments
                    _assessmentListState.value = AssessmentListState.Success(response.assessments)
                }
                .onFailure { error ->
                    _assessmentListState.value = AssessmentListState.Error(error.message ?: "Failed to load")
                }
        }
    }

    fun registerWithoutPhone(
        registrationId: String,
        password: String,
        name: String,
        gender: String,
        email: String,
        age: Int,
        rollNo: String
    ) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState.Loading
                val request = RegisterRequest(
                    registration_id = registrationId,
                    password = password,
                    name = name,
                    gender = gender,
                    email = email,
                    age = age,
                    roll_no = rollNo,
                    is_underage = false,
                    parent_name = null,
                    parent_email = null,
                    phone_no = null
                )
                val response = ApiClient.authApi.registerUser(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    prefs.login(
                        name = name,
                        email = email,
                        gender = gender,
                        id = registrationId,
                        age = age,
                        token = "dummy_token",
                        userId = body.user_id ?: registrationId,
                        isUnderage = false,
                        parentName = "",
                        parentEmail = "",
                        phoneNumber = null
                    )
                    loadAssessmentsIfLoggedIn()
                    _registerState.value = RegisterState.Success(body.message)
                    registerResult.value = body.message
                    resetRegistrationFlow()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _registerState.value = RegisterState.Error(errorMsg)
                    registerResult.value = errorMsg
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Network error")
                registerResult.value = e.message
            }
        }
    }

    fun fetchTrialCountOnly() {
        viewModelScope.launch {
            isJustCheckingCount = true
            checkAnxietyTrials()
        }
    }

    fun checkAnxietyTrials() {
        viewModelScope.launch {
            _trialState.value = TrialState.Loading
            val registrationId = withTimeoutOrNull(2000L) { prefs.registrationId.first { it != null } }
            if (registrationId.isNullOrBlank()) {
                if (!isJustCheckingCount) _trialState.value = TrialState.Error("User not logged in")
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
                        _trialState.value = TrialState.Blocked(body.message ?: "No trials remaining")
                    }
                } else {
                    if (!isJustCheckingCount) _trialState.value = TrialState.Error("Failed to check trials")
                }
            } catch (e: Exception) {
                if (!isJustCheckingCount) _trialState.value = TrialState.Error(e.message ?: "Network error")
            } finally {
                isJustCheckingCount = false
            }
        }
    }

    fun fetchTrialStatus() = fetchTrialCountOnly()

    fun decrementAnxietyTrial(assessmentId: String? = null) {
        viewModelScope.launch {
            val registrationId = prefs.registrationId.first() ?: return@launch
            try {
                ApiClient.authApi.useTrial(UseTrialRequest(registrationId, "anxiety", assessmentId))
            } catch (e: Exception) { Log.e("TRIAL", "decrementAnxietyTrial error", e) }
        }
    }

    fun decrementDepressionTrial(assessmentId: String? = null) {
        viewModelScope.launch {
            val registrationId = prefs.registrationId.first() ?: return@launch
            try {
                ApiClient.authApi.useTrial(UseTrialRequest(registrationId, "depression", assessmentId))
            } catch (e: Exception) { Log.e("TRIAL", "decrementDepressionTrial error", e) }
        }
    }

    fun resetTrialState() { _trialState.value = TrialState.Idle }

    fun deleteAssessment(assessmentId: Int) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            repository.deleteSpecificAssessment(assessmentId)
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
            if (registrationId.isNullOrBlank()) {
                _deleteAllState.value = DeleteState.Error("No registration ID found")
                return@launch
            }
            _deleteAllState.value = DeleteState.Loading
            repository.deleteAllUserData(registrationId)
                .onSuccess { response ->
                    _deleteAllState.value = DeleteState.Success(response.message)
                    logout()
                }
                .onFailure { error ->
                    _deleteAllState.value = DeleteState.Error(error.message ?: "Delete failed")
                }
        }
    }

    fun resetDeleteAllState() { _deleteAllState.value = DeleteState.Idle }
    fun resetDeleteState() { _deleteState.value = DeleteState.Idle }
    fun clearRegisterState() { _registerState.value = RegisterState.Idle }
    fun resetOtpState() { _otpState.value = OtpState.Idle }
    fun resetPasswordResetState() { _passwordResetState.value = PasswordResetState.Idle }
    fun resetRegistrationFlow() { _registrationFlowState.value = RegistrationFlowState.Idle }

    suspend fun forgotPassword(phoneNumber: String): Response<SendOtpResponse> {
        _passwordResetState.value = PasswordResetState.Loading
        return try {
            val response = ApiClient.authApi.forgotPassword(ForgotPasswordRequest(phoneNumber))
            if (response.isSuccessful && response.body() != null) {
                _passwordResetState.value = PasswordResetState.OtpSent(response.body()!!.verification_id ?: "")
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Phone number not registered"
                    400 -> "Invalid phone number"
                    else -> "Failed to send OTP"
                }
                _passwordResetState.value = PasswordResetState.Error(errorMsg)
            }
            response
        } catch (e: Exception) {
            _passwordResetState.value = PasswordResetState.Error(e.message ?: "Network error")
            throw e
        }
    }

    suspend fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String): Response<ResetPasswordResponse> {
        _passwordResetState.value = PasswordResetState.Loading
        return try {
            val response = ApiClient.authApi.resetPassword(ResetPasswordRequest(phoneNumber, otpCode, newPassword))
            if (response.isSuccessful && response.body() != null) {
                _passwordResetState.value = PasswordResetState.Success(response.body()!!.message)
            } else {
                _passwordResetState.value = PasswordResetState.Error(response.errorBody()?.string() ?: "Failed to reset password")
            }
            response
        } catch (e: Exception) {
            _passwordResetState.value = PasswordResetState.Error(e.message ?: "Network error")
            throw e
        }
    }

    // ✅ Patient login – uses correct field names from models.LoginResponse
    fun login(id: String, password: String) {
        _loginState.value = LoginState.Loading
        ApiClient.authApi.loginUser(LoginRequest(id, password))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        viewModelScope.launch {
                            val isUnderage = (body.age ?: 0) < 18
                            prefs.login(
                                name = body.name,
                                email = body.email ?: "",
                                gender = body.gender ?: "",
                                id = body.registration_id,
                                age = body.age ?: 0,
                                token = body.token ?: "dummy_token",
                                userId = body.user_id ?: body.registration_id,
                                isUnderage = isUnderage,
                                parentName = body.parent?.parent_name ?: "",
                                parentEmail = body.parent?.parent_email ?: "",
                                phoneNumber = body.phone_no
                            )
                            loadAssessmentsIfLoggedIn()
                            _loginState.value = LoginState.Success
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> "Invalid credentials"
                            404 -> "User not found"
                            else -> "Login failed"
                        }
                        _loginState.value = LoginState.Error(errorMsg)
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    _loginState.value = LoginState.Error("Network error: ${t.message}")
                }
            })
    }

    // ========== CLINICAL FUNCTIONS (with fixed clinician login) ==========
    fun loginClinician(registrationId: String, password: String) {
        _clinicianLoginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.loginClinician(registrationId, password)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    prefs.saveClinicianSession(
                        registrationId = body.registrationId,
                        name = body.name,
                        userId = body.userId,
                        token = body.token
                    )
                    _clinicianLoginState.value = LoginState.Success
                } else {
                    _clinicianLoginState.value = LoginState.Error(response.errorBody()?.string() ?: "Login failed")
                }
            } catch (e: Exception) {
                _clinicianLoginState.value = LoginState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetClinicianLoginState() {
        _clinicianLoginState.value = LoginState.Idle
    }

    fun fetchPatients(clinicianId: String, searchQuery: String? = null) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.getPatients(clinicianId, searchQuery)
                if (response.isSuccessful && response.body() != null) {
                    _patients.value = response.body()!!.patients
                } else {
                    _patients.value = emptyList()
                }
            } catch (e: Exception) {
                _patients.value = emptyList()
            }
        }
    }

    fun submitHamA(patientId: String, clinicianId: String, itemScores: List<Int>) {
        viewModelScope.launch {
            _submissionState.value = ClinicalSubmissionState.Loading
            try {
                val response = ApiClient.authApi.submitHamA(HamARequest(patientId, clinicianId, itemScores))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _submissionState.value = ClinicalSubmissionState.Success(
                        message = "HAM-A assessment saved",
                        score = body.totalScore,
                        severity = body.severity
                    )
                } else {
                    _submissionState.value = ClinicalSubmissionState.Error("Failed to submit HAM-A")
                }
            } catch (e: Exception) {
                _submissionState.value = ClinicalSubmissionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun submitHdrs(patientId: String, clinicianId: String, itemScores: List<Int>) {
        viewModelScope.launch {
            _submissionState.value = ClinicalSubmissionState.Loading
            try {
                val response = ApiClient.authApi.submitHdrs(HdrsRequest(patientId, clinicianId, itemScores))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _submissionState.value = ClinicalSubmissionState.Success(
                        message = "HDRS assessment saved",
                        score = body.totalScore,
                        severity = body.severity
                    )
                } else {
                    _submissionState.value = ClinicalSubmissionState.Error("Failed to submit HDRS")
                }
            } catch (e: Exception) {
                _submissionState.value = ClinicalSubmissionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = ClinicalSubmissionState.Idle
    }

    suspend fun getClinicalScores(patientId: String): ClinicalScoresResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.authApi.getClinicalScores(patientId)
                if (response.isSuccessful) response.body() else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private val _clinicianRegisterState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val clinicianRegisterState: StateFlow<RegisterState> = _clinicianRegisterState.asStateFlow()

    fun registerClinician(name: String, email: String, password: String) {
        viewModelScope.launch {
            _clinicianRegisterState.value = RegisterState.Loading
            try {
                val request = RegisterClinicianRequest(name, password, email)
                val response = ApiClient.authApi.registerClinician(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        _clinicianRegisterState.value = RegisterState.Success("Clinician registered! Please login.")
                    } else {
                        _clinicianRegisterState.value = RegisterState.Error(body.message ?: "Registration failed")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _clinicianRegisterState.value = RegisterState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _clinicianRegisterState.value = RegisterState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetClinicianRegisterState() {
        _clinicianRegisterState.value = RegisterState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            prefs.logout()
            _assessments.value = emptyList()
            _assessmentListState.value = AssessmentListState.Idle
            _loginState.value = LoginState.Idle
            _clinicianLoginState.value = LoginState.Idle
            _patients.value = emptyList()
            _submissionState.value = ClinicalSubmissionState.Idle
            _otpState.value = OtpState.Idle
            _passwordResetState.value = PasswordResetState.Idle
            resetTrialState()
            resetRegistrationFlow()
        }
    }


    fun getPrefs() = prefs
}