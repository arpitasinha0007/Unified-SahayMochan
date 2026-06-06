package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.navigation.NavController
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Spa
import com.example.unifiedapp.navigation.Screen

@Composable
fun WellnessScreen(navController: NavController) {
    val TipBubbleShape = GenericShape { size, _ ->
        val cornerRadius = 60f
        val triangleWidth = 40f
        val triangleHeight = 30f
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 0f, top = 0f,
                right = size.width, bottom = size.height - 10f,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        )
        moveTo(size.width - cornerRadius - 80f, size.height - 10f)
        lineTo(size.width - cornerRadius - 40f, size.height + triangleHeight)
        lineTo(size.width - cornerRadius, size.height - 10f)
        close()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.4f))
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF29B6F6), Color(0xFF26C6DA))),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Heart",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Wellness Hub",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1D2335)
                        )
                        Text(
                            text = "Tools to help you find calm",
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WellnessToolCard(
                    title = "5-4-3-2-1 Grounding",
                    subtitle = "Anchor yourself in the present",
                    icon = Icons.Outlined.Spa,
                    iconBg = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF2C94C), Color(0xFFF6A97A), Color(0xFFF27AA5), Color(0xFFE843C4)
                        )
                    ),
                    containerColor = Color(0xFFFFF0F6).copy(alpha = 0.85f),
                    borderColor = Color(0xFFF59DBB),
                    onClick = { navController.navigate(Screen.GROUNDING) }
                )
                WellnessToolCard(
                    title = "4-7-8 Breathing",
                    subtitle = "Calming breathing technique",
                    icon = Icons.Default.SelfImprovement,
                    iconBg = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))),
                    containerColor = Color(0xFFE0F2FE).copy(alpha = 0.7f),
                    borderColor = Color(0xFF7DD3FC),
                    onClick = { navController.navigate(Screen.BREATHING) }
                )
                WellnessToolCard(
                    title = "Calming Sounds",
                    subtitle = "Nature sounds & ambient music",
                    icon = Icons.Default.MusicNote,
                    iconBg = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))),
                    containerColor = Color(0xFFF5F3FF).copy(alpha = 0.7f),
                    borderColor = Color(0xFFC4B5FD),
                    onClick = { navController.navigate(Screen.SOUNDS) }
                )
                WellnessToolCard(
                    title = "Mood Tracker",
                    subtitle = "Daily emotional check-in",
                    icon = Icons.Default.SentimentSatisfiedAlt,
                    iconBg = Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF99F6E4))),
                    containerColor = Color(0xFFECFDF5).copy(alpha = 0.7f),
                    borderColor = Color(0xFFA7F3D0),
                    onClick = { navController.navigate(Screen.MOOD_TRACKER) } // Fixed route
                )
                WellnessToolCard(
                    title = "Journal",
                    subtitle = "Write your thoughts",
                    icon = Icons.Outlined.Book,
                    iconBg = Brush.linearGradient(listOf(Color(0xFFAB47BC), Color(0xFF7E57C2))),
                    containerColor = Color(0xFFF3E8FF).copy(alpha = 0.7f),
                    borderColor = Color(0xFFC084FC),
                    onClick = { navController.navigate(Screen.JOURNAL) }
                )

                // Wellness Tip Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = TipBubbleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFBBF24), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Stars, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                            Text(
                                text = "Wellness Tip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1D2335)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Taking just 5 minutes for deep breathing can reset your nervous system and lower cortisol levels.",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(140.dp))
            }
        }
    }
}

@Composable
private fun WellnessToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Brush,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1D2335))
                Text(text = subtitle, fontSize = 13.sp, color = Color(0xFF6B7280))
            }
        }
    }
}