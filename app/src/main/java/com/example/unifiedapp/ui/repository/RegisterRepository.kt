package com.example.unifiedapp.ui.repository

import com.example.unifiedapp.ui.remote.AuthApi
import com.example.unifiedapp.ui.views.AssessmentListResponse
import com.example.unifiedapp.ui.views.AssessmentListState
import com.example.unifiedapp.ui.views.DeleteAllUserResponse
import com.example.unifiedapp.ui.views.DeleteUserRequest
import com.example.unifiedapp.ui.views.RegisterRequest
import com.example.unifiedapp.ui.views.RegisterResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterRepository(private val api: AuthApi) {

    // ── Get assessments ───────────────────────────────────────────────────
    suspend fun getStudentAssessments(studentId: String): Result<AssessmentListResponse> {
        return try {
            val response = api.getStudentAssessments(studentId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(
                    when (response.code()) {
                        404 -> "Student not found"
                        else -> "Failed to load: ${response.code()}"
                    }
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSpecificAssessment(assessmentId: Int): Result<Unit> {
        return try {
            val response = api.deleteAssessment(assessmentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure<Unit>(Exception(
                    when (response.code()) {
                        404 -> "Assessment not found"
                        500 -> "Server error"
                        else -> "Delete failed: ${response.code()}"
                    }
                ))
            }
        } catch (e: Exception) {
            Result.failure<Unit>(e)
        }
    }

    suspend fun deleteAllUserData(registrationId: String): Result<DeleteAllUserResponse> {
        return try {
            val response = api.deleteAllUserData(registrationId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception(
                    when (response.code()) {
                        404 -> "Student not found"
                        500 -> "Server error during deletion"
                        else -> "Delete failed: ${response.code()}"
                    }
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun registerUser(request: RegisterRequest): Response<RegisterResponse> {
        return api.register(request)
    }

    suspend fun deleteAssessment(id: Int) =
        api.deleteAssessment(id)

}