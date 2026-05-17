package com.example.unifiedapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Unified Color Scheme (Blending Sahay & Mochan)
val PurplePrimary = Color(0xFF8B5CF6)      // Mochan's primary
val PurpleSecondary = Color(0xFFA78BFA)    // Mochan's secondary
val SageGreen = Color(0xFFA6BC9E)          // Sahay's primary
val SageLight = Color(0xFFC4D5C0)          // Sahay's secondary

val GradientPurple = Brush.linearGradient(colors = listOf(PurplePrimary, PurpleSecondary))
val GradientSage = Brush.linearGradient(colors = listOf(SageGreen, SageLight))

val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val BackgroundLight = Color(0xFFF5F3FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedDashboardScreen(
    navController: NavController,
    userName: String
) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Welcome, $userName!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "How are you feeling today?",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("profile")
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = PurplePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(PurplePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Mental Wellness Check",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Take a quick assessment to understand your mental health",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Assessment Options Title
            Text(
                text = "Choose Assessment",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Anxiety Assessment Card (Sahay - Sage Green theme)
            AssessmentCard(
                title = "Anxiety Assessment",
                subtitle = "GAD-7 Questionnaire • 7 questions • 2 minutes",
                icon = Icons.Default.Psychology,
                gradientColors = listOf(SageGreen, SageLight),
                onClick = {
                    navController.navigate("anxiety_assessment")
                }
            )

            // Depression Assessment Card (Mochan - Purple theme)
            AssessmentCard(
                title = "Depression Assessment",
                subtitle = "PHQ-9 Questionnaire • 9 questions • 3 minutes",
                icon = Icons.Default.Favorite,
                gradientColors = listOf(PurplePrimary, PurpleSecondary),
                onClick = {
                    navController.navigate("depression_assessment")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wellness Tools Section
            Text(
                text = "Wellness Tools",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Wellness Tools Grid - Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WellnessToolCard(
                    title = "Breathing",
                    icon = Icons.Default.SelfImprovement,
                    color = Color(0xFF10B981),
                    onClick = { navController.navigate("breathing") },
                    modifier = Modifier.weight(1f)
                )
                WellnessToolCard(
                    title = "Sounds",
                    icon = Icons.Default.MusicNote,
                    color = Color(0xFF3B82F6),
                    onClick = { navController.navigate("sounds") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Wellness Tools Grid - Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WellnessToolCard(
                    title = "Mood Tracker",
                    icon = Icons.Default.Mood,
                    color = Color(0xFFF59E0B),
                    onClick = { navController.navigate("mood_tracker") },
                    modifier = Modifier.weight(1f)
                )
                WellnessToolCard(
                    title = "Journal",
                    icon = Icons.Default.Edit,
                    color = Color(0xFF8B5CF6),
                    onClick = { navController.navigate("journal") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Wellness Tools Grid - Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WellnessToolCard(
                    title = "Grounding",
                    icon = Icons.Default.Spa,
                    color = Color(0xFF22D3EE),
                    onClick = { navController.navigate("grounding") },
                    modifier = Modifier.weight(1f)
                )
                WellnessToolCard(
                    title = "History",
                    icon = Icons.Default.History,
                    color = Color(0xFFF97316),
                    onClick = { navController.navigate("assessment_history") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AssessmentCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
        }
    }
}

@Composable
fun WellnessToolCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}