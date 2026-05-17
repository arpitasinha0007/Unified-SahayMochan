package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object EmailHelper {
    private const val TAG = "EmailHelper"
    private const val SERVER_URL = "https://sahay-uz39.onrender.com"

    data class EmailReport(
        val email: String,
        val name: String,
        val score: Int,
        val summary: String
    )

    interface EmailCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    // In EmailHelper.kt, update the sendAssessmentReport function:

    suspend fun sendAssessmentReport(
        context: Context,
        report: EmailReport,
        callback: EmailCallback
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("EMAIL_DEBUG", "📤 Preparing to send email to: ${report.email}")
                Log.d("EMAIL_DEBUG", "📤 Server URL: $SERVER_URL/send-report")

                // Test if server is reachable first
                try {
                    val testUrl = URL("$SERVER_URL/health")
                    val testConnection = testUrl.openConnection() as HttpURLConnection
                    testConnection.requestMethod = "GET"
                    testConnection.connectTimeout = 5000
                    testConnection.readTimeout = 5000
                    val testResponseCode = testConnection.responseCode
                    testConnection.disconnect()
                    Log.d("EMAIL_DEBUG", "🔍 Server health check: HTTP $testResponseCode")
                } catch (e: Exception) {
                    Log.e("EMAIL_DEBUG", "🔴 Server unreachable: ${e.message}")
                }

                val url = URL("$SERVER_URL/send-report")
                Log.d("EMAIL_DEBUG", "📤 Full URL: $url")

                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                // Create JSON request body
                val requestBody = JSONObject().apply {
                    put("email", report.email)
                    put("name", report.name)
                    put("score", report.score)
                    put("summary", report.summary)
                }

                Log.d("EMAIL_DEBUG", "📤 Request body: $requestBody")

                // Write request body
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d("EMAIL_DEBUG", "📥 Response Code: $responseCode")

                val response = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                }

                connection.disconnect()

                Log.d("EMAIL_DEBUG", "📥 Response body: $response")

                if (responseCode in 200..299) {
                    withContext(Dispatchers.Main) {
                        callback.onSuccess("Report sent successfully to ${report.email}")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("Failed (HTTP $responseCode): $response")
                    }
                }

            } catch (e: Exception) {
                Log.e("EMAIL_DEBUG", "💥 Exception: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback.onError("Connection failed: ${e.message}")
                }
            }
        }
    }

    // Helper to create summary text from assessment results
    fun createAssessmentSummary(
        name: String,
        score: Int,
        severityLevel: String,
        aiScore: Int?,
        aiLabel: String?
    ): String {
        val date = java.text.SimpleDateFormat("MMMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())

        return buildString {
            appendLine("📋 PHQ-9 ASSESSMENT REPORT")
            appendLine("═══════════════════════════")
            appendLine("Date: $date")
            appendLine("Patient: $name")
            appendLine("")
            appendLine("📊 RESULTS:")
            appendLine("• PHQ-9 Score: $score/27 - $severityLevel")

            if (aiScore != null && aiLabel != null) {
                appendLine("• AI Facial Analysis: $aiScore/24 - $aiLabel")

                // Add agreement level
                val agreement = when (abs(score - (aiScore * 27/24))) {
                    in 0..3 -> "High agreement between your answers and AI analysis"
                    in 4..7 -> "Moderate agreement - some variation detected"
                    else -> "Significant variation - consider discussing with a professional"
                }
                appendLine("• $agreement")
            }

            appendLine("")
            appendLine("💡 NEXT STEPS:")
            when {
                score <= 9 -> {
                    appendLine("• Continue with self-care practices")
                    appendLine("• Monitor your mood regularly")
                    appendLine("• Practice mindfulness and relaxation")
                }
                score <= 18 -> {
                    appendLine("• Consider consulting a mental health professional")
                    appendLine("• Build a strong support network")
                    appendLine("• Track your symptoms daily")
                }
                else -> {
                    appendLine("• Seek professional help promptly")
                    appendLine("• Contact crisis services if needed: 988")
                    appendLine("• Don't isolate - reach out to trusted people")
                }
            }

            appendLine("")
            appendLine("🌱 Remember: This is a screening tool, not a diagnosis.")
            appendLine("Take care of your mental health! - Mochan Team")
        }
    }

}