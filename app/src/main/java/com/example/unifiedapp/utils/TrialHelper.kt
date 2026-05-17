package com.example.unifiedapp.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TrialHelper {
    private const val TAG = "TrialHelper"
    private const val BASE_URL = "http://203.110.243.202:8000"

    data class TrialsInfo(
        val exists: Boolean,
        val depressionRemaining: Int,
        val anxietyRemaining: Int,
        val canTakeDepression: Boolean,
        val canTakeAnxiety: Boolean,
        val message: String = ""
    )

    /**
     * Fetch full trial information for a user.
     */
    suspend fun getTrialsInfo(registrationId: String): TrialsInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        return@withContext TrialsInfo(
                            exists = json.optBoolean("exists", false),
                            depressionRemaining = json.optInt("depression_trials_remaining", 0),
                            anxietyRemaining = json.optInt("anxiety_trials_remaining", 0),
                            canTakeDepression = json.optBoolean("can_take_depression", false),
                            canTakeAnxiety = json.optBoolean("can_take_anxiety", false),
                            message = json.optString("message", "")
                        )
                    }
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching trials: ${e.message}")
                return@withContext null
            }
        }
    }

    /**
     * Get remaining depression trials for a user.
     * Returns the number of remaining trials or 0 if error.
     */
    suspend fun getRemainingDepressionTrials(registrationId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        return@withContext json.optInt("depression_trials_remaining", 0)
                    }
                }
                return@withContext 0
            } catch (e: Exception) {
                Log.e(TAG, "Error getting remaining depression trials: ${e.message}")
                return@withContext 0
            }
        }
    }

    /**
     * Check if user has remaining depression trials.
     * Returns true if can proceed, false otherwise.
     */
    suspend fun checkDepressionTrials(registrationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/check/$registrationId?assessment_type=depression")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    json.optBoolean("can_proceed", false)
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking depression trials: ${e.message}")
                false
            }
        }
    }

    /**
     * Consume one depression trial when the user views the result screen.
     */
    suspend fun useDepressionTrial(registrationId: String, assessmentId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/trials/use-trial")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val requestBody = JSONObject().apply {
                    put("registration_id", registrationId)
                    put("assessment_type", "depression")
                    assessmentId?.let { put("assessment_id", it) }
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    json.optBoolean("success", false)
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error using depression trial: ${e.message}")
                false
            }
        }
    }
}