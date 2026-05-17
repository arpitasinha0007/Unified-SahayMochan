package com.example.unifiedapp.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.text.SimpleDateFormat
import java.util.*

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "mood_tracker_prefs")

// Mood Types - Store hex colors as strings, not Color objects
enum class MoodType(
    val label: String,
    val emoji: String,
    val colorHex: String,
    val category: MoodCategory
) {
    ECSTATIC("Ecstatic", "🤩", "#8B5CF6", MoodCategory.HAPPY),
    HAPPY("Good", "😊", "#4ADE80", MoodCategory.HAPPY),
    CONTENT("Content", "😌", "#60A5FA", MoodCategory.HAPPY),
    NEUTRAL("Okay", "😐", "#22D3EE", MoodCategory.NEUTRAL),
    TIRED("Tired", "😴", "#A78BFA", MoodCategory.SAD),
    SAD("Low", "😔", "#FB7185", MoodCategory.SAD),
    ANXIOUS("Anxious", "😰", "#FBBF24", MoodCategory.SAD),
    ANGRY("Angry", "😤", "#F87171", MoodCategory.SAD)
}

enum class MoodCategory(val colorHex: String, val emoji: String) {
    HAPPY("#4ADE80", "😊"),
    NEUTRAL("#22D3EE", "😐"),
    SAD("#FB7185", "😔")
}

// Data Class for Mood Entry
data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: MoodType,
    val note: String = "",
    val reasons: List<String> = emptyList(),
    val energy: Float = 0.5f,
    val happinessLevel: Int = 5
)

// Data Class for Daily Stats
data class DailyMoodStats(
    val date: String,
    val primaryMood: MoodType,
    val averageHappiness: Float,
    val moodCounts: Map<MoodCategory, Int>
)

// Data Class for Graph Data
data class GraphDataPoint(
    val date: String,
    val displayDate: String,
    val value: Float,
    val mood: MoodType?,
    val colorHex: String  // Store as hex string, not Color
)

// Main Helper Class
class MoodTrackerHelper(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "mood_tracker_prefs"
        private const val ENTRIES_KEY = "mood_entries"
        private const val MAX_ENTRIES = 365 // Keep 1 year of data

        // Helper function to parse color hex to Compose Color - to be used in Composable functions
        fun parseColor(colorHex: String): Color {
            return try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color.Black
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    // State flows for UI
    private val _entries = MutableStateFlow<List<MoodEntry>>(emptyList())
    val entries: StateFlow<List<MoodEntry>> = _entries.asStateFlow()

    init {
        loadEntries()
    }

    // Load entries from SharedPreferences
    private fun loadEntries() {
        try {
            val json = prefs.getString(ENTRIES_KEY, "[]") ?: "[]"
            val type = object : TypeToken<List<MoodEntry>>() {}.type
            _entries.value = gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            _entries.value = emptyList()
        }
    }

    // Save entries to SharedPreferences
    private fun saveEntries() {
        try {
            val json = gson.toJson(_entries.value)
            prefs.edit().putString(ENTRIES_KEY, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Add or update entry for today
    fun saveMoodEntry(entry: MoodEntry) {
        val today = dateFormatter.format(Date())

        // Remove existing entry for today if exists
        val updatedList = _entries.value.toMutableList()
        updatedList.removeAll { it.date == today }
        updatedList.add(0, entry)

        // Keep only last MAX_ENTRIES
        if (updatedList.size > MAX_ENTRIES) {
            _entries.value = updatedList.take(MAX_ENTRIES)
        } else {
            _entries.value = updatedList
        }

        saveEntries()
    }

    // Get today's entry
    fun getTodayEntry(): MoodEntry? {
        val today = dateFormatter.format(Date())
        return _entries.value.find { it.date == today }
    }

    // Check if today is logged
    fun isTodayLogged(): Boolean = getTodayEntry() != null

    // Get entries for last N days
    fun getLastNDays(n: Int): List<MoodEntry> {
        return _entries.value.take(n)
    }

    // Get entries for date range
    fun getEntriesForDateRange(days: Int): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val cutoff = calendar.time.time
        return _entries.value.filter { it.timestamp >= cutoff }
    }

    // Get graph data for last 30 days
    fun getLast30DaysGraphData(): List<GraphDataPoint> {
        val result = mutableListOf<GraphDataPoint>()
        val calendar = Calendar.getInstance()
        val entriesMap = _entries.value.associateBy { it.date }

        // Get all dates that have entries in the last 30 days
        val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time

        val entriesInLast30Days = _entries.value
            .filter { it.timestamp >= thirtyDaysAgo.time }
            .sortedBy { it.date }

        // Convert each entry to a GraphDataPoint
        entriesInLast30Days.forEach { entry ->
            val date = try {
                dateFormatter.parse(entry.date) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            result.add(
                GraphDataPoint(
                    date = entry.date,
                    displayDate = displayFormatter.format(date),
                    value = entry.happinessLevel.toFloat(),
                    mood = entry.mood,
                    colorHex = entry.mood.colorHex
                )
            )
        }

        return result
    }

    // Get mood category distribution for last 30 days
    fun getMoodCategoryDistribution(): Map<MoodCategory, Int> {
        val last30Days = getEntriesForDateRange(30)
        val distribution = mutableMapOf<MoodCategory, Int>()

        MoodCategory.values().forEach { category ->
            distribution[category] = last30Days.count { it.mood.category == category }
        }

        return distribution
    }

    // Get mood type distribution for last 30 days
    fun getMoodTypeDistribution(): Map<MoodType, Int> {
        val last30Days = getEntriesForDateRange(30)
        return last30Days.groupingBy { it.mood }.eachCount()
    }

    // Get average happiness for last 30 days
    fun getAverageHappiness(): Float {
        val last30Days = getEntriesForDateRange(30)
        if (last30Days.isEmpty()) return 0f
        return last30Days.map { it.happinessLevel }.average().toFloat()
    }

    // Get mood trend (improving, declining, stable)
    fun getMoodTrend(): String {
        val entries = getLastNDays(14)
        if (entries.size < 7) return "Not enough data"

        val firstWeek = entries.take(7).map { it.happinessLevel }.average()
        val lastWeek = entries.takeLast(7).map { it.happinessLevel }.average()

        return when {
            lastWeek > firstWeek + 0.5 -> "Improving 📈"
            lastWeek < firstWeek - 0.5 -> "Declining 📉"
            else -> "Stable ➡️"
        }
    }

    // Get current streak
    fun getCurrentStreak(): Int {
        if (_entries.value.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        var streak = 0
        val entriesByDate = _entries.value.associateBy { it.date }

        for (i in 0 until 365) {
            val date = calendar.apply { add(Calendar.DAY_OF_YEAR, -i) }.time
            val dateString = dateFormatter.format(date)

            if (entriesByDate.containsKey(dateString)) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    // Get insight based on mood data
    fun getInsight(): String {
        val last30Days = getEntriesForDateRange(30)
        if (last30Days.size < 5) return "Log more days to see insights! ✨"

        val happyDays = last30Days.count { it.mood.category == MoodCategory.HAPPY }
        val sadDays = last30Days.count { it.mood.category == MoodCategory.SAD }

        // Check for reason patterns
        val allReasons = last30Days.flatMap { it.reasons }
        if (allReasons.isNotEmpty()) {
            val mostCommonReason = allReasons.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            if (mostCommonReason != null) {
                val avgHappinessWithReason = last30Days.filter { it.reasons.contains(mostCommonReason) }
                    .map { it.happinessLevel }.average()
                val avgHappinessWithout = last30Days.filter { !it.reasons.contains(mostCommonReason) }
                    .map { it.happinessLevel }.average()

                return if (avgHappinessWithReason > avgHappinessWithout) {
                    "You're happiest when you're feeling '$mostCommonReason'! 😊"
                } else {
                    "Feeling '$mostCommonReason' might affect your mood 📉"
                }
            }
        }

        return if (happyDays > sadDays) {
            "You're having more happy days lately! Keep it up! 🌟"
        } else {
            "Things will get better. Small steps matter! 💪"
        }
    }

    // Clear all data (for testing)
    fun clearAllData() {
        _entries.value = emptyList()
        prefs.edit().remove(ENTRIES_KEY).apply()
    }
}

// ViewModel for Compose integration
class MoodTrackerViewModel(private val helper: MoodTrackerHelper) : ViewModel() {

    val entries = helper.entries

    // Compute these values based on entries
    val streak: Int
        get() = helper.getCurrentStreak()

    val averageHappiness: Float
        get() = helper.getAverageHappiness()

    val trend: String
        get() = helper.getMoodTrend()

    val todayLogged: Boolean
        get() = helper.isTodayLogged()

    val insight: String
        get() = helper.getInsight()

    fun getLast30DaysGraphData() = helper.getLast30DaysGraphData()
    fun getMoodCategoryDistribution() = helper.getMoodCategoryDistribution()
    fun getMoodTypeDistribution() = helper.getMoodTypeDistribution()

    fun saveMoodEntry(entry: MoodEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                helper.saveMoodEntry(entry)
            }
        }
    }

    fun getTodayEntry(): MoodEntry? = helper.getTodayEntry()
}