package com.example.unifiedapp.ui.views

import com.google.gson.annotations.SerializedName

// Clinician Login
data class ClinicianLoginRequest(
    @SerializedName("registration_id") val registrationId: String,
    val password: String
)

// Add or update in ClinicalModels.kt

data class RegisterClinicianRequest(
    val name: String,
    val password: String,
    val email: String,
    val age: Int,
    val gender: String          // ✅ Added gender
)

data class RegisterClinicianResponse(
    val success: Boolean,
    val registration_id: String,
    val user_id: String,
    val name: String,
    val message: String? = null
)

data class ClinicianLoginResponse(
    val success: Boolean,
    val token: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("registration_id") val registrationId: String,
    val name: String,
    val role: String
)

// Patient list
data class PatientListResponse(
    val success: Boolean,
    val patients: List<PatientItem>,
    val total: Int
)

data class PatientItem(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("registration_id") val registrationId: String,
    val name: String,
    val age: Int,
    val email: String,
    @SerializedName("last_self_assessment") val lastSelfAssessment: String?,
    @SerializedName("last_ham_a") val lastHamA: String?,
    @SerializedName("last_hdrs") val lastHdrs: String?,
    @SerializedName("latest_ham_a_score") val latestHamAScore: Int?,
    @SerializedName("latest_hdrs_score") val latestHdrsScore: Int?
)

// HAM-A Assessment
data class HamARequest(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("clinician_id") val clinicianId: String,
    @SerializedName("item_scores") val itemScores: List<Int>,
    @SerializedName("total_score") val totalScore: Int? = null
)

data class HamAResponse(
    val success: Boolean,
    @SerializedName("assessment_id") val assessmentId: String,
    @SerializedName("total_score") val totalScore: Int,
    val severity: String
)

// HDRS Assessment
data class HdrsRequest(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("clinician_id") val clinicianId: String,
    @SerializedName("item_scores") val itemScores: List<Int>,
    @SerializedName("total_score") val totalScore: Int? = null
)

data class HdrsResponse(
    val success: Boolean,
    @SerializedName("assessment_id") val assessmentId: String,
    @SerializedName("total_score") val totalScore: Int,
    val severity: String
)

// Patient clinical scores (for patient dashboard)
data class ClinicalScoresResponse(
    val success: Boolean,
    @SerializedName("latest_ham_a") val latestHamA: ClinicalAssessment?,
    @SerializedName("latest_hdrs") val latestHdrs: ClinicalAssessment?,
    @SerializedName("ham_a_trend") val hamATrend: List<ClinicalTrend>?,
    @SerializedName("hdrs_trend") val hdrsTrend: List<ClinicalTrend>?
)

data class ClinicalAssessment(
    @SerializedName("total_score") val totalScore: Int,
    val severity: String,
    @SerializedName("clinician_name") val clinicianName: String,
    @SerializedName("created_at") val createdAt: String
)

data class ClinicalTrend(
    val score: Int,
    val severity: String,
    val date: String
)