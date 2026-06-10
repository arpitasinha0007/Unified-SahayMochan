package com.example.unifiedapp.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull

// Extensions should be in the same package as they are used, or imported.
// Here I use a function to get it to avoid issues with delegation if not careful.
private val Context.journalDataStore by preferencesDataStore(name = "journal_prefs")

data class VibeTag(val emoji: String, val label: String, val isCustom: Boolean = false)

data class SavedJournalEntry(
    val id: String,
    val date: String,
    val timestamp: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val wordCount: Int
)

object JournalStorage {
    private val ENTRIES_KEY = stringPreferencesKey("journal_entries")
    private val CUSTOM_TAGS_KEY = stringPreferencesKey("custom_tags")
    private val gson = Gson()

    // Using user-specific names for datastore to keep userId logic if needed,
    // but the request was to fix redeclaration.
    // If we want userId specific storage, we can't use the extension delegate directly easily for multiple users.
    // Let's stick to the userId logic from the original JournalScreen.kt if that was intended.

    private fun getUserIdDataStore(context: Context, userId: String) =
        // We can't easily use the delegate for dynamic names.
        // Let's use the manual creation if needed, or stick to one file if userId is not strictly necessary to be in the filename.
        // The original code used: context.preferencesDataStore(name = "journal_${userId}_prefs")
        // But that's a delegate.
        // Actually, there's a way to create it manually:
        // PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("journal_${userId}_prefs") })
        // But let's simplify for now if possible, or use the userId as a key prefix.

        // Reverting to the original logic but moved here.
        context.getSharedPreferences("journal_${userId}_prefs", Context.MODE_PRIVATE)
        // Wait, DataStore is preferred.

    suspend fun saveEntries(context: Context, userId: String, entries: List<SavedJournalEntry>) {
        context.journalDataStore.edit { prefs ->
            prefs[stringPreferencesKey("journal_entries_$userId")] = gson.toJson(entries)
        }
    }

    suspend fun loadEntries(context: Context, userId: String): List<SavedJournalEntry> {
        val prefs = context.journalDataStore.data.firstOrNull()
        val json = prefs?.get(stringPreferencesKey("journal_entries_$userId")) ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<SavedJournalEntry>>() {}.type)
    }

    suspend fun saveCustomTags(context: Context, userId: String, tags: List<VibeTag>) {
        context.journalDataStore.edit { prefs ->
            prefs[stringPreferencesKey("custom_tags_$userId")] = gson.toJson(tags)
        }
    }

    suspend fun loadCustomTags(context: Context, userId: String): List<VibeTag> {
        val prefs = context.journalDataStore.data.firstOrNull()
        val json = prefs?.get(stringPreferencesKey("custom_tags_$userId")) ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<VibeTag>>() {}.type)
    }

    suspend fun addEntry(context: Context, userId: String, entry: SavedJournalEntry) {
        val entries = loadEntries(context, userId)
        saveEntries(context, userId, listOf(entry) + entries)
    }
}