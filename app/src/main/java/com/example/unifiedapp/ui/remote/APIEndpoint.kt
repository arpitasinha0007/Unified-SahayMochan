package com.example.unifiedapp.ui.remote

import com.example.unifiedapp.ui.views.ApiResponse
import com.example.unifiedapp.ui.views.AssessmentListResponse
import com.example.unifiedapp.ui.views.DeleteAllUserResponse
import com.example.unifiedapp.ui.views.DeleteAssessmentResponse
import com.example.unifiedapp.ui.views.DeleteUserRequest
import com.example.unifiedapp.ui.views.LoginRequest
import com.example.unifiedapp.ui.views.QuizReportDto
import com.example.unifiedapp.ui.views.RegisterRequest
import com.example.unifiedapp.ui.views.RegisterResponse
import retrofit2.Call
import retrofit2.http.*
import com.example.unifiedapp.ui.views.UseTrialRequest
import com.example.unifiedapp.ui.views.TrialCheckResponse
import retrofit2.Response
import com.example.unifiedapp.ui.views.SendOtpRequest
import com.example.unifiedapp.ui.views.SendOtpResponse
import com.example.unifiedapp.ui.views.ResetPasswordRequest
import com.example.unifiedapp.ui.views.ResetPasswordResponse
import com.example.unifiedapp.ui.views.ForgotPasswordRequest
import com.example.unifiedapp.ui.views.VerifyOtpRequest
import com.example.unifiedapp.ui.views.VerifyOtpResponse
interface EmailApi {
    @POST("send-report")
    suspend fun sendQuizReport(
        @Body report: QuizReportDto
    ): Response<Unit>
}

interface AuthApi {
    @POST("/register-user")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("/login-user")
    fun login(
        @Body request: LoginRequest
    ): Call<ApiResponse>

    @GET("/api/student/{student_id}/assessments")
    suspend fun getStudentAssessments(
        @Path("student_id") studentId: String
    ): Response<AssessmentListResponse>

    @DELETE("/api/assessment/{assessment_id}")
    suspend fun deleteAssessment(
        @Path("assessment_id") id: Int
    ): Response<DeleteAssessmentResponse>

    @DELETE("/api/student/delete-all/{registration_id}")
    suspend fun deleteAllUserData(
        @Path("registration_id") registrationId: String
    ): Response<DeleteAllUserResponse>

    @GET("/api/trials/check/{registration_id}")
    suspend fun checkTrials(
        @Path("registration_id") registrationId: String,
        @Query("assessment_type") assessmentType: String = "anxiety"
    ): Response<TrialCheckResponse>

    @POST("/api/trials/use-trial")
    suspend fun useTrial(
        @Body request: UseTrialRequest
    ): Response<Unit>

    // OTP Endpoints
    @POST("/send-phone-otp")  // Removed "/api/auth" prefix
    suspend fun sendPhoneOtp(
        @Body request: SendOtpRequest
    ): Response<SendOtpResponse>

    @POST("/verify-phone-otp")  // Removed "/api/auth" prefix
    suspend fun verifyPhoneOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

    // Forgot password endpoints - keep as is if those are working
    @POST("/api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<SendOtpResponse>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

}