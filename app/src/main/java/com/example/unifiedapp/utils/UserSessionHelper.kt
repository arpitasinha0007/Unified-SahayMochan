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

    // This method is used during login (if you ever call it – currently not used, but kept for consistency)
    fun saveUserData(context: Context, userData: UserData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name", userData.name)               // ✅ changed from "user_name"
            putString("gender", userData.gender)           // ✅ changed from "user_gender"
            putString("email", userData.email)             // ✅ changed from "user_email"
            putInt("age", userData.age)                    // ✅ changed from "user_age"
            putString("registration_id", userData.registrationId)
            putString("anonymous_id", userData.anonymousId)
            putBoolean("is_logged_in", userData.isLoggedIn)
            putLong("last_updated", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "✅ Saved user data - Reg ID: ${userData.registrationId}")
    }

    // This method is used by ResultScreen and elsewhere – now reads the correct keys
    fun getUserData(context: Context): UserData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return UserData(
            name = prefs.getString("name", "") ?: "",                // ✅ fixed key
            gender = prefs.getString("gender", "") ?: "",            // ✅ fixed key
            email = prefs.getString("email", "") ?: "",              // ✅ fixed key
            age = prefs.getInt("age", 0),                            // ✅ fixed key
            registrationId = prefs.getString("registration_id", "") ?: "",
            anonymousId = prefs.getString("anonymous_id", "") ?: "",
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

    fun clearUserData(context: Context) {
        logout(context)
    }
}