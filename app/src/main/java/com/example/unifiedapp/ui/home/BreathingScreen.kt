package com.example.unifiedapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Updated color palette with blueish accent throughout
val BlueLight = Color(0xFFE3F2FD)      // Very light blue
val BlueMedium = Color(0xFFBBDEFB)     // Medium light blue
val BlueAccent = Color(0xFF2196F3)     // Material Blue
val BlueDark = Color(0xFF1976D2)       // Dark blue
val BlueDeep = Color(0xFF0D47A1)       // Deep blue for text/contrast

// Sky blue accent for breathing
val SkyBlueLight = Color(0xFFE1F5FE)
val SkyBlueAccent = Color(0xFF03A9F4)
val SkyBlueDark = Color(0xFF0288D1)

data class BreathState(
    val instruction: String,
    val scale: Float,
    val opacity: Float
)

@Composable
fun BreathingScreen(onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_cycle")

    // 19-second cycle (4-7-8)
    val totalCycleTime = 19000
    val millis by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalCycleTime.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(totalCycleTime, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_millis"
    )

    val state = remember(millis) {
        when {
            millis < 4000 -> {
                val progress = millis / 4000
                BreathState(
                    instruction = "Inhale",
                    scale = 0.8f + (0.4f * progress),
                    opacity = 0.1f + (0.1f * progress)
                )
            }
            millis < 11000 -> {
                BreathState(
                    instruction = "Hold",
                    scale = 1.2f,
                    opacity = 0.2f
                )
            }
            else -> {
                val progress = (millis - 11000) / 8000
                BreathState(
                    instruction = "Exhale",
                    scale = 1.2f - (0.4f * progress),
                    opacity = 0.2f - (0.1f * progress)
                )
            }
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Updated background with blue gradient throughout
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BlueLight,
                            BlueMedium,
                            BlueAccent.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(padding)
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = BlueDeep.copy(alpha = 0.7f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Text(
                    text = "4-7-8 Breathing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BlueDeep,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Calm your nervous system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BlueDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                // Breathing Visualizer
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow - now with blue accent
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .scale(state.scale * 1.1f)
                            .background(
                                SkyBlueLight.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )

                    // Main circle
                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(state.scale),
                        shape = CircleShape,
                        color = SkyBlueAccent.copy(alpha = state.opacity + 0.3f),
                        border = BorderStroke(1.dp, SkyBlueDark.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = state.instruction,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Supportive Text
                Text(
                    text = "Inhale (4s) • Hold (7s) • Exhale (8s)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = BlueDeep.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Done Button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "I'M DONE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}