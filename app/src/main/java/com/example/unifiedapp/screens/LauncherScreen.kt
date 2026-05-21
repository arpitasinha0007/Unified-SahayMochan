package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.data.UserSessionManager
import com.example.unifiedapp.navigation.Screen

@Composable
fun LauncherScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val user = sessionManager.getUser()
    val isLoggedIn = user?.isLoggedIn == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F3FF),
                        Color(0xFFE9D5FF),
                        Color(0xFFD8B4FE)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile/Login Button at Top Right
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Card(
                    modifier = Modifier.wrapContentSize().clickable {
                        if (isLoggedIn) {
                            navController.navigate(Screen.DASHBOARD)
                        } else {
                            navController.navigate(Screen.AUTH)
                        }
                    },
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
                            contentDescription = if (isLoggedIn) "Profile" else "Login",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLoggedIn) user?.name?.take(15) ?: "Profile" else "Login",
                            fontSize = 13.sp,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // App Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Unified Mental Health",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Text(
                text = "Choose your assessment",
                fontSize = 16.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sahay Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { navController.navigate(Screen.SAHAY_CONSENT) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7F3))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF6B9071)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Spa, contentDescription = "Sahay", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sahay", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E4E42))
                        Text("Anxiety Assessment (GAD-7)", fontSize = 14.sp, color = Color(0xFF5D6D66))
                        Text("7 questions • AI facial analysis • Green theme", fontSize = 12.sp, color = Color(0xFF6B9071))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF6B9071), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mochan Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { navController.navigate(Screen.MOCHAN_CONSENT) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Mochan", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mochan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("Depression Assessment (PHQ-9)", fontSize = 14.sp, color = Color(0xFF6B7280))
                        Text("9 questions • AI facial analysis • Purple theme", fontSize = 12.sp, color = Color(0xFF8B5CF6))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text("Your privacy is our priority", fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }
    }
}