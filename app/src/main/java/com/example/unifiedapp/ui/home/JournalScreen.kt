package com.example.unifiedapp.ui.home

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// VIBRANT COLOR PALETTE (Like SoundScreen)
// ==========================================

// Primary brand colors (your app's sage foundation)
val JournalSageLight = Color(0xFFF1F7F3)
val JournalSageMedium = Color(0xFFD3E4D6)
val JournalSageAccent = Color(0xFF6B9071)
val JournalCharcoal = Color(0xFF3E4E42)
val JournalWhiteSoft = Color(0xFFFAFAFA)
val JournalMutedSlate = Color(0xFF5D6D66)

// Vibrant accent colors (like SoundScreen)
val JournalCoral = Color(0xFFFF8A7F)
val JournalCoralLight = Color(0xFFFFD1CC)
val JournalSky = Color(0xFF7EC8E3)
val JournalSkyLight = Color(0xFFD4F0FF)
val JournalLavender = Color(0xFFC7B5F0)
val JournalLavenderLight = Color(0xFFF0E8FF)
val JournalMint = Color(0xFFA8E6CF)
val JournalMintLight = Color(0xFFE0FFF0)
val JournalPeach = Color(0xFFFFD7B5)
val JournalPeachLight = Color(0xFFFFF0E0)
val JournalRose = Color(0xFFF7CAC9)
val JournalRoseLight = Color(0xFFFFE9E8)
val JournalAmber = Color(0xFFFFE08C)
val JournalAmberLight = Color(0xFFFFF5D9)

// Gradients
val GradientSunset = Brush.linearGradient(
    colors = listOf(JournalCoral, JournalPeach)
)
val GradientOcean = Brush.linearGradient(
    colors = listOf(JournalSky, JournalMint)
)
val GradientLavender = Brush.linearGradient(
    colors = listOf(JournalLavender, JournalRose)
)
val GradientAmber = Brush.linearGradient(
    colors = listOf(JournalAmber, JournalPeach)
)

// ==========================================
// DATA CLASSES
// ==========================================

data class VibeTag(
    val emoji: String,
    val label: String,
    val isCustom: Boolean = false,
    val gradient: Brush = listOf(
        GradientSunset, GradientOcean, GradientLavender, GradientAmber
    ).random()
)

data class SavedJournalEntry(
    val id: String,
    val date: String,
    val timestamp: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val wordCount: Int,
    val gradientIndex: Int = (0..3).random() // For consistent card gradients
)

// Simple storage
object JournalStorage {
    suspend fun loadEntries(context: Context): List<SavedJournalEntry> = emptyList()
    suspend fun saveEntries(context: Context, entries: List<SavedJournalEntry>) {}
    suspend fun loadCustomTags(context: Context): List<VibeTag> = emptyList()
    suspend fun saveCustomTags(context: Context, tags: List<VibeTag>) {}
    suspend fun addEntry(context: Context, entry: SavedJournalEntry) {}
}

// ==========================================
// MAIN JOURNAL SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onBack: () -> Unit) {
    var titleText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var customTags by remember { mutableStateOf<List<VibeTag>>(emptyList()) }
    var savedEntries by remember { mutableStateOf<List<SavedJournalEntry>>(emptyList()) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var emojiInput by remember { mutableStateOf("") }
    var emojiError by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<SavedJournalEntry?>(null) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<SavedJournalEntry?>(null) }

    val context = LocalContext.current

    // Word limits
    val maxTitleWords = 20
    val maxContentWords = 500

    val currentTitleWords = titleText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val currentContentWords = noteText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

    val isTitleOverLimit = currentTitleWords > maxTitleWords
    val isContentOverLimit = currentContentWords > maxContentWords
    val isOverLimit = isTitleOverLimit || isContentOverLimit

    val maxEntries = 50

    // Load data on start
    LaunchedEffect(Unit) {
        customTags = JournalStorage.loadCustomTags(context)
        savedEntries = JournalStorage.loadEntries(context)
    }

    // Predefined vibe tags with gradients
    val predefinedVibeTags = listOf(
        VibeTag("📚", "Learning", gradient = GradientOcean),
        VibeTag("💼", "Work", gradient = GradientSunset),
        VibeTag("❤️", "Love", gradient = GradientLavender),
        VibeTag("✈️", "Travel", gradient = GradientAmber),
        VibeTag("🎨", "Creative", gradient = GradientLavender),
        VibeTag("🏃", "Health", gradient = GradientOcean),
        VibeTag("🧘", "Mindfulness", gradient = GradientOcean),
        VibeTag("🎮", "Fun", gradient = GradientAmber),
        VibeTag("👨‍👩‍👧", "Family", gradient = GradientSunset),
        VibeTag("🌱", "Growth", gradient = GradientOcean),
        VibeTag("☕", "Daily", gradient = GradientAmber),
        VibeTag("🌟", "Achievement", gradient = GradientLavender)
    )

    val allTags = remember(predefinedVibeTags, customTags) { predefinedVibeTags + customTags }

    val gradients = listOf(GradientSunset, GradientOcean, GradientLavender, GradientAmber)

    // Streak calculation
    fun calculateStreak(entries: List<SavedJournalEntry>): Int {
        if (entries.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        val sortedEntries = entries.sortedByDescending { it.timestamp }
        val mostRecentDate = Date(sortedEntries.first().timestamp)

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return if (sdf.format(mostRecentDate) == sdf.format(today)) {
            // Has entry today, calculate streak
            var streak = 1
            var currentDate = calendar
            currentDate.time = today
            currentDate.add(Calendar.DAY_OF_YEAR, -1)

            while (true) {
                val dateToCheck = sdf.format(currentDate.time)
                val hasEntry = sortedEntries.any {
                    sdf.format(Date(it.timestamp)) == dateToCheck
                }
                if (hasEntry) {
                    streak++
                    currentDate.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            streak
        } else if (sdf.format(mostRecentDate) == sdf.format(yesterday)) {
            // Most recent was yesterday, calculate from there
            var streak = 1
            var currentDate = calendar
            currentDate.time = yesterday
            currentDate.add(Calendar.DAY_OF_YEAR, -1)

            while (true) {
                val dateToCheck = sdf.format(currentDate.time)
                val hasEntry = sortedEntries.any {
                    sdf.format(Date(it.timestamp)) == dateToCheck
                }
                if (hasEntry) {
                    streak++
                    currentDate.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            streak
        } else {
            0
        }
    }

    fun isValidEmoji(input: String): Boolean {
        if (input.isEmpty() || input.length > 2) return false
        return input.matches(Regex("[\\p{So}\\p{Sk}\\u20E3\\uFE0F\\u200D]"))
    }

    fun findExistingTag(emoji: String): VibeTag? = allTags.find { it.emoji == emoji }

    fun saveEntry() {
        if (titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit) {
            if (savedEntries.size >= maxEntries) {
                showDeleteWarning = true
                return
            }

            val entry = SavedJournalEntry(
                id = UUID.randomUUID().toString(),
                date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()),
                timestamp = System.currentTimeMillis(),
                title = titleText,
                content = noteText,
                tags = selectedTags,
                wordCount = currentContentWords,
                gradientIndex = (0..3).random()
            )

            savedEntries = listOf(entry) + savedEntries
            CoroutineScope(Dispatchers.IO).launch { JournalStorage.addEntry(context, entry) }

            titleText = ""
            noteText = ""
            selectedTags = emptyList()
        }
    }

    fun deleteEntry(entry: SavedJournalEntry) {
        savedEntries = savedEntries.filter { it.id != entry.id }
        CoroutineScope(Dispatchers.IO).launch { JournalStorage.saveEntries(context, savedEntries) }
    }

    // Calculate stats
    val totalEntries = savedEntries.size
    val totalWords = savedEntries.sumOf { it.wordCount }
    val avgWords = if (totalEntries > 0) totalWords / totalEntries else 0
    val streak = calculateStreak(savedEntries)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Modern TopAppBar with gradient (like SoundScreen)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Journal",
                            style = MaterialTheme.typography.headlineSmall,
                            color = JournalCharcoal,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Capture your thoughts",
                            style = MaterialTheme.typography.bodySmall,
                            color = JournalMutedSlate
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JournalWhiteSoft)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = JournalCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JournalSageLight,
                            JournalWhiteSoft
                        )
                    )
                )
        ) {
            // Decorative floating elements
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = (-50).dp, y = (-50).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(JournalCoral.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(JournalSky.copy(alpha = 0.1f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Stats Cards Row (Like WealthScreen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Streak Card
                    StatsCard(
                        value = "$streak",
                        label = "Day Streak",
                        icon = Icons.Outlined.LocalFireDepartment,
                        gradient = GradientSunset,
                        modifier = Modifier.weight(1f)
                    )

                    // Entries Card
                    StatsCard(
                        value = totalEntries.toString(),
                        label = "Total Entries",
                        icon = Icons.Outlined.MenuBook,
                        gradient = GradientOcean,
                        modifier = Modifier.weight(1f)
                    )

                    // Words Card
                    StatsCard(
                        value = avgWords.toString(),
                        label = "Avg Words",
                        icon = Icons.Outlined.Edit,
                        gradient = GradientLavender,
                        modifier = Modifier.weight(1f)
                    )
                }

                // New Entry Card - Redesigned
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header with gradient icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GradientOcean),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Create,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Write New Entry",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalCharcoal
                                )
                                Text(
                                    "Express yourself today",
                                    fontSize = 12.sp,
                                    color = JournalMutedSlate
                                )
                            }

                            // Character count indicator
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = JournalSageLight
                            ) {
                                Text(
                                    "${savedEntries.size}/$maxEntries",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = JournalSageAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tags Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Add Tags",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JournalMutedSlate
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = JournalLavenderLight,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { showAddTagDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add tag",
                                            tint = JournalLavender,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Colorful tag row
                            if (allTags.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(allTags) { tag ->
                                        ColorfulTagChip(
                                            tag = tag,
                                            isSelected = selectedTags.contains(tag.label),
                                            onSelect = {
                                                selectedTags = if (selectedTags.contains(tag.label)) {
                                                    selectedTags - tag.label
                                                } else {
                                                    selectedTags + tag.label
                                                }
                                            },
                                            onRemove = {
                                                if (tag.isCustom) {
                                                    customTags = customTags - tag
                                                    selectedTags = selectedTags - tag.label
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        JournalStorage.saveCustomTags(context, customTags)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "No tags yet. Add one!",
                                    fontSize = 12.sp,
                                    color = JournalMutedSlate,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title Input
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = {
                                val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                if (words <= maxTitleWords) titleText = it
                            },
                            placeholder = {
                                Text(
                                    "Give your entry a title...",
                                    color = JournalMutedSlate.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            isError = isTitleOverLimit,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JournalSageAccent,
                                unfocusedBorderColor = JournalSageMedium,
                                focusedContainerColor = JournalWhiteSoft,
                                unfocusedContainerColor = JournalSageLight
                            )
                        )

                        // Word count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "$currentTitleWords/$maxTitleWords words",
                                fontSize = 11.sp,
                                color = if (isTitleOverLimit) Color.Red else JournalMutedSlate
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Content Input
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = {
                                val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                if (words <= maxContentWords) noteText = it
                            },
                            placeholder = {
                                Text(
                                    "What's on your mind?",
                                    color = JournalMutedSlate.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(16.dp),
                            isError = isContentOverLimit,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JournalSageAccent,
                                unfocusedBorderColor = JournalSageMedium,
                                focusedContainerColor = JournalWhiteSoft,
                                unfocusedContainerColor = JournalSageLight
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "$currentContentWords/$maxContentWords words",
                                fontSize = 11.sp,
                                color = if (isContentOverLimit) Color.Red else JournalMutedSlate
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Save Button with gradient
                        val isEnabled = titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit

                        Button(
                            onClick = { saveEntry() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnabled) JournalSageAccent else Color.LightGray
                            ),
                            enabled = isEnabled
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Save Entry",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Add gradient overlay hint
                        if (isEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(GradientOcean)
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Entries Header
                if (savedEntries.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 24.dp)
                                    .background(GradientLavender, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Recent Entries",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalCharcoal
                            )
                        }

                        Text(
                            "View All →",
                            fontSize = 14.sp,
                            color = JournalSageAccent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { /* Navigate to all entries */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Entries List
                if (savedEntries.isEmpty()) {
                    // Colorful Empty State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(200.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated icon
                            val infiniteTransition = rememberInfiniteTransition()
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GradientSunset),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Your journal is empty",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalCharcoal
                            )

                            Text(
                                "Write your first entry above",
                                fontSize = 14.sp,
                                color = JournalMutedSlate
                            )
                        }
                    }
                } else {
                    // Colorful entry cards
                    savedEntries.take(5).forEach { entry ->
                        JournalEntryCard(
                            entry = entry,
                            allTags = allTags,
                            gradients = gradients,
                            onClick = { selectedEntry = entry },
                            onDelete = {
                                entryToDelete = entry
                                showDeleteWarning = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Full Entry Dialog
    if (selectedEntry != null) {
        FullEntryDialog(
            entry = selectedEntry!!,
            allTags = allTags,
            gradients = gradients,
            onDismiss = { selectedEntry = null }
        )
    }

    // Delete Warning Dialog
    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false; entryToDelete = null },
            title = {
                Text(
                    if (entryToDelete != null) "Delete Entry?" else "Journal Limit Reached",
                    color = JournalCharcoal
                )
            },
            text = {
                if (entryToDelete != null) {
                    Text("This entry will be permanently removed.", color = JournalMutedSlate)
                } else {
                    Text(
                        "You've reached the maximum of $maxEntries entries. Delete some to continue.",
                        color = JournalMutedSlate
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (entryToDelete != null) deleteEntry(entryToDelete!!)
                        showDeleteWarning = false
                        entryToDelete = null
                    }
                ) {
                    Text(
                        if (entryToDelete != null) "Delete" else "OK",
                        color = JournalCoral
                    )
                }
            },
            dismissButton = {
                if (entryToDelete != null) {
                    TextButton(onClick = { showDeleteWarning = false; entryToDelete = null }) {
                        Text("Cancel", color = JournalMutedSlate)
                    }
                }
            }
        )
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                emojiInput = ""
                newTagName = ""
                emojiError = null
            },
            title = {
                Text(
                    "Create Custom Tag",
                    color = JournalCharcoal
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = emojiInput,
                        onValueChange = { emojiInput = it },
                        label = { Text("Emoji", color = JournalMutedSlate) },
                        placeholder = { Text("e.g., ✨") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = emojiError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JournalLavender,
                            unfocusedBorderColor = JournalSageMedium
                        )
                    )
                    if (emojiError != null) {
                        Text(
                            emojiError!!,
                            color = JournalCoral,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag name", color = JournalMutedSlate) },
                        placeholder = { Text("e.g., Motivation") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JournalLavender,
                            unfocusedBorderColor = JournalSageMedium
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isValidEmoji(emojiInput)) {
                            emojiError = "Please enter a valid emoji"
                            return@TextButton
                        }
                        if (newTagName.isBlank()) {
                            emojiError = "Please enter a tag name"
                            return@TextButton
                        }

                        val existingTag = findExistingTag(emojiInput)
                        if (existingTag != null) {
                            selectedTags = selectedTags + existingTag.label
                            showAddTagDialog = false
                            emojiInput = ""
                            newTagName = ""
                            emojiError = null
                        } else {
                            val newTag = VibeTag(
                                emojiInput,
                                newTagName,
                                true,
                                gradient = gradients.random()
                            )
                            customTags = customTags + newTag
                            selectedTags = selectedTags + newTagName
                            CoroutineScope(Dispatchers.IO).launch {
                                JournalStorage.saveCustomTags(context, customTags)
                            }
                            showAddTagDialog = false
                            emojiInput = ""
                            newTagName = ""
                            emojiError = null
                        }
                    },
                    enabled = emojiInput.isNotEmpty() && newTagName.isNotBlank()
                ) {
                    Text("Add", color = JournalSageAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTagDialog = false
                    emojiInput = ""
                    newTagName = ""
                    emojiError = null
                }) {
                    Text("Cancel", color = JournalMutedSlate)
                }
            }
        )
    }
}

// ==========================================
// STATS CARD (Like WealthScreen)
// ==========================================

@Composable
fun StatsCard(
    value: String,
    label: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = JournalCharcoal
            )

            Text(
                label,
                fontSize = 10.sp,
                color = JournalMutedSlate
            )
        }
    }
}

// ==========================================
// COLORFUL TAG CHIP
// ==========================================

@Composable
fun ColorfulTagChip(
    tag: VibeTag,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else JournalSageLight,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier
            .wrapContentWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    brush = if (isSelected) tag.gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(tag.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                tag.label,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else JournalCharcoal,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (tag.isCustom) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Color.White.copy(alpha = 0.3f) else JournalCoralLight,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onRemove)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = if (isSelected) Color.White else JournalCoral,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// JOURNAL ENTRY CARD (Colorful)
// ==========================================

@Composable
fun JournalEntryCard(
    entry: SavedJournalEntry,
    allTags: List<VibeTag>,
    gradients: List<Brush>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val entryGradient = gradients[entry.gradientIndex % gradients.size]
    val entryTags = allTags.filter { it.label in entry.tags }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colorful header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(entryGradient)
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = JournalMutedSlate,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            entry.date,
                            fontSize = 12.sp,
                            color = JournalMutedSlate
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = JournalCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    entry.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalCharcoal,
                    maxLines = 1
                )

                // Preview
                Text(
                    entry.content.take(100) + if (entry.content.length > 100) "..." else "",
                    fontSize = 14.sp,
                    color = JournalMutedSlate,
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                // Tags row
                if (entryTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(entryTags.take(3)) { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = JournalSageLight,
                                modifier = Modifier
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(brush = tag.gradient, shape = RoundedCornerShape(16.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag.emoji, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        tag.label,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        if (entryTags.size > 3) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = JournalSageLight
                                ) {
                                    Text(
                                        "+${entryTags.size - 3}",
                                        fontSize = 10.sp,
                                        color = JournalMutedSlate,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = JournalMutedSlate.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${entry.wordCount} words",
                        fontSize = 10.sp,
                        color = JournalMutedSlate
                    )
                }
            }
        }
    }
}

// ==========================================
// FULL ENTRY DIALOG
// ==========================================

@Composable
fun FullEntryDialog(
    entry: SavedJournalEntry,
    allTags: List<VibeTag>,
    gradients: List<Brush>,
    onDismiss: () -> Unit
) {
    val entryGradient = gradients[entry.gradientIndex % gradients.size]
    val entryTags = allTags.filter { it.label in entry.tags }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header with gradient strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 48.dp)
                            .background(entryGradient, RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalCharcoal
                        )
                        Text(
                            entry.date,
                            fontSize = 14.sp,
                            color = JournalMutedSlate
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JournalMutedSlate
                        )
                    }
                }

                if (entryTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Tags section
                    Text(
                        "Tags",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalMutedSlate,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entryTags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.background(brush = tag.gradient, shape = RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(tag.emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        tag.label,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JournalSageLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        entry.content,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = JournalCharcoal,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Word count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = JournalMutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${entry.wordCount} words",
                        fontSize = 12.sp,
                        color = JournalMutedSlate
                    )
                }
            }
        }
    }
}