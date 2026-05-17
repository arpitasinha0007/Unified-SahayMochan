package com.example.unifiedapp.models

import com.google.gson.annotations.SerializedName

// Request Models
data class RegisterRequest(
    @SerializedName("registration_id") val registrationId: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("email") val email: String,
    @SerializedName("age") val age: Int,
    @SerializedName("phone_no") val phoneNumber: String,
    @SerializedName("parent_name") val parentName: String? = null,
    @SerializedName("parent_email") val parentEmail: String? = null,
    @SerializedName("is_underage") val isUnderage: Boolean = false
)

data class LoginRequest(
    @SerializedName("registration_id") val registrationId: String,
    @SerializedName("password") val password: String
)

data class SendOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

data class VerifyOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String
)

// Response Models
data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("registration_id") val registrationId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("phone_no") val phoneNumber: String? = null,
    @SerializedName("trials") val trials: TrialsInfo? = null,
    @SerializedName("parent") val parent: ParentInfo? = null
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("email") val email: String,
    @SerializedName("age") val age: Int,
    @SerializedName("phone_no") val phoneNumber: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("registration_id") val registrationId: String,
    @SerializedName("trials") val trials: TrialsInfo? = null,
    @SerializedName("parent") val parent: ParentInfo? = null
)

data class TrialsInfo(
    @SerializedName("depression") val depression: Int,
    @SerializedName("anxiety") val anxiety: Int
)

data class ParentInfo(
    @SerializedName("parent_name") val name: String,
    @SerializedName("parent_email") val email: String,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

data class SendOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("verification_id") val verificationId: String? = null,
    @SerializedName("test_mode") val testMode: Boolean = false,
    @SerializedName("demo_otp") val demoOtp: String? = null
)

data class VerifyOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("phone_number") val phoneNumber: String? = null
)

// Email Report Model
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