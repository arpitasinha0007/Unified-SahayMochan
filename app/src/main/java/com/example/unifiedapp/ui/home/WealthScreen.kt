package com.example.unifiedapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.unifiedapp.theme.*

data class WealthCategory(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val cardBgColor: Color,
    val lightColor: Color,
    val items: List<WealthMenuItem>
)

data class WealthMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val cardBgColor: Color,
    val route: String
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WealthMenuScreen(
    onClose: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToAssessmentHistory: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToSounds: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToGrounding: () -> Unit
) {
    // Pleasant, warm colors that complement sage green
    // Warm Sunset theme for Mindfulness
    val warmCoral = Color(0xFFFF9A8B)        // Soft coral
    val warmPeach = Color(0xFFFFB6A0)        // Warm peach
    val lightPeach = Color(0xFFFFF0EA)       // Very light peach for cards

    // Fresh Meadow theme for Tracking
    val softMint = Color(0xFFA8E6CF)         // Soft mint green
    val freshSage = Color(0xFFB8D9C6)        // Fresh sage
    val lightMint = Color(0xFFF0FAF5)        // Very light mint for cards

    // Happy Lavender theme for Health
    val happyLavender = Color(0xFFDAB6FF)     // Happy lavender
    val softLilac = Color(0xFFE5C5FF)         // Soft lilac
    val lightLavender = Color(0xFFFAF0FF)     // Very light lavender for cards

    // Organize wellness tools by category with pleasant colors
    val categories = listOf(
        // MINDFULNESS CATEGORY - Warm Sunset theme
        WealthCategory(
            title = "Mindfulness",
            icon = Icons.Outlined.SelfImprovement,
            accentColor = warmCoral,
            cardBgColor = lightPeach,
            lightColor = warmPeach,
            items = listOf(
                WealthMenuItem(
                    id = "breathing",
                    title = "4-7-8 Breathing",
                    subtitle = "Calming technique",
                    icon = Icons.Outlined.SelfImprovement,
                    iconBgColor = warmCoral,
                    cardBgColor = lightPeach,
                    route = "breathing"
                ),
                WealthMenuItem(
                    id = "sounds",
                    title = "Calming Sounds",
                    subtitle = "Nature & ambient",
                    icon = Icons.Outlined.MusicNote,
                    iconBgColor = warmCoral,
                    cardBgColor = lightPeach,
                    route = "sounds"
                ),
                WealthMenuItem(
                    id = "grounding",
                    title = "Grounding",
                    subtitle = "5-4-3-2-1 technique",
                    icon = Icons.Outlined.Spa,
                    iconBgColor = warmCoral,
                    cardBgColor = lightPeach,
                    route = "grounding"
                )
            )
        ),

        // TRACKING CATEGORY - Fresh Meadow theme
        WealthCategory(
            title = "Tracking",
            icon = Icons.Outlined.Timeline,
            accentColor = softMint,
            cardBgColor = lightMint,
            lightColor = freshSage,
            items = listOf(
                WealthMenuItem(
                    id = "mood",
                    title = "Mood Tracker",
                    subtitle = "Daily check-in",
                    icon = Icons.Outlined.SentimentSatisfiedAlt,
                    iconBgColor = softMint,
                    cardBgColor = lightMint,
                    route = "mood_flow"
                ),
                WealthMenuItem(
                    id = "journal",
                    title = "Journal",
                    subtitle = "Write thoughts",
                    icon = Icons.Outlined.Book,
                    iconBgColor = softMint,
                    cardBgColor = lightMint,
                    route = "journal"
                ),
                WealthMenuItem(
                    id = "history",
                    title = "Assessment History",
                    subtitle = "View past results",
                    icon = Icons.Outlined.History,
                    iconBgColor = softMint,
                    cardBgColor = lightMint,
                    route = "assessment_history"
                )
            )
        ),

        // HEALTH CATEGORY - Happy Lavender theme
        WealthCategory(
            title = "Health",
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = happyLavender,
            cardBgColor = lightLavender,
            lightColor = softLilac,
            items = listOf(
                WealthMenuItem(
                    id = "water",
                    title = "Water Intake",
                    subtitle = "Stay hydrated",
                    icon = Icons.Outlined.WaterDrop,
                    iconBgColor = happyLavender,
                    cardBgColor = lightLavender,
                    route = "water_intake_screen"
                ),
                WealthMenuItem(
                    id = "sleep",
                    title = "Sleep Quality",
                    subtitle = "Track your rest",
                    icon = Icons.Outlined.Bed,
                    iconBgColor = happyLavender,
                    cardBgColor = lightLavender,
                    route = "sleep_quality_screen"
                )
            )
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPurpleBg)  // Replaced BackgroundGradient
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with warm gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Wellness Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Your bright path to wellbeing ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,  // Changed from TextMedium
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(categories.size) { categoryIndex ->
                    val category = categories[categoryIndex]

                    // Category Header with pleasant design
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category icon with warm gradient background
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            category.accentColor,
                                            category.lightColor
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = category.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            style = LocalTextStyle.current.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        category.accentColor,
                                        category.lightColor
                                    )
                                )
                            )
                        )
                    }

                    // Display items in rows of 3
                    val itemsChunked = category.items.chunked(3)

                    itemsChunked.forEachIndexed { chunkIndex, rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                PleasantWellnessCard(
                                    item = item,
                                    onClick = {
                                        when (item.id) {
                                            "water" -> onNavigateToWater()
                                            "sleep" -> onNavigateToSleep()
                                            "mood" -> onNavigateToMood()
                                            "history" -> onNavigateToAssessmentHistory()
                                            "breathing" -> onNavigateToBreathing()
                                            "sounds" -> onNavigateToSounds()
                                            "journal" -> onNavigateToJournal()
                                            "grounding" -> onNavigateToGrounding()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Happy separator between categories
                    if (categoryIndex < categories.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .padding(horizontal = 2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index) {
                                                0 -> warmCoral
                                                1 -> softMint
                                                else -> happyLavender
                                            }.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cheerful close button with gradient
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                warmCoral.copy(alpha = 0.3f),
                                softMint.copy(alpha = 0.3f),
                                happyLavender.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        spotColor = warmCoral.copy(alpha = 0.2f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Pleasant card design with cheerful touches
@Composable
fun PleasantWellnessCard(
    item: WealthMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .height(170.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .shadow(
                elevation = if (isPressed) 4.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = item.iconBgColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.cardBgColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            item.cardBgColor,
                            item.cardBgColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon with pleasant gradient
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    item.iconBgColor,
                                    item.iconBgColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = item.iconBgColor.copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title with subtle gradient
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,  // Changed from TextMedium
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            // Cute little decoration dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.iconBgColor.copy(alpha = 0.3f))
            )
        }
    }
}

// ==========================================
// WRAPPER FUNCTION
// ==========================================
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(
    onClose: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToAssessmentHistory: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToSounds: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToGrounding: () -> Unit
) {
    WealthMenuScreen(
        onClose = onClose,
        onNavigateToWater = onNavigateToWater,
        onNavigateToSleep = onNavigateToSleep,
        onNavigateToMood = onNavigateToMood,
        onNavigateToAssessmentHistory = onNavigateToAssessmentHistory,
        onNavigateToBreathing = onNavigateToBreathing,
        onNavigateToSounds = onNavigateToSounds,
        onNavigateToJournal = onNavigateToJournal,
        onNavigateToGrounding = onNavigateToGrounding
    )
}