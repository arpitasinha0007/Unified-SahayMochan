package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserData(
    val isLoggedIn: Boolean = false,
    val name: String = "",
    val email: String = "",
    val gender: String = "",
    val age: Int = 0,
    val id: String = "",
    val token: String = "",
    val parentEmail: String = "",
    val isUnderage: Boolean = false,
    val registrationId: String = ""
)

class UserPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_GENDER = "user_gender"
        private const val KEY_AGE = "user_age"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "user_token"
        private const val KEY_PARENT_EMAIL = "parent_email"
        private const val KEY_IS_UNDERAGE = "is_underage"
        private const val KEY_REGISTRATION_ID = "registration_id"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userData = MutableStateFlow(getCurrentUser())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    fun getCurrentUser(): UserData {
        return UserData(
            isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false),
            name = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            gender = prefs.getString(KEY_GENDER, "") ?: "",
            age = prefs.getInt(KEY_AGE, 0),
            id = prefs.getString(KEY_USER_ID, "") ?: "",
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            parentEmail = prefs.getString(KEY_PARENT_EMAIL, "") ?: "",
            isUnderage = prefs.getBoolean(KEY_IS_UNDERAGE, false),
            registrationId = prefs.getString(KEY_REGISTRATION_ID, "") ?: ""
        )
    }

    fun saveUserData(userData: UserData) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, userData.isLoggedIn)
            putString(KEY_NAME, userData.name)
            putString(KEY_EMAIL, userData.email)
            putString(KEY_GENDER, userData.gender)
            putInt(KEY_AGE, userData.age)
            putString(KEY_USER_ID, userData.id)
            putString(KEY_TOKEN, userData.token)
            putString(KEY_PARENT_EMAIL, userData.parentEmail)
            putBoolean(KEY_IS_UNDERAGE, userData.isUnderage)
            putString(KEY_REGISTRATION_ID, userData.registrationId)
            apply()
        }
        _userData.value = userData
        Log.d("UserPreferences", "Saved user data - Name: ${userData.name}, Age: ${userData.age}, ParentEmail: ${userData.parentEmail}")
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
        _userData.value = UserData()
        Log.d("UserPreferences", "Cleared user data")
    }

    fun isUnderage(): Boolean {
        return prefs.getBoolean(KEY_IS_UNDERAGE, false)
    }

    fun getParentEmail(): String {
        return prefs.getString(KEY_PARENT_EMAIL, "") ?: ""
    }
}