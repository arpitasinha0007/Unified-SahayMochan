package com.example.unifiedapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas

// ==========================================
// SIMPLE COLOR PALETTE (Matching other screens)
// ==========================================
val SleepPrimary = Color(0xFF5B8C8C)        // Muted teal
val SleepSecondary = Color(0xFF8E6B5C)      // Warm taupe
val SleepBackground = Color(0xFFF8F5F2)      // Warm off-white
val SleepCardBg = Color(0xFFFFFFFF)          // White
val SleepTextPrimary = Color(0xFF2D2D2D)        // Dark gray
val SleepTextMedium = Color(0xFF666666)       // Medium gray
val SleepTextLight = Color(0xFF999999)        // Light gray
val SleepGood = Color(0xFF7FAF7A)             // Green for good sleep
val SleepBad = Color(0xFFE8894A)               // Orange for poor sleep

// ==========================================
// SIMPLE DATA CLASS
// ==========================================
data class SleepEntry(
    val date: String,           // Date string (MM/dd/yyyy format)
    val hours: Float,           // Hours slept (0-24)
    val quality: Int,           // Quality rating 1-5
    val notes: String = ""
)

// ==========================================
// SIMPLE STORAGE
// ==========================================
object SimpleSleepStorage {
    private const val PREFS_NAME = "sleep_tracker"
    private const val KEY_ENTRIES = "sleep_entries"
    private val gson = Gson()

    fun getEntries(context: Context): List<SleepEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SleepEntry>>() {}.type
            gson.fromJson<List<SleepEntry>>(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveEntry(context: Context, entry: SleepEntry) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = getEntries(context).toMutableList()

        entries.removeAll { it.date == entry.date }
        entries.add(entry)

        val sorted = entries.sortedByDescending { it.date }.take(90)
        val json = gson.toJson(sorted)
        prefs.edit().putString(KEY_ENTRIES, json).apply()
    }
}

// Helper function to get today's date string
fun getTodayDateString(): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date())
}

// Helper function to format date for display
fun formatDisplayDate(dateString: String): String {
    return try {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = formatter.parse(dateString)
        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        displayFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}

// Helper function to get short day name from date
fun getShortDayName(dateString: String): String {
    return try {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = formatter.parse(dateString)
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        dayFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

// ==========================================
// MAIN SCREEN - CLEAN & SIMPLE
// ==========================================
@Composable
fun SleepQualityScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(SimpleSleepStorage.getEntries(context)) }
    var showLogSheet by remember { mutableStateOf(false) }
    val todayDate = getTodayDateString()

    val todayEntry = entries.find { it.date == todayDate }

    // Get last 7 days for chart
    val last7Days = entries.sortedBy { it.date }.takeLast(7)
    val chartData = remember(last7Days) {
        val dates = last7Days.map { getShortDayName(it.date) }
        val hours = last7Days.map { it.hours }
        Pair(dates, hours)
    }

    Scaffold(
        containerColor = SleepBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogSheet = true },
                containerColor = SleepPrimary,
                shape = CircleShape,
                modifier = Modifier.shadow(4.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log Sleep", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            SimpleSleepTopBar(onClose = onClose)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // Today's card
                item {
                    TodaySleepCard(
                        entry = todayEntry,
                        onLogClick = { showLogSheet = true }
                    )
                }

                // Simple Chart (only if we have data)
                if (last7Days.isNotEmpty()) {
                    item {
                        SimpleSleepChart(
                            dates = chartData.first,
                            hours = chartData.second,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Quick stats
                item {
                    SimpleStatsCard(
                        entries = entries,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recent history
                if (entries.isNotEmpty()) {
                    item {
                        Text(
                            "Recent History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleepTextPrimary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(entries.sortedByDescending { it.date }.take(10)) { entry ->
                        SimpleHistoryItem(entry = entry)
                    }
                }
            }
        }
    }

    // Log Sleep Bottom Sheet
    if (showLogSheet) {
        SimpleLogSleepSheet(
            onDismiss = { showLogSheet = false },
            onSave = { hours, quality, notes ->
                val entry = SleepEntry(
                    date = getTodayDateString(),
                    hours = hours,
                    quality = quality,
                    notes = notes
                )
                SimpleSleepStorage.saveEntry(context, entry)
                entries = SimpleSleepStorage.getEntries(context)
                showLogSheet = false
                Toast.makeText(context, "Sleep logged! 💤", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==========================================
// FIXED HEADER - Matches other wellness screens
// ==========================================
@Composable
fun SimpleSleepTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleepBackground)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleepPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = SleepPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column {
                Text(
                    "Sleep Tracker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleepTextPrimary
                )
                Text(
                    "Monitor your rest",
                    fontSize = 13.sp,
                    color = SleepTextMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SleepTextLight.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = SleepTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==========================================
// SIMPLE SLEEP CHART
// ==========================================
@Composable
fun SimpleSleepChart(
    dates: List<String>,
    hours: List<Float>,
    modifier: Modifier = Modifier
) {
    if (dates.isEmpty() || hours.isEmpty()) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Last 7 Days",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleepTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(SleepPrimary, CircleShape)
                    )
                    Text(
                        "Hours slept",
                        fontSize = 11.sp,
                        color = SleepTextMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SimpleBarChart(
                dates = dates,
                hours = hours,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val avgHours = if (hours.isNotEmpty()) hours.average().toFloat() else 0f
            val sleepMessage = when {
                avgHours >= 8 -> "🌟 Great! You're getting enough sleep"
                avgHours >= 6 -> "😐 Your sleep could be better"
                else -> "😴 Try to sleep more this week"
            }

            Text(
                sleepMessage,
                fontSize = 13.sp,
                color = SleepTextMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SimpleBarChart(
    dates: List<String>,
    hours: List<Float>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / (dates.size * 2.5f)
            val spacing = (size.width - (barWidth * dates.size)) / (dates.size + 1)
            val chartHeight = size.height - 40f

            dates.forEachIndexed { index, date ->
                val xPos = spacing + (index * (barWidth + spacing))
                val barHeight = (hours[index] / 12f) * chartHeight

                val barColor = when {
                    hours[index] >= 8 -> SleepGood
                    hours[index] >= 6 -> SleepSecondary
                    else -> SleepBad
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(xPos, size.height - barHeight - 25f),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dates.forEach { date ->
                Text(
                    text = date,
                    fontSize = 10.sp,
                    color = SleepTextMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(30.dp)
                )
            }
        }
    }
}

// ==========================================
// TODAY'S SLEEP CARD
// ==========================================
@Composable
fun TodaySleepCard(
    entry: SleepEntry?,
    onLogClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayDisplay = dateFormat.format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tonight",
                    fontSize = 14.sp,
                    color = SleepTextMedium
                )
                Text(
                    todayDisplay,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleepTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TodayStat(
                        value = String.format("%.1f", entry.hours),
                        unit = "hours",
                        label = "Duration",
                        color = when {
                            entry.hours >= 8 -> SleepGood
                            entry.hours >= 6 -> SleepSecondary
                            else -> SleepBad
                        }
                    )
                    TodayStat(
                        value = entry.quality.toString(),
                        unit = "/5",
                        label = "Quality",
                        color = SleepSecondary
                    )
                }

                if (entry.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "📝 ${entry.notes}",
                        fontSize = 13.sp,
                        color = SleepTextMedium,
                        maxLines = 2
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = SleepTextLight,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No sleep logged for today",
                        color = SleepTextMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLogClick,
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleepPrimary
                        )
                    ) {
                        Text("Log Sleep", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayStat(
    value: String,
    unit: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                unit,
                fontSize = 14.sp,
                color = SleepTextMedium,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            color = SleepTextLight
        )
    }
}

// ==========================================
// SIMPLE STATS CARD
// ==========================================
@Composable
fun SimpleStatsCard(
    entries: List<SleepEntry>,
    modifier: Modifier = Modifier
) {
    val avgHours = if (entries.isNotEmpty())
        entries.map { it.hours }.average().toFloat() else 0f
    val avgQuality = if (entries.isNotEmpty())
        entries.map { it.quality }.average().toFloat() else 0f
    val totalEntries = entries.size

    val sleepScore = if (entries.isNotEmpty()) {
        val hoursScore = (avgHours / 8f).coerceIn(0f, 1f) * 50
        val qualityScore = (avgQuality / 5f) * 50
        (hoursScore + qualityScore).toInt()
    } else 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sleep Score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleepTextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = when {
                        sleepScore >= 80 -> SleepGood
                        sleepScore >= 50 -> SleepSecondary
                        else -> SleepBad
                    }.copy(alpha = 0.2f)
                ) {
                    Text(
                        "$sleepScore",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            sleepScore >= 80 -> SleepGood
                            sleepScore >= 50 -> SleepSecondary
                            else -> SleepBad
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = if (avgHours > 0) String.format("%.1f", avgHours) else "-",
                    label = "Avg Hours",
                    icon = Icons.Outlined.Schedule
                )
                StatItem(
                    value = if (avgQuality > 0) String.format("%.1f", avgQuality) else "-",
                    label = "Avg Quality",
                    icon = Icons.Outlined.Star
                )
                StatItem(
                    value = totalEntries.toString(),
                    label = "Total Logs",
                    icon = Icons.Outlined.DateRange
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SleepPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SleepTextPrimary
        )
        Text(
            label,
            fontSize = 10.sp,
            color = SleepTextLight
        )
    }
}

// ==========================================
// SIMPLE HISTORY ITEM
// ==========================================
@Composable
fun SimpleHistoryItem(entry: SleepEntry) {
    val displayDate = formatDisplayDate(entry.date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    displayDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleepTextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        String.format("%.1f", entry.hours),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            entry.hours >= 8 -> SleepGood
                            entry.hours >= 6 -> SleepSecondary
                            else -> SleepBad
                        }
                    )
                    Text(
                        "h",
                        fontSize = 12.sp,
                        color = SleepTextMedium,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        if (index < entry.quality) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (index < entry.quality) SleepSecondary else SleepTextLight.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (entry.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.Notes,
                        contentDescription = "Has notes",
                        tint = SleepTextLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SIMPLE LOG SLEEP SHEET
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleLogSleepSheet(
    onDismiss: () -> Unit,
    onSave: (Float, Int, String) -> Unit
) {
    var hours by remember { mutableFloatStateOf(7.5f) }
    var quality by remember { mutableIntStateOf(3) }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Log Sleep",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SleepTextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hours
            Text(
                "Hours slept",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SleepTextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (hours > 1) hours -= 0.5f },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SleepPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease", tint = SleepPrimary)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        String.format("%.1f", hours),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleepPrimary
                    )
                    Text(
                        "hours",
                        fontSize = 12.sp,
                        color = SleepTextMedium
                    )
                }

                IconButton(
                    onClick = { if (hours < 12) hours += 0.5f },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SleepPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase", tint = SleepPrimary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quality
            Text(
                "Sleep quality",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SleepTextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 1..5) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { quality = i }
                            .padding(8.dp)
                    ) {
                        Icon(
                            if (i <= quality) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (i <= quality) SleepSecondary else SleepTextLight,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            i.toString(),
                            fontSize = 11.sp,
                            color = if (i <= quality) SleepSecondary else SleepTextLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SleepPrimary,
                    unfocusedBorderColor = SleepTextLight.copy(alpha = 0.3f)
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SleepTextMedium
                    )
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onSave(hours, quality, notes) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleepPrimary
                    )
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}