package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.unifiedapp.utils.UserSessionHelper
import com.example.unifiedapp.utils.SavedJournalEntry
import com.example.unifiedapp.utils.VibeTag
import com.example.unifiedapp.utils.JournalStorage

// ========== GRADIENTS ==========
val CreamGradient = Brush.linearGradient(
    colors = listOf(Color.White, Color(0xFFF8F8FA))
)
val LavenderCreamGradient = Brush.linearGradient(
    colors = listOf(Color.White, Color(0xFFF5E6FF))
)
val WarmWhiteGradient = Brush.linearGradient(
    colors = listOf(Color.White, Color(0xFFFFFAF0))
)

// ========== MAIN SCREEN ==========
class JournalScreen {

    @Composable
    fun JournalContent(navController: NavController) {
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

        val maxTitleWords = 50
        val maxContentWords = 1000
        val currentTitleWords = titleText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val currentContentWords = noteText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val isTitleOverLimit = currentTitleWords > maxTitleWords
        val isContentOverLimit = currentContentWords > maxContentWords
        val isOverLimit = isTitleOverLimit || isContentOverLimit
        val maxEntries = 5000

        LaunchedEffect(Unit) {
            customTags = JournalStorage.loadCustomTags(context, userId)
            savedEntries = JournalStorage.loadEntries(context, userId)
        }

        val predefinedVibeTags = listOf(
            VibeTag("📚", "Learning"), VibeTag("💼", "Work"), VibeTag("❤️", "Love"),
            VibeTag("✈️", "Travel"), VibeTag("🎨", "Creative"), VibeTag("🏃", "Health"),
            VibeTag("🧘", "Mindfulness"), VibeTag("🎮", "Fun"), VibeTag("👨‍👩‍👧", "Family"),
            VibeTag("🌱", "Growth"), VibeTag("☕", "Daily"), VibeTag("🌟", "Achievement")
        )
        val allTags = remember(predefinedVibeTags, customTags) { predefinedVibeTags + customTags }

        fun isValidEmoji(input: String): Boolean {
            if (input.isEmpty() || input.length > 2) return false
            val codePoints = input.codePointCount(0, input.length)
            if (codePoints > 2) return false
            return input.matches(Regex("[\\p{So}\\p{Sk}\\u20E3\\uFE0F\\u200D]")) ||
                    input.codePoints().anyMatch { Character.isSurrogate(it.toChar()) }
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
                    wordCount = currentContentWords
                )
                savedEntries = listOf(entry) + savedEntries
                CoroutineScope(Dispatchers.IO).launch {
                    JournalStorage.addEntry(context, userId, entry)
                }
                titleText = ""
                noteText = ""
                selectedTags = emptyList()
            }
        }

        fun deleteEntry(entry: SavedJournalEntry) {
            savedEntries = savedEntries.filter { it.id != entry.id }
            CoroutineScope(Dispatchers.IO).launch {
                JournalStorage.saveEntries(context, userId, savedEntries)
            }
        }

        // ========== UI ==========
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFAB47BC), Color(0xFF7E57C2)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Book, contentDescription = "Journal", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Journal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Express your thoughts and feelings", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (savedEntries.size >= maxEntries - 5) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${savedEntries.size}/$maxEntries entries. Delete old ones to add more.", fontSize = 12.sp, color = Color(0xFFB45309))
                        }
                    }
                }

                // Main Journal Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.background(CreamGradient)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2335))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tag your entry", color = Color(0xFF6B7280), fontSize = 15.sp)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.size(32.dp).clickable { showAddTagDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = "Add tag", tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 10.dp)
                            ) {
                                items(allTags) { tag ->
                                    VibeTagItem(
                                        tag = tag,
                                        isSelected = selectedTags.contains(tag.label),
                                        canRemove = tag.isCustom,
                                        onSelect = {
                                            selectedTags = if (selectedTags.contains(tag.label)) selectedTags - tag.label else selectedTags + tag.label
                                        },
                                        onRemove = {
                                            if (tag.isCustom) {
                                                customTags = customTags - tag
                                                selectedTags = selectedTags - tag.label
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    JournalStorage.saveCustomTags(context, userId, customTags)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Title & Content inputs
                            Column {
                                Text("Title", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 4.dp))
                                OutlinedTextField(
                                    value = titleText,
                                    onValueChange = {
                                        val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                        if (words <= maxTitleWords) titleText = it
                                    },
                                    placeholder = { Text("Enter a title...", color = Color(0xFFBDBDBD)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    isError = isTitleOverLimit,
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = Color(0xFFE5E7EB),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        errorBorderColor = Color.Red,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    )
                                )
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                                    Text("$currentTitleWords/$maxTitleWords words", fontSize = 11.sp, color = if (isTitleOverLimit) Color.Red else Color(0xFF9CA3AF))
                                }
                                if (isTitleOverLimit) {
                                    Text("Title word limit exceeded! Maximum $maxTitleWords words.", fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
                                }

                                Text("Content", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151), modifier = Modifier.padding(bottom = 4.dp))
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = {
                                        val words = it.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                        if (words <= maxContentWords) noteText = it
                                    },
                                    placeholder = { Text("Write your thoughts here...", color = Color(0xFFBDBDBD)) },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    isError = isContentOverLimit,
                                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = Color(0xFFE5E7EB),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        errorBorderColor = Color.Red,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    )
                                )
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                                    Text("$currentContentWords/$maxContentWords words", fontSize = 11.sp, color = if (isContentOverLimit) Color.Red else Color(0xFF9CA3AF))
                                }
                                if (isContentOverLimit) {
                                    Text("Content word limit exceeded! Maximum $maxContentWords words.", fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(top = 2.dp))
                                }
                            }

                            if (selectedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Selected tags:", fontSize = 12.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(bottom = 4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(selectedTags) { tag ->
                                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF3E8FF)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                            ) {
                                                Text(tag, fontSize = 12.sp, color = Color(0xFFA855F7), modifier = Modifier.padding(end = 4.dp))
                                                Icon(
                                                    Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFA855F7),
                                                    modifier = Modifier.size(14.dp).clickable { selectedTags = selectedTags - tag }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { saveEntry() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF3E8FF),
                                    disabledContainerColor = Color(0xFFF3E8FF).copy(alpha = 0.5f)
                                ),
                                enabled = titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = if (titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit) Color(0xFFA855F7) else Color(0xFFBDBDBD))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Entry", color = if (titleText.isNotBlank() && noteText.isNotBlank() && !isOverLimit) Color(0xFFA855F7) else Color(0xFFBDBDBD), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (savedEntries.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(220.dp),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(modifier = Modifier.background(WarmWhiteGradient)) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.AutoStories, null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No journal entries yet. Start\nwriting your thoughts above.", color = Color(0xFF9CA3AF), textAlign = TextAlign.Center, fontSize = 15.sp)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Recent Entries (${savedEntries.size}/$maxEntries)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2335), modifier = Modifier.padding(bottom = 12.dp))
                        savedEntries.take(10).forEachIndexed { index, entry ->
                            val cardGradient = if (index % 2 == 0) LavenderCreamGradient else WarmWhiteGradient
                            EntryPreviewCard(
                                entry = entry,
                                allTags = allTags,
                                gradient = cardGradient,
                                onClick = { selectedEntry = entry },
                                onDelete = {
                                    entryToDelete = entry
                                    showDeleteWarning = true
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        if (selectedEntry != null) {
            FullEntryDialog(entry = selectedEntry!!, allTags = allTags, onDismiss = { selectedEntry = null })
        }

        if (showDeleteWarning) {
            AlertDialog(
                onDismissRequest = { showDeleteWarning = false; entryToDelete = null },
                title = { Text(if (entryToDelete != null) "Delete Entry?" else "Journal Limit Reached", color = if (entryToDelete != null) Color.Red else Color(0xFF1D2335)) },
                text = {
                    if (entryToDelete != null) {
                        Text("Are you sure you want to delete this entry? This action cannot be undone.")
                    } else {
                        Text("You've reached the maximum of $maxEntries journal entries. Please delete some old entries to add new ones.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (entryToDelete != null) deleteEntry(entryToDelete!!)
                        showDeleteWarning = false
                        entryToDelete = null
                    }) {
                        Text(if (entryToDelete != null) "Delete" else "OK", color = Color.Red)
                    }
                },
                dismissButton = {
                    if (entryToDelete != null) {
                        TextButton(onClick = { showDeleteWarning = false; entryToDelete = null }) { Text("Cancel") }
                    }
                }
            )
        }

        if (showAddTagDialog) {
            var isProcessing by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = {
                    showAddTagDialog = false
                    emojiInput = ""
                    newTagName = ""
                    emojiError = null
                    isProcessing = false
                },
                title = { Text("Create Custom Tag") },
                text = {
                    Column {
                        Text("Enter emoji:", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = emojiInput,
                            onValueChange = { emojiInput = it; emojiError = null },
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)).padding(16.dp),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 14.sp),
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (emojiInput.isEmpty()) Text("✨ (paste or type any emoji)", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                                    innerTextField()
                                }
                            }
                        )
                        if (emojiError != null) Text(emojiError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tag label:", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            placeholder = { Text("e.g., Motivation, Goals") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = newTagName.isBlank() && emojiInput.isNotEmpty(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                        )
                        if (emojiInput.isNotEmpty() && newTagName.isNotBlank() && isValidEmoji(emojiInput)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preview:", fontSize = 12.sp, color = Color(0xFF6B7280))
                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF3E8FF), modifier = Modifier.padding(top = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(emojiInput, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(newTagName, fontSize = 12.sp, color = Color(0xFFA855F7))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isProcessing = true
                            if (!isValidEmoji(emojiInput)) {
                                emojiError = "Please enter a valid emoji"
                                isProcessing = false
                                return@TextButton
                            }
                            if (newTagName.isBlank()) {
                                emojiError = "Please enter a tag name"
                                isProcessing = false
                                return@TextButton
                            }
                            val existingTag = findExistingTag(emojiInput)
                            if (existingTag != null) {
                                selectedTags = if (selectedTags.contains(existingTag.label)) selectedTags else selectedTags + existingTag.label
                                emojiError = "✨ Tag already exists - selected it!"
                                coroutineScope.launch {
                                    delay(1000)
                                    showAddTagDialog = false
                                    emojiInput = ""
                                    newTagName = ""
                                    emojiError = null
                                    isProcessing = false
                                }
                            } else {
                                val newTag = VibeTag(emojiInput, newTagName, true)
                                customTags = customTags + newTag
                                selectedTags = selectedTags + newTagName
                                CoroutineScope(Dispatchers.IO).launch {
                                    JournalStorage.saveCustomTags(context, userId, customTags)
                                }
                                showAddTagDialog = false
                                emojiInput = ""
                                newTagName = ""
                                emojiError = null
                                isProcessing = false
                            }
                        },
                        enabled = emojiInput.isNotEmpty() && newTagName.isNotBlank() && !isProcessing
                    ) {
                        if (isProcessing) Text("Processing...", color = Color(0xFFA855F7)) else Text("Add", color = Color(0xFFA855F7))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddTagDialog = false
                        emojiInput = ""
                        newTagName = ""
                        emojiError = null
                        isProcessing = false
                    }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    private fun VibeTagItem(tag: VibeTag, isSelected: Boolean, canRemove: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
        val borderColor = if (isSelected) Color(0xFFA855F7) else Color(0xFFE5E7EB)
        val containerColor = if (isSelected) Color(0xFFFDF4FF) else Color.White
        Box(
            modifier = Modifier
                .width(75.dp).height(90.dp)
                .background(containerColor, RoundedCornerShape(20.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
        ) {
            Box(modifier = Modifier.matchParentSize().clickable { onSelect() })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(tag.emoji, fontSize = 28.sp)
                Text(tag.label, fontSize = 11.sp, color = if (isSelected) Color(0xFFA855F7) else Color(0xFF6B7280), maxLines = 1)
            }
            if (canRemove) {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = Color.Red.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clickable { onRemove() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Remove tag", tint = Color.Red, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun EntryPreviewCard(entry: SavedJournalEntry, allTags: List<VibeTag>, gradient: Brush, onClick: () -> Unit, onDelete: () -> Unit) {
        val entryTagEmojis = allTags.filter { it.label in entry.tags }.map { it.emoji }
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Box(modifier = Modifier.background(gradient)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.date, fontSize = 12.sp, color = Color(0xFF9CA3AF))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(entry.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2335), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.content.take(80) + if (entry.content.length > 80) "..." else "", fontSize = 14.sp, color = Color(0xFF4B5563))
                    if (entryTagEmojis.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(entryTagEmojis.joinToString(" "), fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                            Text("•", fontSize = 12.sp, color = Color(0xFFA855F7))
                            Text("${entry.tags.size} tags", fontSize = 10.sp, color = Color(0xFFA855F7), modifier = Modifier.padding(start = 4.dp))
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            items(entry.tags.take(3)) { tag ->
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3E8FF).copy(alpha = 0.5f)) {
                                    Text(tag, fontSize = 9.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (entry.tags.size > 3) {
                                item { Text("+${entry.tags.size - 3}", fontSize = 9.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(start = 2.dp)) }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        Text("${entry.wordCount} words", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                    }
                }
            }
        }
    }

    @Composable
    private fun FullEntryDialog(entry: SavedJournalEntry, allTags: List<VibeTag>, onDismiss: () -> Unit) {
        val entryTagEmojis = allTags.filter { it.label in entry.tags }.map { it.emoji }
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.date, fontSize = 14.sp, color = Color(0xFF6B7280))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(entry.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2335), modifier = Modifier.padding(bottom = 8.dp))
                    if (entryTagEmojis.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF3E8FF), modifier = Modifier.padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    entryTagEmojis.forEach { emoji ->
                                        Text(emoji, fontSize = 24.sp, modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(entry.tags) { tag ->
                                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                                            Text(tag, fontSize = 12.sp, color = Color(0xFFA855F7), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(entry.content, fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF1D2335))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("${entry.wordCount} words", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen(navController: NavController) {
    JournalScreen().JournalContent(navController = navController)
}