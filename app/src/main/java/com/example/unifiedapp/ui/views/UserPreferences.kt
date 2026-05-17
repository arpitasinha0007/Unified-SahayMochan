package com.example.unifiedapp.ui.views

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// Remove this line: import kotlinx.serialization.Serializable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // Basic user info
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val GENDER = stringPreferencesKey("gender")
        val REGISTRATION_ID = stringPreferencesKey("id")
        val AGE = intPreferencesKey("age")
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")

        // Phone number - NEW
        val PHONE_NUMBER = stringPreferencesKey("phone_number")

        // Underage support - NEW
        val IS_UNDERAGE = booleanPreferencesKey("is_underage")
        val PARENT_NAME = stringPreferencesKey("parent_name")
        val PARENT_EMAIL = stringPreferencesKey("parent_email")

        // Trial tracking
        val DEPRESSION_TRIALS_REMAINING = intPreferencesKey("depression_trials_remaining")
        val ANXIETY_TRIALS_REMAINING = intPreferencesKey("anxiety_trials_remaining")
        val TOTAL_DEPRESSION_TRIALS = intPreferencesKey("total_depression_trials")
        val TOTAL_ANXIETY_TRIALS = intPreferencesKey("total_anxiety_trials")

        // Legacy keys for migration
        val LEGACY_REGISTRATION_ID = stringPreferencesKey("registration_id")
        val LEGACY_STUDENT_ID = stringPreferencesKey("student_id")
    }

    // User data flow with all fields
    val userData: Flow<UserData> = dataStore.data.map { prefs ->
        UserData(
            isLoggedIn = prefs[IS_LOGGED_IN] ?: false,
            name = prefs[NAME] ?: "",
            email = prefs[EMAIL] ?: "",
            gender = prefs[GENDER] ?: "",
            age = prefs[AGE] ?: 0,
            id = prefs[REGISTRATION_ID] ?: "",
            TOKEN = prefs[TOKEN] ?: "",
            userId = prefs[USER_ID] ?: "",
            // Phone number - NEW
            phoneNumber = prefs[PHONE_NUMBER] ?: "",
            // Underage fields
            isUnderage = prefs[IS_UNDERAGE] ?: false,
            parentName = prefs[PARENT_NAME] ?: "",
            parentEmail = prefs[PARENT_EMAIL] ?: "",
            // Trial fields
            depressionTrialsRemaining = prefs[DEPRESSION_TRIALS_REMAINING] ?: 3,
            anxietyTrialsRemaining = prefs[ANXIETY_TRIALS_REMAINING] ?: 3,
            totalDepressionTrials = prefs[TOTAL_DEPRESSION_TRIALS] ?: 3,
            totalAnxietyTrials = prefs[TOTAL_ANXIETY_TRIALS] ?: 3
        )
    }

    // Flow for registration ID (with legacy support)
    val registrationId: Flow<String?> = dataStore.data
        .map { prefs ->
            val id = prefs[REGISTRATION_ID]
                ?: prefs[LEGACY_REGISTRATION_ID]
                ?: prefs[LEGACY_STUDENT_ID]
            Log.d("AUTH_DEBUG", "registrationId flow emitting = '$id'")
            id
        }

    // Flow for user ID
    val userId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_ID]
    }

    // Flow for underage status
    val isUnderage: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_UNDERAGE] ?: false
    }

    // Flow for parent info
    val parentInfo: Flow<Pair<String, String>> = dataStore.data.map { prefs ->
        Pair(
            prefs[PARENT_NAME] ?: "",
            prefs[PARENT_EMAIL] ?: ""
        )
    }

    // Flow for phone number - NEW
    val phoneNumber: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PHONE_NUMBER]
    }

    // Get registration ID synchronously
    suspend fun getRegistrationId(): String? {
        val prefs = dataStore.data.first()
        return prefs[REGISTRATION_ID]
            ?: prefs[LEGACY_REGISTRATION_ID]
            ?: prefs[LEGACY_STUDENT_ID]
    }

    // Get user ID synchronously
    suspend fun getUserId(): String? {
        val prefs = dataStore.data.first()
        return prefs[USER_ID]
    }

    // Get underage status synchronously
    suspend fun isUserUnderage(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[IS_UNDERAGE] ?: false
    }

    // Get parent email synchronously
    suspend fun getParentEmail(): String? {
        val prefs = dataStore.data.first()
        return prefs[PARENT_EMAIL]
    }

    // Get phone number synchronously - NEW
    suspend fun getPhoneNumber(): String? {
        val prefs = dataStore.data.first()
        return prefs[PHONE_NUMBER]
    }

    // Login function - saves all user data including phone number
    suspend fun login(
        name: String,
        email: String,
        gender: String,
        id: String,
        age: Int,
        token: String,
        userId: String? = null,
        isUnderage: Boolean = false,
        parentName: String = "",
        parentEmail: String = "",
        phoneNumber: String? = null,
        depressionTrialsRemaining: Int = 3,
        anxietyTrialsRemaining: Int = 3,
        totalDepressionTrials: Int = 3,
        totalAnxietyTrials: Int = 3
    ) {
        withContext(Dispatchers.IO) {
            Log.d("AUTH_DEBUG", "========== SAVING TO PREFS ==========")
            Log.d("AUTH_DEBUG", "isUnderage: $isUnderage")
            Log.d("AUTH_DEBUG", "parentName: '$parentName'")
            Log.d("AUTH_DEBUG", "parentEmail: '$parentEmail'")
            Log.d("AUTH_DEBUG", "phoneNumber: '$phoneNumber'")

            dataStore.edit { prefs ->
                // Basic info
                prefs[IS_LOGGED_IN] = true
                prefs[NAME] = name
                prefs[EMAIL] = email
                prefs[GENDER] = gender
                prefs[REGISTRATION_ID] = id
                prefs[AGE] = age
                prefs[TOKEN] = token

                // Phone number - Handle nullable
                if (!phoneNumber.isNullOrBlank()) {
                    prefs[PHONE_NUMBER] = phoneNumber
                } else {
                    prefs.remove(PHONE_NUMBER)
                }

                if (userId != null) {
                    prefs[USER_ID] = userId
                }

                // Underage info
                prefs[IS_UNDERAGE] = isUnderage

                if (isUnderage && parentName.isNotBlank()) {
                    prefs[PARENT_NAME] = parentName
                    prefs[PARENT_EMAIL] = parentEmail
                    Log.d("AUTH_DEBUG", "✅ Saved parent info to prefs")
                } else {
                    prefs.remove(PARENT_NAME)
                    prefs.remove(PARENT_EMAIL)
                    Log.d("AUTH_DEBUG", "Not saving parent info - isUnderage: $isUnderage, parentName blank: ${parentName.isBlank()}")
                }

                // Trial info
                prefs[DEPRESSION_TRIALS_REMAINING] = depressionTrialsRemaining
                prefs[ANXIETY_TRIALS_REMAINING] = anxietyTrialsRemaining
                prefs[TOTAL_DEPRESSION_TRIALS] = totalDepressionTrials
                prefs[TOTAL_ANXIETY_TRIALS] = totalAnxietyTrials
            }

            // Verify save
            val saved = getCurrentUser()
            Log.d("AUTH_DEBUG", "Verification after save - phoneNumber: '${saved.phoneNumber}', parentName: '${saved.parentName}', parentEmail: '${saved.parentEmail}'")
        }
    }

    // Update trial counts
    suspend fun updateTrialCounts(
        anxietyTrialsRemaining: Int? = null,
        depressionTrialsRemaining: Int? = null
    ) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                anxietyTrialsRemaining?.let { prefs[ANXIETY_TRIALS_REMAINING] = it }
                depressionTrialsRemaining?.let { prefs[DEPRESSION_TRIALS_REMAINING] = it }
            }
        }
    }

    // Update parent info (if needed)
    suspend fun updateParentInfo(parentName: String, parentEmail: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[PARENT_NAME] = parentName
                prefs[PARENT_EMAIL] = parentEmail
            }
        }
    }

    // Update phone number - NEW
    suspend fun updatePhoneNumber(phoneNumber: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[PHONE_NUMBER] = phoneNumber
            }
            Log.d("AUTH_DEBUG", "Updated phone number: $phoneNumber")
        }
    }

    // Logout - clear all data
    suspend fun forceLogout() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.clear()
            }
            Log.d("AUTH_DEBUG", "User force logged out, all preferences cleared")
        }
    }

    // Also fix the existing logout method
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                // Clear all keys
                prefs.clear()
            }
            Log.d("AUTH_DEBUG", "User logged out, all preferences cleared")
        }
    }

    // Check if user is logged in
    suspend fun isLoggedIn(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[IS_LOGGED_IN] ?: false
    }

    // Get current user data synchronously
    suspend fun getCurrentUser(): UserData {
        val prefs = dataStore.data.first()
        return UserData(
            isLoggedIn = prefs[IS_LOGGED_IN] ?: false,
            name = prefs[NAME] ?: "",
            email = prefs[EMAIL] ?: "",
            gender = prefs[GENDER] ?: "",
            age = prefs[AGE] ?: 0,
            id = prefs[REGISTRATION_ID] ?: "",
            TOKEN = prefs[TOKEN] ?: "",
            userId = prefs[USER_ID] ?: "",
            phoneNumber = prefs[PHONE_NUMBER] ?: "",
            isUnderage = prefs[IS_UNDERAGE] ?: false,
            parentName = prefs[PARENT_NAME] ?: "",
            parentEmail = prefs[PARENT_EMAIL] ?: "",
            depressionTrialsRemaining = prefs[DEPRESSION_TRIALS_REMAINING] ?: 3,
            anxietyTrialsRemaining = prefs[ANXIETY_TRIALS_REMAINING] ?: 3,
            totalDepressionTrials = prefs[TOTAL_DEPRESSION_TRIALS] ?: 3,
            totalAnxietyTrials = prefs[TOTAL_ANXIETY_TRIALS] ?: 3
        )
    }

    // Clear specific user data (for debugging)
    suspend fun clearUserData() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.remove(IS_LOGGED_IN)
                prefs.remove(NAME)
                prefs.remove(EMAIL)
                prefs.remove(GENDER)
                prefs.remove(REGISTRATION_ID)
                prefs.remove(AGE)
                prefs.remove(TOKEN)
                prefs.remove(USER_ID)
                prefs.remove(PHONE_NUMBER)
                prefs.remove(IS_UNDERAGE)
                prefs.remove(PARENT_NAME)
                prefs.remove(PARENT_EMAIL)
            }
        }
    }
}

// Updated UserData class with all fields including phone number
data class UserData(
    val isLoggedIn: Boolean,
    val name: String,
    val email: String,
    val gender: String,
    val age: Int,
    val id: String,
    val TOKEN: String,
    val userId: String = "",
    // Phone number - NEW
    val phoneNumber: String? = null,
    // Underage support
    val isUnderage: Boolean = false,
    val parentName: String = "",
    val parentEmail: String = "",
    // Trial tracking
    val depressionTrialsRemaining: Int = 3,
    val anxietyTrialsRemaining: Int = 3,
    val totalDepressionTrials: Int = 3,
    val totalAnxietyTrials: Int = 3
)

// REMOVE @Serializable - you don't need it
data class QuizReportDto(
    val email: String,
    val name: String,
    val score: Int,
    val summary: String
)