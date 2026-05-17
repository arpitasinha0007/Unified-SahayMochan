package com.example.unifiedapp.screens

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "journal_prefs")

// Updated data class for saved entries with title field
data class SavedJournalEntry(
    val id: String,
    val date: String,
    val timestamp: Long,
    val title: String,           // New title field
    val content: String,
    val tags: List<String>,
    val wordCount: Int
)

object JournalStorage {
    private val ENTRIES_KEY = stringPreferencesKey("journal_entries")
    private val CUSTOM_TAGS_KEY = stringPreferencesKey("custom_tags")
    private val gson = Gson()

    // Save entries
    suspend fun saveEntries(context: Context, entries: List<SavedJournalEntry>) {
        val json = gson.toJson(entries)
        context.dataStore.edit { preferences ->
            preferences[ENTRIES_KEY] = json
        }
    }

    // Load entries as Flow
    fun getEntriesFlow(context: Context): Flow<List<SavedJournalEntry>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[ENTRIES_KEY] ?: "[]"
            val type = object : TypeToken<List<SavedJournalEntry>>() {}.type
            gson.fromJson(json, type)
        }
    }

    // Load entries once (for initial load)
    suspend fun loadEntries(context: Context): List<SavedJournalEntry> {
        return try {
            val preferences = context.dataStore.data.firstOrNull()
            val json = preferences?.get(ENTRIES_KEY) ?: "[]"
            val type = object : TypeToken<List<SavedJournalEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Save custom tags
    suspend fun saveCustomTags(context: Context, tags: List<VibeTag>) {
        val json = gson.toJson(tags)
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_TAGS_KEY] = json
        }
    }

    // Load custom tags
    suspend fun loadCustomTags(context: Context): List<VibeTag> {
        return try {
            val preferences = context.dataStore.data.firstOrNull()
            val json = preferences?.get(CUSTOM_TAGS_KEY) ?: "[]"
            val type = object : TypeToken<List<VibeTag>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Add single entry
    suspend fun addEntry(context: Context, entry: SavedJournalEntry) {
        val currentEntries = loadEntries(context)
        val newEntries = listOf(entry) + currentEntries
        saveEntries(context, newEntries)
    }
}