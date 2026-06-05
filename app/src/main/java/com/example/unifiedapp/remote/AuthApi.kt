package com.example.unifiedapp.remote

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import com.example.unifiedapp.ui.views.*

interface AuthApi {

    // Patient endpoints
    @POST("register-user")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("register-user")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login-user")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @POST("send-phone-otp")
    fun sendPhoneOtp(@Body request: SendOtpRequest): Call<SendOtpResponse>

    // ✅ Clinician registration (single, correct version)
    @POST("register-clinician")
    suspend fun registerClinician(
        @Body request: RegisterClinicianRequest
    ): Response<RegisterClinicianResponse>

    @POST("verify-phone-otp")
    fun verifyPhoneOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>

    @POST("api/send-report-via-google-mochan")
    fun sendEmailReport(@Body request: EmailReportRequest): Call<EmailReportResponse>

    @POST("api/send-quiz-report")
    suspend fun sendQuizReport(@Body report: QuizReportDto): Response<Unit>

    // Assessment Management
    @GET("api/student/{student_id}/assessments")
    suspend fun getStudentAssessments(@Path("student_id") studentId: String): Response<AssessmentListResponse>

    @DELETE("api/assessment/{id}")
    suspend fun deleteAssessment(@Path("id") id: Int): Response<Unit>

    @DELETE("api/student/delete-all/{registration_id}")
    suspend fun deleteAllUserData(@Path("registration_id") registrationId: String): Response<DeleteAllUserResponse>

    // Trial Management
    @GET("api/trials/check")
    suspend fun checkTrials(
        @Query("registration_id") registrationId: String,
        @Query("type") type: String
    ): Response<TrialCheckResponse>

    @POST("api/trials/use")
    suspend fun useTrial(@Body request: UseTrialRequest): Response<UseTrialResponse>

    // Password Reset
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<SendOtpResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResetPasswordResponse>

    // Clinical endpoints
    @POST("register-patient")
    suspend fun registerPatient(@Body request: RegisterRequest): Response<RegisterResponse>

    // ✅ Clinician login uses QUERY parameters
    @POST("login-clinician")
    suspend fun loginClinician(
        @Query("registration_id") registrationId: String,
        @Query("password") password: String
    ): Response<ClinicianLoginResponse>

    @GET("api/clinician/patients")
    suspend fun getPatients(
        @Query("clinician_id") clinicianId: String,
        @Query("search") search: String? = null
    ): Response<PatientListResponse>

    @POST("api/assessments/ham-a")
    suspend fun submitHamA(@Body request: HamARequest): Response<HamAResponse>

    @POST("api/assessments/hdrs")
    suspend fun submitHdrs(@Body request: HdrsRequest): Response<HdrsResponse>

    @GET("api/patient/clinical-assessments/{patient_id}")
    suspend fun getClinicalScores(@Path("patient_id") patientId: String): Response<ClinicalScoresResponse>
}