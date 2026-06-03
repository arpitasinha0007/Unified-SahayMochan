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

    suspend fun getTrialsInfo(registrationId: String): TrialsInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getTrialsInfo for: $registrationId")
                val url = URL("$BASE_URL/api/trials/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "getTrialsInfo response: $response")
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
                Log.e(TAG, "getTrialsInfo failed, code: $responseCode")
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching trials: ${e.message}", e)
                return@withContext null
            }
        }
    }

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

    suspend fun getRemainingAnxietyTrials(registrationId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getRemainingAnxietyTrials for: $registrationId")
                val url = URL("$BASE_URL/api/trials/$registrationId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "getRemainingAnxietyTrials response: $response")
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        val remaining = json.optInt("anxiety_trials_remaining", 0)
                        Log.d(TAG, "Remaining anxiety trials: $remaining")
                        return@withContext remaining
                    }
                }
                Log.e(TAG, "getRemainingAnxietyTrials failed, code: $responseCode")
                return@withContext 0
            } catch (e: Exception) {
                Log.e(TAG, "Error getting remaining anxiety trials: ${e.message}")
                return@withContext 0
            }
        }
    }

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
                    return@withContext json.optBoolean("can_proceed", false)
                } else {
                    Log.e(TAG, "checkDepressionTrials HTTP $responseCode")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkDepressionTrials error", e)
                return@withContext false
            }
        }
    }

    suspend fun checkAnxietyTrials(registrationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "checkAnxietyTrials for registrationId: $registrationId")
                val url = URL("$BASE_URL/api/trials/check/$registrationId?assessment_type=anxiety")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d(TAG, "checkAnxietyTrials response code: $responseCode")
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "checkAnxietyTrials raw response: $response")
                    val json = JSONObject(response)
                    val canProceed = json.optBoolean("can_proceed", false)
                    Log.d(TAG, "can_proceed = $canProceed")
                    return@withContext canProceed
                } else {
                    Log.e(TAG, "checkAnxietyTrials HTTP $responseCode")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkAnxietyTrials error", e)
                return@withContext false
            }
        }
    }

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
                    return@withContext json.optBoolean("success", false)
                } else {
                    Log.e(TAG, "useDepressionTrial HTTP $responseCode")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "useDepressionTrial error", e)
                return@withContext false
            }
        }
    }

    suspend fun useAnxietyTrial(registrationId: String, assessmentId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "useAnxietyTrial for registrationId: $registrationId")
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
                    put("assessment_type", "anxiety")
                    assessmentId?.let { put("assessment_id", it) }
                }

                Log.d(TAG, "useAnxietyTrial request body: $requestBody")
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "useAnxietyTrial response code: $responseCode")
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "useAnxietyTrial response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)
                    Log.d(TAG, "Success = $success")
                    if (!success) {
                        Log.e(TAG, "Server returned success=false, message: ${json.optString("message")}")
                    }
                    return@withContext success
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "useAnxietyTrial HTTP $responseCode, error: $errorBody")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "useAnxietyTrial error", e)
                return@withContext false
            }
        }
    }
}