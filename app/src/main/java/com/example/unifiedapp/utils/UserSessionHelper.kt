package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log

object UserSessionHelper {
    private const val TAG = "UserSessionHelper"
    private const val PREFS_NAME = "user_session"

    data class UserData(
        val name: String,
        val gender: String,
        val email: String,
        val age: Int,
        val registrationId: String,
        val anonymousId: String,
        val isLoggedIn: Boolean
    )

    fun saveUserData(context: Context, userData: UserData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", userData.name)
            putString("user_gender", userData.gender)
            putString("user_email", userData.email)
            putInt("user_age", userData.age)
            putString("registration_id", userData.registrationId)
            putString("anonymous_id", userData.anonymousId)
            putBoolean("is_logged_in", userData.isLoggedIn)
            putLong("last_updated", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "✅ Saved user data - Reg ID: ${userData.registrationId}, Anon ID: ${userData.anonymousId}")
    }

    fun getUserData(context: Context): UserData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val anonymousId = prefs.getString("anonymous_id", "") ?: ""
        return UserData(
            name = prefs.getString("user_name", "") ?: "",
            gender = prefs.getString("user_gender", "") ?: "",
            email = prefs.getString("user_email", "") ?: "",
            age = prefs.getInt("user_age", 0),
            registrationId = prefs.getString("registration_id", "") ?: "",
            anonymousId = anonymousId,
            isLoggedIn = prefs.getBoolean("is_logged_in", false)
        )
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getRegistrationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("registration_id", "") ?: ""
    }

    fun getAnonymousId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("anonymous_id", "") ?: ""
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "User logged out")
    }

    // ADD THIS: Verification function
    fun verifyAnonymousIdConsistency(context: Context): Boolean {
        val sessionData = getUserData(context)

        // Check user_prefs for backward compatibility
        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val prefsAnonId = userPrefs.getString("anonymous_id", "")

        val isConsistent = sessionData.anonymousId == prefsAnonId

        Log.d(TAG, "=== ANONYMOUS ID VERIFICATION ===")
        Log.d(TAG, "Session Anonymous ID: ${sessionData.anonymousId}")
        Log.d(TAG, "user_prefs Anonymous ID: $prefsAnonId")
        Log.d(TAG, "Consistent: $isConsistent")

        return isConsistent
    }

    fun clearUserData(context: Context) {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

//    fun getRegistrationId(context: Context): String {
//        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
//        return prefs.getString("registration_id", "") ?: ""
//    }
}