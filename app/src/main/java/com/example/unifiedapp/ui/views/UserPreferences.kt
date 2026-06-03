package com.example.unifiedapp.ui.views

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

        // Phone number
        val PHONE_NUMBER = stringPreferencesKey("phone_number")

        // Underage support
        val IS_UNDERAGE = booleanPreferencesKey("is_underage")
        val PARENT_NAME = stringPreferencesKey("parent_name")
        val PARENT_EMAIL = stringPreferencesKey("parent_email")

        // Trial tracking
        val DEPRESSION_TRIALS_REMAINING = intPreferencesKey("depression_trials_remaining")
        val ANXIETY_TRIALS_REMAINING = intPreferencesKey("anxiety_trials_remaining")
        val TOTAL_DEPRESSION_TRIALS = intPreferencesKey("total_depression_trials")
        val TOTAL_ANXIETY_TRIALS = intPreferencesKey("total_anxiety_trials")

        // Clinician session keys
        val IS_CLINICIAN_LOGGED_IN = booleanPreferencesKey("is_clinician_logged_in")
        val CLINICIAN_REGISTRATION_ID = stringPreferencesKey("clinician_registration_id")
        val CLINICIAN_NAME = stringPreferencesKey("clinician_name")
        val CLINICIAN_USER_ID = stringPreferencesKey("clinician_user_id")
        val CLINICIAN_TOKEN = stringPreferencesKey("clinician_token")

        // Legacy keys for migration
        val LEGACY_REGISTRATION_ID = stringPreferencesKey("registration_id")
        val LEGACY_STUDENT_ID = stringPreferencesKey("student_id")
    }

    // Patient data flow
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

    val registrationId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[REGISTRATION_ID] ?: prefs[LEGACY_REGISTRATION_ID] ?: prefs[LEGACY_STUDENT_ID]
    }

    val userId: Flow<String?> = dataStore.data.map { prefs -> prefs[USER_ID] }
    val isUnderage: Flow<Boolean> = dataStore.data.map { prefs -> prefs[IS_UNDERAGE] ?: false }
    val parentInfo: Flow<Pair<String, String>> = dataStore.data.map { prefs ->
        Pair(prefs[PARENT_NAME] ?: "", prefs[PARENT_EMAIL] ?: "")
    }
    val phoneNumber: Flow<String?> = dataStore.data.map { prefs -> prefs[PHONE_NUMBER] }

    // Clinician session flows
    val isClinicianLoggedIn: Flow<Boolean> = dataStore.data.map { prefs -> prefs[IS_CLINICIAN_LOGGED_IN] ?: false }
    val clinicianRegistrationId: Flow<String?> = dataStore.data.map { prefs -> prefs[CLINICIAN_REGISTRATION_ID] }
    val clinicianName: Flow<String?> = dataStore.data.map { prefs -> prefs[CLINICIAN_NAME] }
    val clinicianUserId: Flow<String?> = dataStore.data.map { prefs -> prefs[CLINICIAN_USER_ID] }
    val clinicianToken: Flow<String?> = dataStore.data.map { prefs -> prefs[CLINICIAN_TOKEN] }

    // Synchronous getters
    suspend fun getRegistrationId(): String? = dataStore.data.first().let {
        it[REGISTRATION_ID] ?: it[LEGACY_REGISTRATION_ID] ?: it[LEGACY_STUDENT_ID]
    }
    suspend fun getUserId(): String? = dataStore.data.first()[USER_ID]
    suspend fun isUserUnderage(): Boolean = dataStore.data.first()[IS_UNDERAGE] ?: false
    suspend fun getParentEmail(): String? = dataStore.data.first()[PARENT_EMAIL]
    suspend fun getPhoneNumber(): String? = dataStore.data.first()[PHONE_NUMBER]
    suspend fun getClinicianRegistrationId(): String? = dataStore.data.first()[CLINICIAN_REGISTRATION_ID]
    suspend fun getClinicianUserId(): String? = dataStore.data.first()[CLINICIAN_USER_ID]
    suspend fun getClinicianToken(): String? = dataStore.data.first()[CLINICIAN_TOKEN]

    suspend fun saveClinicianSession(registrationId: String, name: String, userId: String, token: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[IS_CLINICIAN_LOGGED_IN] = true
                prefs[CLINICIAN_REGISTRATION_ID] = registrationId
                prefs[CLINICIAN_NAME] = name
                prefs[CLINICIAN_USER_ID] = userId
                prefs[CLINICIAN_TOKEN] = token
            }
        }
    }

    suspend fun clearClinicianSession() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.remove(IS_CLINICIAN_LOGGED_IN)
                prefs.remove(CLINICIAN_REGISTRATION_ID)
                prefs.remove(CLINICIAN_NAME)
                prefs.remove(CLINICIAN_USER_ID)
                prefs.remove(CLINICIAN_TOKEN)
            }
        }
    }

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
            dataStore.edit { prefs ->
                prefs[IS_LOGGED_IN] = true
                prefs[NAME] = name
                prefs[EMAIL] = email
                prefs[GENDER] = gender
                prefs[REGISTRATION_ID] = id
                prefs[AGE] = age
                prefs[TOKEN] = token
                if (!phoneNumber.isNullOrBlank()) prefs[PHONE_NUMBER] = phoneNumber else prefs.remove(PHONE_NUMBER)
                userId?.let { prefs[USER_ID] = it }
                prefs[IS_UNDERAGE] = isUnderage
                if (isUnderage && parentName.isNotBlank()) {
                    prefs[PARENT_NAME] = parentName
                    prefs[PARENT_EMAIL] = parentEmail
                } else {
                    prefs.remove(PARENT_NAME)
                    prefs.remove(PARENT_EMAIL)
                }
                prefs[DEPRESSION_TRIALS_REMAINING] = depressionTrialsRemaining
                prefs[ANXIETY_TRIALS_REMAINING] = anxietyTrialsRemaining
                prefs[TOTAL_DEPRESSION_TRIALS] = totalDepressionTrials
                prefs[TOTAL_ANXIETY_TRIALS] = totalAnxietyTrials
            }
        }
    }

    suspend fun loginClinician(registrationId: String, name: String, userId: String, token: String) =
        saveClinicianSession(registrationId, name, userId, token)

    suspend fun updateTrialCounts(anxietyTrialsRemaining: Int? = null, depressionTrialsRemaining: Int? = null) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                anxietyTrialsRemaining?.let { prefs[ANXIETY_TRIALS_REMAINING] = it }
                depressionTrialsRemaining?.let { prefs[DEPRESSION_TRIALS_REMAINING] = it }
            }
        }
    }

    suspend fun updateParentInfo(parentName: String, parentEmail: String) {
        withContext(Dispatchers.IO) { dataStore.edit { prefs -> prefs[PARENT_NAME] = parentName; prefs[PARENT_EMAIL] = parentEmail } }
    }

    suspend fun updatePhoneNumber(phoneNumber: String) {
        withContext(Dispatchers.IO) { dataStore.edit { prefs -> prefs[PHONE_NUMBER] = phoneNumber } }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) { dataStore.edit { it.clear() } }
    }

    suspend fun forceLogout() = logout()

    suspend fun isLoggedIn(): Boolean = dataStore.data.first()[IS_LOGGED_IN] ?: false
    suspend fun isClinicianLoggedIn(): Boolean = dataStore.data.first()[IS_CLINICIAN_LOGGED_IN] ?: false

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

    suspend fun getCurrentClinician(): ClinicianData {
        val prefs = dataStore.data.first()
        return ClinicianData(
            isLoggedIn = prefs[IS_CLINICIAN_LOGGED_IN] ?: false,
            registrationId = prefs[CLINICIAN_REGISTRATION_ID] ?: "",
            name = prefs[CLINICIAN_NAME] ?: "",
            userId = prefs[CLINICIAN_USER_ID] ?: "",
            token = prefs[CLINICIAN_TOKEN] ?: ""
        )
    }

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
                prefs.remove(IS_CLINICIAN_LOGGED_IN)
                prefs.remove(CLINICIAN_REGISTRATION_ID)
                prefs.remove(CLINICIAN_NAME)
                prefs.remove(CLINICIAN_USER_ID)
                prefs.remove(CLINICIAN_TOKEN)
            }
        }
    }
}

data class UserData(
    val isLoggedIn: Boolean,
    val name: String,
    val email: String,
    val gender: String,
    val age: Int,
    val id: String,
    val TOKEN: String,
    val userId: String = "",
    val phoneNumber: String? = null,
    val isUnderage: Boolean = false,
    val parentName: String = "",
    val parentEmail: String = "",
    val depressionTrialsRemaining: Int = 3,
    val anxietyTrialsRemaining: Int = 3,
    val totalDepressionTrials: Int = 3,
    val totalAnxietyTrials: Int = 3
)

data class ClinicianData(
    val isLoggedIn: Boolean,
    val registrationId: String,
    val name: String,
    val userId: String,
    val token: String
)

data class QuizReportDto(
    val email: String,
    val name: String,
    val score: Int,
    val summary: String
)