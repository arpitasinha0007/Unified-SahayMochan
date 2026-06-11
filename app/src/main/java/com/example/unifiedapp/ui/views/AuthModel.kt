package com.example.unifiedapp.ui.views

import com.google.gson.annotations.SerializedName

// ──────────────────────────────────────────────────────────────────────────
// USER REGISTRATION (Parent fields included - backend handles automatically)
// ──────────────────────────────────────────────────────────────────────────
// AuthModel.kt - Update RegisterRequest and LoginResponse

data class RegisterRequest(
    val registration_id: String,
    val password: String,
    val name: String,
    val gender: String,
    val email: String,
    val age: Int,
    val roll_no: String? = null,
    val is_underage: Boolean = false,
    val parent_name: String? = null,
    val parent_email: String? = null,
    val phone_no: String? = null
)

data class PatientRegisterRequest(
    val name: String,
    val password: String,
    val age: Int,
    val email: String,
    val gender: String,
    val phone_no: String? = null,
    val is_underage: Boolean = false,
    val parent_name: String? = null,
    val parent_email: String? = null,
    val roll_no: String? = null   // ✅ Added – will be generated uniquely
)

data class RegisterResponse(
    val message: String,
    val user_id: String? = null,
    val registration_id: String? = null,
    val trials: TrialsInfo? = null,
    val parent: ParentInfo? = null      // Returned if user is underage
)

data class TrialsInfo(
    val depression: Int,
    val anxiety: Int
)

data class ParentInfo(
    val parent_name: String,
    val parent_email: String
)

// ──────────────────────────────────────────────────────────────────────────
// LOGIN REQUEST
// ──────────────────────────────────────────────────────────────────────────
data class LoginRequest(
    val registration_id: String,
    val password: String
)

// ──────────────────────────────────────────────────────────────────────────
// LOGIN RESPONSE (Nested structure as per new backend)
// ──────────────────────────────────────────────────────────────────────────
// Update LoginResponse to include phone_no
data class LoginResponse(
    val token: String? = null,
    val name: String,
    val email: String? = null,
    val gender: String? = null,
    val registration_id: String,
    val age: Int? = null,
    val user_id: String? = null,
    val phone_no: String? = null,
    val trials: TrialsData? = null,
    val parent: ParentData? = null
)

data class TrialsData(
    val depression: TrialInfo,
    val anxiety: TrialInfo
)

data class TrialInfo(
    val remaining: Int,
    val total: Int,
    val used: Int,
    val can_take: Boolean
)

data class ParentData(
    val parent_name: String,
    val parent_email: String,
    val is_verified: Boolean
)

// ──────────────────────────────────────────────────────────────────────────
// DELETE STATES
// ──────────────────────────────────────────────────────────────────────────
sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    data class Success(val message: String) : DeleteState()
    data class Error(val message: String) : DeleteState()
}

data class DeleteUserRequest(
    val anonymousId: String
)

// ──────────────────────────────────────────────────────────────────────────
// ASSESSMENT RESPONSES
// ──────────────────────────────────────────────────────────────────────────
data class AssessmentListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("total_assessments") val totalAssessments: Int,
    @SerializedName("assessments") val assessments: List<Assessment_Data>
)

data class Assessment_Data(
    @SerializedName("id") val id: Int,
    @SerializedName("assessment_score") val anxietyPrediction: Float?,
    @SerializedName("gad7_score") val gad7Score: Float?,
    @SerializedName("phq_score") val phqScore: Float?,   // ✅ ADDED – missing field
    @SerializedName("questionnaire_score") val questionnaireScore: Float?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("video_count") val videoCount: Int
)

// ──────────────────────────────────────────────────────────────────────────
// DELETE ALL USER DATA
// ──────────────────────────────────────────────────────────────────────────
data class DeleteAllUserResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("deleted_summary") val deletedSummary: DeleteAllSummary
)

data class DeleteAllSummary(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("user_record_deleted") val userRecordDeleted: Boolean,
    @SerializedName("assessments_deleted") val assessmentsDeleted: Int,
    @SerializedName("videos_deleted") val videosDeleted: Int,
    @SerializedName("files_deleted") val filesDeleted: Int
)

// ──────────────────────────────────────────────────────────────────────────
// DELETE SPECIFIC ASSESSMENT
// ──────────────────────────────────────────────────────────────────────────
data class DeleteAssessmentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("files_deleted") val filesDeleted: Int
)

// ──────────────────────────────────────────────────────────────────────────
// TRIAL MANAGEMENT
// ──────────────────────────────────────────────────────────────────────────
data class TrialCheckResponse(
    val success: Boolean,
    val can_proceed: Boolean,
    val assessment_type: String,
    val trials_remaining: Int,
    val total_trials: Int,
    val message: String? = null
)

data class UseTrialRequest(
    val registration_id: String,
    val assessment_type: String,
    val assessment_id: String? = null
)

data class UseTrialResponse(
    val success: Boolean,
    val message: String,
    val assessment_type: String,
    val trials_remaining: Int,
    val can_take_more: Boolean
)
// Add these to AuthModel.kt

// ── OTP MODELS ───────────────────────────────────────────────────────────────
data class SendOtpRequest(
    @SerializedName("phone_number")  // Must match server's field name
    val phoneNumber: String
)

data class SendOtpResponse(
    val message: String,
    val verification_id: String? = null
)

data class VerifyOtpRequest(
    val phone_number: String,
    val otp_code: String
)

data class VerifyOtpResponse(
    val message: String,
    val verified: Boolean
)

data class ForgotPasswordRequest(
    val phone_number: String
)

data class ResetPasswordRequest(
    val phone_number: String,
    val otp_code: String,
    val new_password: String
)

data class ResetPasswordResponse(
    val message: String,
    val success: Boolean
)

// Email Report Models
data class EmailReportRequest(
    @SerializedName("to_email") val toEmail: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("assessment_type") val assessmentType: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("ai_prediction") val aiPrediction: String? = null,
    @SerializedName("phq9_score") val phq9Score: Int? = null
)

data class EmailReportResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)