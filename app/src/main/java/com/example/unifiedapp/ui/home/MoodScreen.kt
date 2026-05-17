package com.example.unifiedapp.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Enhanced Data Model
data class MoodOption(
    val emoji: String,
    val label: String,
    val color: Color,
    val lightColor: Color,
    val description: String,
    val gradientColors: List<Color>
)

data class MoodEntry(
    val moodLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    onBack: () -> Unit,
    onMoodSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State management
    val moods = remember {
        listOf(
            MoodOption(
                "😔", "Sad",
                Color(0xFF90A4AE),
                Color(0xFFE3F2FD),
                "Quiet time",
                listOf(Color(0xFF90A4AE), Color(0xFFB0BEC5))
            ),
            MoodOption(
                "😐", "Neutral",
                Color(0xFFAED581),
                Color(0xFFF1F8E9),
                "Just being",
                listOf(Color(0xFFAED581), Color(0xFFC5E1A5))
            ),
            MoodOption(
                "😊", "Good",
                Color(0xFF81C784),
                Color(0xFFE8F5E8),
                "Feeling light",
                listOf(Color(0xFF81C784), Color(0xFFA5D6A7))
            ),
            MoodOption(
                "✨", "Amazing",
                Color(0xFFFFD54F),
                Color(0xFFFFF8E1),
                "On top of the world",
                listOf(Color(0xFFFFD54F), Color(0xFFFFE082))
            ),
            MoodOption(
                "🤯", "Stressed",
                Color(0xFFFFAB91),
                Color(0xFFFBE9E7),
                "Need a breath",
                listOf(Color(0xFFFFAB91), Color(0xFFFFCCBC))
            )
        )
    }

    // State variables
    var recentEntries by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var showStats by remember { mutableStateOf(false) }
    var mostFrequentMood by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Function to calculate most frequent mood
    fun calculateMostFrequentMood(entries: List<MoodEntry>): String? {
        if (entries.isEmpty()) return null

        val frequency = entries
            .groupBy { it.moodLabel }
            .mapValues { it.value.size }

        return frequency.maxByOrNull { it.value }?.key
    }

    // Function to save mood entry
    fun saveMoodEntry(moodLabel: String, moodEmoji: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existingEntry = recentEntries.find { it.date == today }

        val updatedEntries = if (existingEntry != null) {
            // Replace today's entry if it exists
            recentEntries.map {
                if (it.date == today) it.copy(moodLabel = moodLabel, timestamp = System.currentTimeMillis())
                else it
            }
        } else {
            // Add new entry
            listOf(MoodEntry(moodLabel)) + recentEntries
        }

        // Keep only last 30 days
        recentEntries = updatedEntries
            .sortedByDescending { it.timestamp }
            .take(30)

        mostFrequentMood = calculateMostFrequentMood(recentEntries)

        // Show confirmation
        val actionLabel = if (existingEntry != null) "Updated" else "Saved"
        val message = "$actionLabel $moodEmoji $moodLabel mood for today!"

        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Load sample data on first composition
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        val sampleEntries = listOf(
            "Good" to 0,
            "Amazing" to 1,
            "Good" to 2,
            "Neutral" to 3,
            "Good" to 4,
            "Stressed" to 5
        ).map { (mood, daysAgo) ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            MoodEntry(
                moodLabel = mood,
                timestamp = calendar.timeInMillis,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            )
        }
        recentEntries = sampleEntries.sortedByDescending { it.timestamp }
        mostFrequentMood = calculateMostFrequentMood(recentEntries)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F4F1))
                .padding(paddingValues)
        ) {
            // Enhanced decorative elements with more colors
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-50).dp)
                    .size(250.dp)
                    .background(Color(0xFFDDE6D5).copy(alpha = 0.5f), CircleShape)
                    .blur(60.dp)
            )
            Box(
                modifier = Modifier
                    .offset(x = 150.dp, y = 400.dp)
                    .size(200.dp)
                    .background(Color(0xFFFFD54F).copy(alpha = 0.3f), CircleShape)
                    .blur(60.dp)
            )
            Box(
                modifier = Modifier
                    .offset(x = 50.dp, y = 700.dp)
                    .size(180.dp)
                    .background(Color(0xFF81C784).copy(alpha = 0.3f), CircleShape)
                    .blur(60.dp)
            )

            // Exit Confirmation Dialog
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Go Back?") },
                    text = { Text("Your mood for today has been saved. Are you sure you want to go back?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                onBack()
                            }
                        ) {
                            Text("Yes", color = Color(0xFF4A5D23))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("Stay", color = Color.Gray)
                        }
                    }
                )
            }

            // Mood Confirmation Dialog
            if (showConfirmation && selectedMood != null) {
                Dialog(onDismissRequest = { showConfirmation = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = selectedMood!!.gradientColors
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedMood!!.emoji,
                                    fontSize = 40.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Mood Saved!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3436)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You're feeling ${selectedMood!!.label.lowercase()} today",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = { showConfirmation = false },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Close", color = Color.Gray)
                                }

                                Button(
                                    onClick = {
                                        showConfirmation = false
                                        showExitDialog = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A5D23)
                                    )
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enhanced App Bar with stats toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            // Check if they've saved a mood today
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val hasEntryToday = recentEntries.any { it.date == today }

                            if (hasEntryToday) {
                                showExitDialog = true
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color(0xFF4A5D23),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "MOOD TRACKER",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A5D23).copy(alpha = 0.6f)
                        )
                    )

                    IconButton(
                        onClick = { showStats = !showStats },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "Stats",
                            tint = Color(0xFF4A5D23),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Section
                if (showStats) {
                    StatsSection(
                        recentEntries = recentEntries,
                        mostFrequentMood = mostFrequentMood,
                        moods = moods
                    )
                } else {
                    // Elegant Header
                    Text(
                        text = "How's your soul\ntoday?",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Light,
                            color = Color(0xFF2D3436),
                            lineHeight = 44.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Take a moment to check in with yourself.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Mood Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(moods) { mood ->
                        EnhancedMoodCard(
                            mood = mood,
                            onClick = {
                                selectedMood = mood
                                saveMoodEntry(mood.label, mood.emoji)
                                showConfirmation = true
                            }
                        )
                    }
                }

                // Daily Check-in Streak
                if (!showStats && recentEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StreakIndicator(entries = recentEntries)

                    // Show today's status
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayEntry = recentEntries.find { it.date == today }

                    if (todayEntry != null) {
                        val todayMood = moods.find { it.label == todayEntry.moodLabel }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = todayMood?.lightColor ?: Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = todayMood?.color ?: Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Today's mood: ${todayMood?.emoji} ${todayEntry.moodLabel}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2D3436)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSection(
    recentEntries: List<MoodEntry>,
    mostFrequentMood: String?,
    moods: List<MoodOption>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "30-Day Statistics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3436)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Most Frequent Mood
            if (mostFrequentMood != null) {
                val mood = moods.find { it.label == mostFrequentMood }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most frequent:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mood?.emoji ?: "",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = mostFrequentMood,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = mood?.color ?: Color.Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Entries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total check-ins:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "${recentEntries.size} days",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mood Distribution (simplified)
            if (recentEntries.isNotEmpty()) {
                Text(
                    text = "Recent trend:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val last5Entries = recentEntries.take(5)
                    last5Entries.forEach { entry ->
                        val mood = moods.find { it.label == entry.moodLabel }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(mood?.color ?: Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakIndicator(entries: List<MoodEntry>) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val checkedToday = entries.any { it.date == today }

    var streak = 0
    var currentDate = Date()
    val calendar = Calendar.getInstance()

    while (true) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
        if (entries.any { it.date == dateStr }) {
            streak++
            calendar.time = currentDate
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            currentDate = calendar.time
        } else {
            break
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val isActive = index < streak
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFFFFD54F)
                        else Color.LightGray.copy(alpha = 0.3f)
                    )
            )
            if (index < 6) Spacer(modifier = Modifier.width(4.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$streak day${if (streak != 1) "s" else ""} streak",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF4A5D23),
                fontWeight = FontWeight.Medium
            )
        )

        if (!checkedToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFAB91))
            )
        }
    }
}

@Composable
fun EnhancedMoodCard(mood: MoodOption, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .scale(scale)
            .height(180.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            mood.lightColor,
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Enhanced Emoji Circle with gradient
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = mood.gradientColors
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mood.emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.scale(1.2f)
                    )
                }

                Column {
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3436)
                        )
                    )
                    Text(
                        text = mood.description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray
                        )
                    )
                }
            }

            // Decorative accent
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .offset(x = 10.dp, y = (-10).dp)
                    .background(mood.color.copy(alpha = 0.1f), CircleShape)
                    .blur(10.dp)
            )
        }
    }
}