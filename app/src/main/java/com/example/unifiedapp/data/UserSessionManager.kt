package com.example.unifiedapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.unifiedapp.ui.auth.UserProfile

class UserSessionManager(context: Context) {

    // ✅ Use the SAME preferences file as UnifiedAuthScreen
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUser(profile: UserProfile) {
        prefs.edit().apply {
            putString("name", profile.name)
            putString("email", profile.email)
            putString("registration_id", profile.registrationId)
            putInt("age", profile.age)
            putString("gender", profile.gender)
            putString("anonymous_id", profile.anonymousId)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun getUser(): UserProfile? {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return null

        val name = prefs.getString("name", null) ?: return null
        return UserProfile(
            name = name,
            email = prefs.getString("email", "") ?: "",
            registrationId = prefs.getString("registration_id", "") ?: "",
            age = prefs.getInt("age", 0),
            gender = prefs.getString("gender", "") ?: "",
            isLoggedIn = true,
            anonymousId = prefs.getString("anonymous_id", "") ?: ""
        )
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun clearUser() = logout()
}