package com.example.unifiedapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.unifiedapp.ui.auth.UserProfile

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("unified_session", Context.MODE_PRIVATE)

    fun saveUser(profile: UserProfile) {
        prefs.edit().apply {
            putString("name", profile.name)
            putString("email", profile.email)
            putString("registration_id", profile.registrationId)
            putInt("age", profile.age)
            putString("gender", profile.gender)
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
            isLoggedIn = true
        )
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}