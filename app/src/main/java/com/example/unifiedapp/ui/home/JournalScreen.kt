package com.example.unifiedapp.ui.home

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.unifiedapp.utils.VibeTag
import com.example.unifiedapp.utils.SavedJournalEntry
import com.example.unifiedapp.utils.JournalStorage
import com.example.unifiedapp.utils.UserSessionHelper

// ==========================================
// VIBRANT COLOR PALETTE (Like SoundScreen)
// ==========================================

val JournalSageLight = Color(0xFFF1F7F3)
val JournalSageMedium = Color(0xFFD3E4D6)
val JournalSageAccent = Color(0xFF6B9071)
val JournalCharcoal = Color(0xFF3E4E42)
val JournalWhiteSoft = Color(0xFFFAFAFA)
val JournalMutedSlate = Color(0xFF5D6D66)

val JournalCoral = Color(0xFFFF8A7F)
val JournalCoralLight = Color(0xFFFFD1CC)
val JournalSky = Color(0xFF7EC8E3)
val JournalLavender = Color(0xFFC7B5F0)
val JournalLavenderLight = Color(0xFFF0E8FF)
val JournalMint = Color(0xFFA8E6CF)
val JournalPeach = Color(0xFFFFD7B5)
val JournalRose = Color(0xFFF7CAC9)
val JournalAmber = Color(0xFFFFE08C)

val GradientSunset = Brush.linearGradient(colors = listOf(JournalCoral, JournalPeach))
val GradientOcean = Brush.linearGradient(colors = listOf(JournalSky, JournalMint))
val GradientLavender = Brush.linearGradient(colors = listOf(JournalLavender, JournalRose))
val GradientAmber = Brush.linearGradient(colors = listOf(JournalAmber, JournalPeach))

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
    val session = UserSessionHelper.getUserData(context)
    val userId = session.anonymousId.ifEmpty { session.registrationId }

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
        customTags = JournalStorage.loadCustomTags(context, userId)
        savedEntries = JournalStorage.loadEntries(context, userId)
    }

    // Predefined vibe tags with gradients
    val predefinedVibeTags = listOf(
        VibeTag("📚", "Learning"),
        VibeTag("💼", "Work"),
        VibeTag("❤️", "Love"),
        VibeTag("✈️", "Travel"),
        VibeTag("🎨", "Creative"),
        VibeTag("🏃", "Health"),
        VibeTag("🧘", "Mindfulness"),
        VibeTag("🎮", "Fun"),
        VibeTag("👨‍👩‍👧", "Family"),
        VibeTag("🌱", "Growth"),
        VibeTag("☕", "Daily"),
        VibeTag("🌟", "Achievement")
    )

    val allTags = remember(predefinedVibeTags, customTags) { predefinedVibeTags + customTags }
    val gradients = listOf(GradientSunset, GradientOcean, GradientLavender, GradientAmber)

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
            var streak = 1
            val cur = Calendar.getInstance()
            cur.time = today
            cur.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val dateToCheck = sdf.format(cur.time)
                if (sortedEntries.any { sdf.format(Date(it.timestamp)) == dateToCheck }) {
                    streak++
                    cur.add(Calendar.DAY_OF_YEAR, -1)
                } else break
            }
            streak
        } else if (sdf.format(mostRecentDate) == sdf.format(yesterday)) {
            var streak = 1
            val cur = Calendar.getInstance()
            cur.time = yesterday
            cur.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val dateToCheck = sdf.format(cur.time)
                if (sortedEntries.any { sdf.format(Date(it.timestamp)) == dateToCheck }) {
                    streak++
                    cur.add(Calendar.DAY_OF_YEAR, -1)
                } else break
            }
            streak
        } else 0
    }

    fun isValidEmoji(input: String): Boolean {
        if (input.isEmpty() || input.length > 2) return false
        return input.matches(Regex("[\\p{So}\\p{Sk}\\u20E3\\uFE0F\\u200D]"))
    }

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
                wordCount = currentContentWords
            )
            savedEntries = listOf(entry) + savedEntries
            CoroutineScope(Dispatchers.IO).launch { JournalStorage.addEntry(context, userId, entry) }
            titleText = ""
            noteText = ""
            selectedTags = emptyList()
        }
    }

    fun deleteEntry(entry: SavedJournalEntry) {
        savedEntries = savedEntries.filter { it.id != entry.id }
        CoroutineScope(Dispatchers.IO).launch { JournalStorage.saveEntries(context, userId, savedEntries) }
    }

    val totalEntries = savedEntries.size
    val totalWords = savedEntries.sumOf { it.wordCount }
    val avgWords = if (totalEntries > 0) totalWords / totalEntries else 0
    val streak = calculateStreak(savedEntries)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Journal", style = MaterialTheme.typography.headlineSmall, color = JournalCharcoal, fontWeight = FontWeight.Bold)
                        Text("Capture your thoughts", style = MaterialTheme.typography.bodySmall, color = JournalMutedSlate)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(JournalWhiteSoft)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = JournalCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(JournalSageLight, JournalWhiteSoft)))) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("$streak", "Day Streak", Icons.Outlined.LocalFireDepartment, GradientSunset, Modifier.weight(1f))
                    StatsCard(totalEntries.toString(), "Total Entries", Icons.Outlined.MenuBook, GradientOcean, Modifier.weight(1f))
                    StatsCard(avgWords.toString(), "Avg Words", Icons.Outlined.Edit, GradientLavender, Modifier.weight(1f))
                }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(32.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(GradientOcean), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Create, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Write New Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal)
                                Text("Express yourself today", fontSize = 12.sp, color = JournalMutedSlate)
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = JournalSageLight) {
                                Text("${savedEntries.size}/$maxEntries", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = JournalSageAccent, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Add Tags", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = JournalMutedSlate)
                            Surface(shape = RoundedCornerShape(16.dp), color = JournalLavenderLight, modifier = Modifier.size(36.dp).clickable { showAddTagDialog = true }) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add tag", tint = JournalLavender, modifier = Modifier.size(20.dp)) }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                            items(allTags) { tag ->
                                ColorfulTagChip(tag, selectedTags.contains(tag.label), onSelect = { selectedTags = if (selectedTags.contains(tag.label)) selectedTags - tag.label else selectedTags + tag.label }, onRemove = {
                                    if (tag.isCustom) {
                                        customTags = customTags - tag
                                        selectedTags = selectedTags - tag.label
                                        CoroutineScope(Dispatchers.IO).launch { JournalStorage.saveCustomTags(context, userId, customTags) }
                                    }
                                })
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(value = titleText, onValueChange = { val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size; if (words <= maxTitleWords) titleText = it }, placeholder = { Text("Give your entry a title...", color = JournalMutedSlate.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isError = isTitleOverLimit, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JournalSageAccent, unfocusedBorderColor = JournalSageMedium, focusedContainerColor = JournalWhiteSoft, unfocusedContainerColor = JournalSageLight))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("$currentTitleWords/$maxTitleWords words", fontSize = 11.sp, color = if (isTitleOverLimit) Color.Red else JournalMutedSlate) }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = noteText, onValueChange = { val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size; if (words <= maxContentWords) noteText = it }, placeholder = { Text("What's on your mind?", color = JournalMutedSlate.copy(alpha = 0.5f)) }, modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(16.dp), isError = isContentOverLimit, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JournalSageAccent, unfocusedBorderColor = JournalSageMedium, focusedContainerColor = JournalWhiteSoft, unfocusedContainerColor = JournalSageLight))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("$currentContentWords/$maxContentWords words", fontSize = 11.sp, color = if (isContentOverLimit) Color.Red else JournalMutedSlate) }
                        Spacer(modifier = Modifier.height(20.dp))
                        val isEnabled = titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit
                        Button(onClick = { saveEntry() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isEnabled) JournalSageAccent else Color.LightGray), enabled = isEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Save, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Entry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (savedEntries.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp, 24.dp).background(GradientLavender, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recent Entries", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (savedEntries.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(200.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(GradientSunset), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoStories, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Your journal is empty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal)
                            Text("Write your first entry above", fontSize = 14.sp, color = JournalMutedSlate)
                        }
                    }
                } else {
                    savedEntries.take(5).forEachIndexed { index, entry -> JournalEntryCard(entry, allTags, gradients, index, onClick = { selectedEntry = entry }, onDelete = { entryToDelete = entry; showDeleteWarning = true }) }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (selectedEntry != null) FullEntryDialog(selectedEntry!!, allTags, onDismiss = { selectedEntry = null })

    if (showDeleteWarning) {
        AlertDialog(onDismissRequest = { showDeleteWarning = false; entryToDelete = null }, title = { Text(if (entryToDelete != null) "Delete Entry?" else "Journal Limit Reached", color = JournalCharcoal) }, text = { if (entryToDelete != null) Text("This entry will be permanently removed.", color = JournalMutedSlate) else Text("You've reached the maximum entries. Delete some to continue.", color = JournalMutedSlate) }, confirmButton = { TextButton(onClick = { if (entryToDelete != null) deleteEntry(entryToDelete!!) else showDeleteWarning = false; entryToDelete = null }) { Text(if (entryToDelete != null) "Delete" else "OK", color = JournalCoral) } }, dismissButton = { if (entryToDelete != null) TextButton(onClick = { showDeleteWarning = false; entryToDelete = null }) { Text("Cancel", color = JournalMutedSlate) } })
    }

    if (showAddTagDialog) {
        AlertDialog(onDismissRequest = { showAddTagDialog = false; emojiInput = ""; newTagName = ""; emojiError = null }, title = { Text("Create Custom Tag", color = JournalCharcoal) }, text = { Column { OutlinedTextField(value = emojiInput, onValueChange = { emojiInput = it }, label = { Text("Emoji", color = JournalMutedSlate) }, modifier = Modifier.fillMaxWidth(), isError = emojiError != null) ; if (emojiError != null) Text(emojiError!!, color = JournalCoral, fontSize = 12.sp) ; Spacer(modifier = Modifier.height(8.dp)) ; OutlinedTextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("Tag name", color = JournalMutedSlate) }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(onClick = { if (!isValidEmoji(emojiInput)) { emojiError = "Please enter a valid emoji"; return@TextButton } ; if (newTagName.isBlank()) { emojiError = "Please enter a tag name"; return@TextButton } ; val existingTag = allTags.find { it.emoji == emojiInput } ; if (existingTag != null) { selectedTags = selectedTags + existingTag.label } else { val newTag = VibeTag(emojiInput, newTagName, true) ; customTags = customTags + newTag ; selectedTags = selectedTags + newTagName ; CoroutineScope(Dispatchers.IO).launch { JournalStorage.saveCustomTags(context, userId, customTags) } } ; showAddTagDialog = false ; emojiInput = "" ; newTagName = "" ; emojiError = null }, enabled = emojiInput.isNotEmpty() && newTagName.isNotBlank()) { Text("Add", color = JournalSageAccent) } }, dismissButton = { TextButton(onClick = { showAddTagDialog = false; emojiInput = ""; newTagName = ""; emojiError = null }) { Text("Cancel", color = JournalMutedSlate) } })
    }
}

@Composable
fun StatsCard(value: String, label: String, icon: ImageVector, gradient: Brush, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(gradient), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal)
            Text(label, fontSize = 10.sp, color = JournalMutedSlate)
        }
    }
}

@Composable
fun ColorfulTagChip(tag: VibeTag, isSelected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = if (isSelected) Color.White else JournalSageLight, modifier = Modifier.wrapContentWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSelect)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(brush = if (isSelected) GradientOcean else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), shape = RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(tag.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(tag.label, fontSize = 13.sp, color = if (isSelected) Color.White else JournalCharcoal, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (tag.isCustom) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = CircleShape, color = if (isSelected) Color.White.copy(alpha = 0.3f) else JournalCoralLight, modifier = Modifier.size(18.dp).clickable(onClick = onRemove)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = if (isSelected) Color.White else JournalCoral, modifier = Modifier.size(12.dp)) } }
            }
        }
    }
}

@Composable
fun JournalEntryCard(entry: SavedJournalEntry, allTags: List<VibeTag>, gradients: List<Brush>, index: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    val entryGradient = gradients[index % gradients.size]
    val entryTags = allTags.filter { it.label in entry.tags }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(entryGradient))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CalendarToday, null, tint = JournalMutedSlate, modifier = Modifier.size(14.dp)) ; Spacer(modifier = Modifier.width(6.dp)) ; Text(entry.date, fontSize = 12.sp, color = JournalMutedSlate) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = JournalCoral, modifier = Modifier.size(18.dp)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(entry.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal, maxLines = 1)
                Text(entry.content.take(100) + if (entry.content.length > 100) "..." else "", fontSize = 14.sp, color = JournalMutedSlate, maxLines = 2)
                if (entryTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entryTags.take(3)) { tag ->
                            Surface(shape = RoundedCornerShape(16.dp), color = JournalSageLight) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(brush = GradientOcean, shape = RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(tag.emoji, fontSize = 12.sp) ; Spacer(modifier = Modifier.width(4.dp)) ; Text(tag.label, fontSize = 10.sp, color = Color.White) } }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) { Icon(Icons.Outlined.Edit, null, tint = JournalMutedSlate.copy(alpha = 0.5f), modifier = Modifier.size(12.dp)) ; Spacer(modifier = Modifier.width(4.dp)) ; Text("${entry.wordCount} words", fontSize = 10.sp, color = JournalMutedSlate) }
            }
        }
    }
}

@Composable
fun FullEntryDialog(entry: SavedJournalEntry, allTags: List<VibeTag>, onDismiss: () -> Unit) {
    val entryTags = allTags.filter { it.label in entry.tags }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = Color.White, modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp, 48.dp).background(GradientOcean, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(entry.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = JournalCharcoal) ; Text(entry.date, fontSize = 14.sp, color = JournalMutedSlate) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = JournalMutedSlate) }
                }
                if (entryTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entryTags) { tag ->
                            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.background(brush = GradientOcean, shape = RoundedCornerShape(20.dp))) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Text(tag.emoji, fontSize = 16.sp) ; Spacer(modifier = Modifier.width(8.dp)) ; Text(tag.label, fontSize = 14.sp, color = Color.White) } }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JournalSageLight), modifier = Modifier.fillMaxWidth()) { Text(entry.content, fontSize = 16.sp, lineHeight = 24.sp, color = JournalCharcoal, modifier = Modifier.padding(20.dp)) }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Icon(Icons.Outlined.Edit, null, tint = JournalMutedSlate, modifier = Modifier.size(14.dp)) ; Spacer(modifier = Modifier.width(4.dp)) ; Text("${entry.wordCount} words", fontSize = 12.sp, color = JournalMutedSlate) }
            }
        }
    }
}