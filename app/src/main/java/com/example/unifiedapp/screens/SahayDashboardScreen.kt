package com.example.unifiedapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.R
import com.example.unifiedapp.ui.theme.*

@Composable
fun SahayDashboardScreen(
    navController: NavController
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SahayGradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mochan_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Sahay",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SahayCharcoal
                    )
                    Text(
                        "Anxiety Assessment",
                        fontSize = 14.sp,
                        color = SahayMutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Assessment Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("sahay_consent")
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SahaySageAccent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MonitorHeart,
                            contentDescription = "Start",
                            tint = SahaySageAccent,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Start Anxiety Assessment",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SahayCharcoal
                    )
                    Text(
                        "GAD-7 Questionnaire • 7 questions",
                        fontSize = 14.sp,
                        color = SahayMutedSlate
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "• Camera required for AI analysis",
                        fontSize = 12.sp,
                        color = SahayMutedSlate
                    )
                    Text(
                        "• Takes 3-4 minutes",
                        fontSize = 12.sp,
                        color = SahayMutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wellness Tools
            Text(
                "Wellness Tools",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SahayCharcoal,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Wellness Cards
            SahayWellnessToolCard(
                title = "Breathing Exercises",
                icon = Icons.Default.SelfImprovement,
                onClick = { navController.navigate("sahay_breathing") }
            )
            SahayWellnessToolCard(
                title = "Calming Sounds",
                icon = Icons.Default.MusicNote,
                onClick = { navController.navigate("sahay_sounds") }
            )
            SahayWellnessToolCard(
                title = "Mood Tracker",
                icon = Icons.Default.Favorite,
                onClick = { navController.navigate("sahay_mood_tracker") }
            )
            SahayWellnessToolCard(
                title = "Journal",
                icon = Icons.Default.Edit,
                onClick = { navController.navigate("sahay_journal") }
            )
            SahayWellnessToolCard(
                title = "Grounding",
                icon = Icons.Default.Nature,
                onClick = { navController.navigate("sahay_grounding") }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SahayWellnessToolCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SahaySageAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SahaySageAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SahayCharcoal)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SahayMutedSlate)
        }
    }
}