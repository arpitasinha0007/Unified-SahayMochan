package com.example.unifiedapp.ui.repository

import com.example.unifiedapp.ui.views.QuizReportDto
import com.example.unifiedapp.ui.remote.ApiClient

class EmailRepository {

    suspend fun sendReport(report: QuizReportDto) {
        val response = ApiClient.emailApi.sendQuizReport(report)
        if (!response.isSuccessful) {
            throw Exception("Failed to send email")
        }
    }
}
