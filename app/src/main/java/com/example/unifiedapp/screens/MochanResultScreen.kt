package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MochanResultScreen(
    navController: NavController,
    score: Int
) {
    val severity = when {
        score <= 4 -> "Minimal depression"
        score <= 9 -> "Mild depression"
        score <= 14 -> "Moderate depression"
        score <= 19 -> "Moderately severe depression"
        else -> "Severe depression"
    }

    val severityColor = when {
        score <= 4 -> Color(0xFF10B981)
        score <= 9 -> Color(0xFF34D399)
        score <= 14 -> Color(0xFFF59E0B)
        score <= 19 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F3FF),
                        Color(0xFFEDE9FE)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌿", fontSize = 48.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Mochan Assessment Complete",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Your PHQ-9 Score", fontSize = 16.sp, color = Color.Gray)
                        Text("$score/27", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = severityColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = severityColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                severity,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = severityColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Recommendations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val recommendations = when {
                            score <= 9 -> listOf(
                                "🌿 Practice daily mindfulness or meditation",
                                "🏃 Maintain regular exercise and healthy routine",
                                "📓 Keep a mood journal to track your feelings",
                                "👪 Connect with friends and family regularly"
                            )
                            score <= 18 -> listOf(
                                "🏥 Consider speaking with a mental health professional",
                                "💬 Build a support network you can trust",
                                "📋 Create a consistent daily routine",
                                "📊 Track your symptoms to identify patterns"
                            )
                            else -> listOf(
                                "🆘 Seek professional help immediately",
                                "📞 Call a mental health helpline (988)",
                                "👥 Reach out to someone you trust",
                                "⚕️ Discuss treatment options with a provider"
                            )
                        }

                        recommendations.forEach { rec ->
                            Text(rec, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = severityColor)
                ) {
                    Text("Return Home", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}