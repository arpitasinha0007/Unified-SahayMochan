package com.example.unifiedapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unifiedapp.navigation.Screen
import com.example.unifiedapp.utils.MoodTrackerHelper
import com.example.unifiedapp.utils.MoodTrackerViewModel
import com.example.unifiedapp.utils.MoodType
import com.example.unifiedapp.utils.MoodCategory
import com.example.unifiedapp.utils.MoodEntry
import com.example.unifiedapp.utils.GraphDataPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.lerp

// Theme Colors
val GlassWhiteMood = Color.White.copy(alpha = 0.4f)
val GlassBorderMood = Color.White.copy(alpha = 0.3f)
val CardWhiteMood = Color.White.copy(alpha = 0.95f)
val TextPrimaryMood = Color(0xFF1D2335)
val TextSoftMood = Color(0xFF4B5563)
val TextLightMood = Color(0xFF6B7280)

// Gradient for mood tracker
val MoodGradientMood = Brush.linearGradient(
    colors = listOf(Color(0xFF34D399), Color(0xFF99F6E4))
)

// Helper function to get color based on happiness level
@Composable
fun getHappinessColorMood(level: Int): Color {
    val brightBlue = Color(0xFF2979FF)
    val cyan = Color(0xFF00E5FF)
    val teal = Color(0xFF1DE9B6)
    val oceanGreen = Color(0xFF2ECC9A)
    val deepOceanGreen = Color(0xFF00BFA5)

    val fraction = (level - 1) / 9f

    return when {
        fraction < 0.25f -> {
            lerp(brightBlue, cyan, fraction / 0.25f)
        }
        fraction < 0.5f -> {
            lerp(cyan, teal, (fraction - 0.25f) / 0.25f)
        }
        fraction < 0.75f -> {
            lerp(teal, oceanGreen, (fraction - 0.5f) / 0.25f)
        }
        else -> {
            lerp(oceanGreen, deepOceanGreen, (fraction - 0.75f) / 0.25f)
        }
    }
}

// Helper function to get color based on energy level
@Composable
fun getEnergyColorMood(level: Float): Color {
    val red = Color(0xFFFF3B30)
    val orange = Color(0xFFFF8C00)
    val amber = Color(0xFFFFB300)

    return when {
        level < 0.5f -> {
            lerp(red, orange, level / 0.5f)
        }
        else -> {
            lerp(orange, amber, (level - 0.5f) / 0.5f)
        }
    }
}

@Composable
fun MoodTrackerScreen(
    onBack: () -> Unit = {},
    navController: NavController? = null
) {
    val handleBack = {
        if (navController != null) {
            navController.navigate(Screen.WELLNESS) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            onBack()
        }
    }

    val context = LocalContext.current
    val helper = remember { MoodTrackerHelper(context) }
    val viewModel = remember { MoodTrackerViewModel(helper) }

    val entries by viewModel.entries.collectAsState()
    val graphData by remember { derivedStateOf { viewModel.getLast30DaysGraphData() } }
    val categoryDistribution by remember { derivedStateOf { viewModel.getMoodCategoryDistribution() } }
    val typeDistribution by remember { derivedStateOf { viewModel.getMoodTypeDistribution() } }

    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var note by remember { mutableStateOf("") }
    var energyLevel by remember { mutableStateOf(0.5f) }
    var happinessLevel by remember { mutableStateOf(5) }
    var selectedReasons by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val reasons = listOf("Rested", "Stressed", "Lonely", "Productive", "Overwhelmed",
        "Loved", "Exercise", "Work", "Family", "Friends")

    val isTodayLogged = viewModel.todayLogged

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
            selectedMood = null
            note = ""
            energyLevel = 0.5f
            happinessLevel = 5
            selectedReasons = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GlassHeaderMood(onBack = handleBack)

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MoodInputCardMood(
                    streak = viewModel.streak,
                    isTodayLogged = isTodayLogged,
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it },
                    happinessLevel = happinessLevel,
                    onHappinessChange = { happinessLevel = it },
                    energyLevel = energyLevel,
                    onEnergyChange = { energyLevel = it },
                    selectedReasons = selectedReasons,
                    onReasonToggle = { reason ->
                        selectedReasons = if (selectedReasons.contains(reason)) {
                            selectedReasons - reason
                        } else {
                            selectedReasons + reason
                        }
                    },
                    note = note,
                    onNoteChange = { note = it },
                    reasons = reasons,
                    onSave = {
                        if (selectedMood != null) {
                            val entry = MoodEntry(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                mood = selectedMood!!,
                                note = note,
                                reasons = selectedReasons,
                                energy = energyLevel,
                                happinessLevel = happinessLevel
                            )
                            viewModel.saveMoodEntry(entry)
                            showSuccessMessage = true
                        }
                    }
                )

                if (entries.isNotEmpty()) {
                    QuickStatsCardMood(
                        totalEntries = entries.size,
                        averageHappiness = viewModel.averageHappiness,
                        trend = viewModel.trend
                    )

                    HappinessGraphCardMood(graphData = graphData)

                    MoodDistributionCardMood(
                        categoryDistribution = categoryDistribution,
                        typeDistribution = typeDistribution
                    )

                    RecentHistoryCardMood(entries = entries.take(5))
                } else {
                    EmptyStateCardMood()
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showSuccessMessage) {
            SuccessOverlayMood()
        }
    }
}

@Composable
fun GlassHeaderMood(onBack: () -> Unit) {
    val milkyWhite = Color.White.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = milkyWhite,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF99F6E4)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = "Mood Tracker",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Mood Tracker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    "Track your emotional journey",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============ THE REST OF THE COMPOSABLES REMAIN EXACTLY AS THEY WERE ============
// (MoodInputCardMood, MoodChipMood, QuickStatsCardMood, HappinessGraphCardMood, etc.)
// They are omitted here for brevity but must be kept identical to your original file.
// Make sure to copy them from your current working version.
@Composable
fun MoodInputCardMood(
    streak: Int,
    isTodayLogged: Boolean,
    selectedMood: MoodType?,
    onMoodSelected: (MoodType) -> Unit,
    happinessLevel: Int,
    onHappinessChange: (Int) -> Unit,
    energyLevel: Float,
    onEnergyChange: (Float) -> Unit,
    selectedReasons: List<String>,
    onReasonToggle: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    reasons: List<String>,
    onSave: () -> Unit
) {
    val cyanWhiteGradient = Brush.linearGradient(
        colors = listOf(
            Color.White,
            Color(0xFFE0F2FE),
            Color(0xFFF0F9FF)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cyanWhiteGradient)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "How are you feeling today?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryMood
                    )

                    if (streak > 0) {
                        Surface(
                            shape = RoundedCornerShape(30.dp),
                            color = Color(0xFFFFB74D).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$streak day streak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                if (!isTodayLogged) {
                    Text("Select your mood", fontSize = 14.sp, color = TextSoftMood)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MoodType.values().toList()) { mood ->
                            MoodChipMood(
                                mood = mood,
                                isSelected = selectedMood == mood,
                                onClick = { onMoodSelected(mood) }
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Happiness Level", color = TextSoftMood)
                            Text(
                                "$happinessLevel/10",
                                fontWeight = FontWeight.Bold,
                                color = getHappinessColorMood(happinessLevel)
                            )
                        }
                        Slider(
                            value = happinessLevel.toFloat(),
                            onValueChange = { onHappinessChange(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = getHappinessColorMood(happinessLevel),
                                activeTrackColor = getHappinessColorMood(happinessLevel)
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Energy Level", color = TextSoftMood)
                            Text(
                                when {
                                    energyLevel < 0.33 -> "Low 😴"
                                    energyLevel < 0.66 -> "Medium 🙂"
                                    else -> "High ⚡"
                                },
                                fontWeight = FontWeight.Bold,
                                color = getEnergyColorMood(energyLevel)
                            )
                        }
                        Slider(
                            value = energyLevel,
                            onValueChange = onEnergyChange,
                            colors = SliderDefaults.colors(
                                thumbColor = getEnergyColorMood(energyLevel),
                                activeTrackColor = getEnergyColorMood(energyLevel)
                            )
                        )
                    }

                    Text("What influenced your mood?", fontSize = 14.sp, color = TextSoftMood)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reasons) { reason ->
                            val isSelected = selectedReasons.contains(reason)
                            Surface(
                                modifier = Modifier
                                    .clickable { onReasonToggle(reason) }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF34D399).copy(alpha = 0.1f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF34D399) else TextLightMood.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            Modifier.size(14.dp),
                                            tint = Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        reason,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFF34D399) else TextSoftMood
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        placeholder = { Text("Add a note...", color = TextLightMood) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = TextLightMood.copy(alpha = 0.3f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = onSave,
                        enabled = selectedMood != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMood != null) Color(0xFF34D399) else Color.LightGray
                        )
                    ) {
                        Text("Log Today's Mood", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Today's mood logged!", color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodChipMood(mood: MoodType, isSelected: Boolean, onClick: () -> Unit) {
    val moodColor = MoodTrackerHelper.parseColor(mood.colorHex)

    Surface(
        modifier = Modifier
            .width(70.dp)
            .height(80.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) moodColor.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(2.dp, if (isSelected) moodColor else TextLightMood.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(mood.emoji, fontSize = 28.sp)
            Text(
                mood.label,
                fontSize = 10.sp,
                color = if (isSelected) moodColor else TextSoftMood,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun QuickStatsCardMood(
    totalEntries: Int,
    averageHappiness: Float,
    trend: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhiteMood),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅", fontSize = 24.sp)
                Text("$totalEntries", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimaryMood)
                Text("Total Days", fontSize = 12.sp, color = TextLightMood)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😊", fontSize = 24.sp)
                Text(
                    String.format("%.1f", averageHappiness),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
                Text("Avg Happiness", fontSize = 12.sp, color = TextLightMood)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 24.sp)
                Text(trend, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryMood)
                Text("Trend", fontSize = 12.sp, color = TextLightMood)
            }
        }
    }
}

@Composable
fun HappinessGraphCardMood(graphData: List<GraphDataPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhiteMood),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Last 30 Days Happiness",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryMood
                )
                Icon(Icons.Default.ShowChart, null, tint = Color(0xFF34D399))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (graphData.all { it.value == 0f }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", color = TextLightMood)
                }
            } else {
                HappinessBarGraphMood(data = graphData)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItemMood(color = Color(0xFF4ADE80), label = "Happy")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItemMood(color = Color(0xFF22D3EE), label = "Neutral")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItemMood(color = Color(0xFFFB7185), label = "Low")
            }
        }
    }
}

@Composable
fun HappinessBarGraphMood(data: List<GraphDataPoint>) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp - 72.dp
    val barWidth = screenWidth / 30f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val barHeight = size.height - 20.dp.toPx()
            val barWidthPx = barWidth.toPx()

            data.forEachIndexed { index, point ->
                val x = index * barWidthPx
                val height = (point.value / 10f) * barHeight

                if (height > 0) {
                    drawRoundRect(
                        color = MoodTrackerHelper.parseColor(point.colorHex),
                        topLeft = Offset(x, barHeight - height + 10.dp.toPx()),
                        size = Size(barWidthPx - 2.dp.toPx(), height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 5, 10, 15, 20, 25, 29).forEach { index ->
                Text(
                    text = if (index == 29) "Today" else "${30 - index}d",
                    fontSize = 8.sp,
                    color = TextLightMood
                )
            }
        }
    }
}

@Composable
fun MoodDistributionCardMood(
    categoryDistribution: Map<MoodCategory, Int>,
    typeDistribution: Map<MoodType, Int>
) {
    val total = categoryDistribution.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhiteMood),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Mood Distribution (30 days)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryMood
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", color = TextLightMood)
                }
            } else {
                MoodCategory.values().forEach { category ->
                    val count = categoryDistribution[category] ?: 0
                    val percentage = if (total > 0) (count * 100f / total) else 0f

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${category.emoji} ${category.name}", color = TextSoftMood)
                            Text("$count days", fontWeight = FontWeight.Bold, color = TextPrimaryMood)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(TextLightMood.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage / 100f)
                                    .height(8.dp)
                                    .background(
                                        MoodTrackerHelper.parseColor(category.colorHex),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (typeDistribution.isNotEmpty()) {
                    Text(
                        "Most frequent moods:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSoftMood
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    typeDistribution.toList()
                        .sortedByDescending { it.second }
                        .take(3)
                        .forEach { (mood, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mood.emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mood.label, color = TextSoftMood)
                                }
                                Text("$count days", fontWeight = FontWeight.Bold, color = TextPrimaryMood)
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun RecentHistoryCardMood(entries: List<MoodEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhiteMood),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Recent History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryMood
            )

            Spacer(modifier = Modifier.height(12.dp))

            entries.forEachIndexed { index, entry ->
                HistoryItemMood(entry = entry)
                if (index < entries.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = TextLightMood.copy(alpha = 0.2f)
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent entries", color = TextLightMood)
                }
            }
        }
    }
}

@Composable
fun HistoryItemMood(entry: MoodEntry) {
    val moodColor = MoodTrackerHelper.parseColor(entry.mood.colorHex)
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(moodColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.mood.emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.date)
                    dateFormat.format(date)
                } catch (e: Exception) {
                    entry.date
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryMood
            )
            if (entry.note.isNotEmpty()) {
                Text(
                    text = entry.note,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = TextSoftMood
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.happinessLevel}/10",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = moodColor
            )
            Text(
                text = "⚡${(entry.energy * 100).toInt()}%",
                fontSize = 10.sp,
                color = TextLightMood
            )
        }
    }
}

@Composable
fun EmptyStateCardMood() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhiteMood),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.FavoriteBorder,
                null,
                modifier = Modifier.size(64.dp),
                tint = TextLightMood.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No mood entries yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryMood
            )
            Text(
                "Start tracking your mood above",
                fontSize = 14.sp,
                color = TextSoftMood,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SuccessOverlayMood() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xAA34D399), Color.Transparent),
                        radius = 500f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFF34D399)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Mood Logged Successfully!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LegendItemMood(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextLightMood)
    }
}