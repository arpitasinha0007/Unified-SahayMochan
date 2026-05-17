package com.example.unifiedapp.remote

import com.example.unifiedapp.models.*
import retrofit2.Call
import retrofit2.http.*

interface AuthApi {

    @POST("register-user")
    fun registerUser(
        @Body request: RegisterRequest
    ): Call<RegisterResponse>

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
}