package com.example.unifiedapp.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
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
import com.example.unifiedapp.navigation.Screen
import kotlinx.coroutines.delay

enum class BreathingPhase(val label: String, val duration: Int) {
    IDLE("Ready to Begin", 0),
    INHALE("Breathe In", 4),
    HOLD("Hold", 7),
    EXHALE("Breathe Out", 8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingScreen(
    onBack: () -> Unit = {},
    navController: NavController? = null
) {
    var isActive by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(BreathingPhase.IDLE) }
    var cycleCount by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(0) }

    val scaleAnim = remember { Animatable(1f) }

    // Logic: Timer and Phase Switching
    LaunchedEffect(isActive, timeLeft, phase) {
        if (isActive && phase != BreathingPhase.IDLE) {
            if (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            } else {
                when (phase) {
                    BreathingPhase.INHALE -> {
                        phase = BreathingPhase.HOLD
                        timeLeft = BreathingPhase.HOLD.duration
                    }
                    BreathingPhase.HOLD -> {
                        phase = BreathingPhase.EXHALE
                        timeLeft = BreathingPhase.EXHALE.duration
                    }
                    BreathingPhase.EXHALE -> {
                        cycleCount += 1
                        phase = BreathingPhase.INHALE
                        timeLeft = BreathingPhase.INHALE.duration
                    }
                    else -> {}
                }
            }
        }
    }

    // Logic: Animation Scaling
    LaunchedEffect(phase) {
        val targetScale = when (phase) {
            BreathingPhase.INHALE -> 1.5f
            BreathingPhase.EXHALE -> 0.7f
            else -> 1.0f
        }
        scaleAnim.animateTo(
            targetValue = targetScale,
            animationSpec = tween(
                durationMillis = if (phase == BreathingPhase.IDLE) 0 else 1000,
                easing = LinearEasing
            )
        )
    }

    // Handle back button – navigate to wellness screen if navController is provided
    val handleBack = {
        if (navController != null) {
            navController.navigate(Screen.WELLNESS) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            onBack()
        }
    }

    val gradientBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE))
    )

    val milkyWhite = Color.White.copy(alpha = 0.4f)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
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
                    // Back button with updated behavior
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icon with gradient background – fixed modifier chain
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title and subtitle
                    Column {
                        Text(
                            "4-7-8 Breathing",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Relaxation technique",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 24.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 120.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Breathing Circle Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = phase.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Box(
                            modifier = Modifier.size(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(256.dp * scaleAnim.value)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.1f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(192.dp * scaleAnim.value)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(128.dp * scaleAnim.value)
                                    .clip(CircleShape)
                                    .background(gradientBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$timeLeft",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Cycles Completed", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = "$cycleCount",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0EA5E9)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isActive && phase == BreathingPhase.IDLE) {
                            Button(
                                onClick = {
                                    isActive = true
                                    phase = BreathingPhase.INHALE
                                    timeLeft = BreathingPhase.INHALE.duration
                                    cycleCount = 0
                                },
                                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(gradientBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Exercise", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = { isActive = !isActive },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    border = BorderStroke(2.dp, Color.LightGray)
                                ) {
                                    Text(if (isActive) "Pause" else "Resume", color = Color.DarkGray)
                                }
                                OutlinedButton(
                                    onClick = {
                                        isActive = false
                                        phase = BreathingPhase.IDLE
                                        timeLeft = 0
                                        cycleCount = 0
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    border = BorderStroke(2.dp, Color.LightGray)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.DarkGray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Instructions Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("How It Works", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(16.dp))

                        InstructionRow("4", "Breathe in through your nose", Color(0xFFDBEAFE), Color(0xFF2563EB))
                        InstructionRow("7", "Hold your breath comfortably", Color(0xFFF3E8FF), Color(0xFF9333EA))
                        InstructionRow("8", "Exhale slowly through your mouth", Color(0xFFCFFAFE), Color(0xFF0891B2))
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionRow(number: String, text: String, bgColor: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF374151))
    }
}