package com.example.unifiedapp.models

import com.google.gson.annotations.SerializedName

// ========== TRIAL MANAGEMENT ==========
data class TrialCheckResponse(
    val success: Boolean,
    @SerializedName("can_proceed") val can_proceed: Boolean,
    @SerializedName("trials_remaining") val trials_remaining: Int,
    @SerializedName("total_trials") val total_trials: Int,
    val message: String? = null
)

data class UseTrialRequest(
    @SerializedName("registration_id") val registration_id: String,
    @SerializedName("assessment_type") val assessment_type: String,
    @SerializedName("assessment_id") val assessment_id: String? = null
)

data class UseTrialResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("trials_remaining") val trials_remaining: Int
)

// ========== PASSWORD RESET ==========
data class ForgotPasswordRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

data class ResetPasswordRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("new_password") val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)