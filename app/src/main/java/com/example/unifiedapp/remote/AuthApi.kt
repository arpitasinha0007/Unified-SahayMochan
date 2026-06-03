package com.example.unifiedapp.remote

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import com.example.unifiedapp.ui.views.*

interface AuthApi {

    // ==================== EXISTING ENDPOINTS ====================
    @POST("register-user")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("login-user")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("send-phone-otp")
    fun sendPhoneOtp(
        @Body request: SendOtpRequest
    ): Call<SendOtpResponse>

    @POST("verify-phone-otp")
    fun verifyPhoneOtp(
        @Body request: VerifyOtpRequest
    ): Call<VerifyOtpResponse>

    @POST("api/send-report-via-google-mochan")
    fun sendEmailReport(
        @Body request: EmailReportRequest
    ): Call<EmailReportResponse>

    // Missing methods needed by UI
    @GET("api/trials/check")
    suspend fun checkTrials(
        @Query("registration_id") registrationId: String,
        @Query("assessment_type") type: String
    ): Response<TrialCheckResponse>

    @POST("api/trials/use")
    suspend fun useTrial(
        @Body request: UseTrialRequest
    ): Response<UseTrialResponse>

    @POST("api/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<SendOtpResponse>

    @POST("api/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    @GET("api/student/assessments/{student_id}")
    suspend fun getStudentAssessments(
        @Path("student_id") studentId: String
    ): Response<AssessmentListResponse>

    @DELETE("api/assessments/{assessment_id}")
    suspend fun deleteAssessment(
        @Path("assessment_id") assessmentId: Int
    ): Response<DeleteAssessmentResponse>

    @DELETE("api/student/data/{registration_id}")
    suspend fun deleteAllUserData(
        @Path("registration_id") registrationId: String
    ): Response<DeleteAllUserResponse>

    @POST("register-user")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/send-quiz-report")
    suspend fun sendQuizReport(
        @Body report: QuizReportDto
    ): Response<Unit>

    // ==================== CLINICAL ENDPOINTS ====================

    @POST("register-patient")
    suspend fun registerPatient(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("register-clinician")
    suspend fun registerClinician(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    // ✅ FIXED: Clinician login with query parameters (form-urlencoded)
    @POST("login-clinician")
    @FormUrlEncoded
    suspend fun loginClinician(
        @Field("registration_id") registrationId: String,
        @Field("password") password: String
    ): Response<ClinicianLoginResponse>

    @GET("api/clinician/patients")
    suspend fun getPatients(
        @Query("clinician_id") clinicianId: String,
        @Query("search") search: String? = null
    ): Response<PatientListResponse>

    @POST("api/assessments/ham-a")
    suspend fun submitHamA(
        @Body request: HamARequest
    ): Response<HamAResponse>

    @POST("api/assessments/hdrs")
    suspend fun submitHdrs(
        @Body request: HdrsRequest
    ): Response<HdrsResponse>

    @GET("api/patient/clinical-assessments/{patient_id}")
    suspend fun getClinicalScores(
        @Path("patient_id") patientId: String
    ): Response<ClinicalScoresResponse>
}
