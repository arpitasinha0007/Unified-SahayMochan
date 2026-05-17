// Create a new file: AnonymousIdMigrationHelper.kt
package com.example.unifiedapp.utils

import android.content.Context
import android.util.Log

object AnonymousIdMigrationHelper {
    private const val TAG = "AnonymousIdMigration"

    fun ensureConsistentAnonymousId(context: Context) {
        val session = UserSessionHelper.getUserData(context)

        if (!session.isLoggedIn) return

        // Check if anonymous ID exists and is consistent
        if (session.anonymousId.isBlank()) {
            // Generate new anonymous ID
            val newAnonymousId = generateAnonymousId(session.name, session.registrationId)

            // Update session
            val updatedUserData = session.copy(anonymousId = newAnonymousId)
            UserSessionHelper.saveUserData(context, updatedUserData)

            Log.d(TAG, "Generated missing anonymous ID: $newAnonymousId")
        }

        // Ensure user_prefs also has the same ID for backward compatibility
        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val prefsAnonId = userPrefs.getString("anonymous_id", "")

        if (prefsAnonId != session.anonymousId) {
            userPrefs.edit().putString("anonymous_id", session.anonymousId).apply()
            Log.d(TAG, "Synced anonymous ID to user_prefs: ${session.anonymousId}")
        }
    }

    private fun generateAnonymousId(name: String, rollNo: String): String {
        return try {
            val input = "${name.lowercase(java.util.Locale.getDefault()).trim()}_${rollNo.lowercase(java.util.Locale.getDefault()).trim()}"
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hash = md.digest(input.toByteArray())
            val hexString = hash.joinToString("") { "%02x".format(it) }
            "STU_${hexString.take(8).uppercase(java.util.Locale.getDefault())}"
        } catch (e: Exception) {
            "STU_${System.currentTimeMillis().toString().takeLast(8)}"
        }
    }
}