package com.example.unifiedapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import androidx.core.content.edit
import android.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect

// --- MATCHING SAGE COLOR PALETTE from your other screens ---
val WaterSageLight = Color(0xFFF1F7F3)
val WaterSageMedium = Color(0xFFD3E4D6)
val WaterSageAccent = Color(0xFF6B9071)
val WaterCharcoal = Color(0xFF3E4E42)
val WaterWhiteSoft = Color(0xFFFAFAFA)
val WaterMutedSlate = Color(0xFF5D6D66)
val WaterBlue = Color(0xFF4A90E2) // Slight blue tint for water theme

// Gradient matching your ProfileScreen
val WaterSageGradient: Brush = Brush.verticalGradient(
    colors = listOf(WaterSageLight, WaterSageMedium)
)

object WaterIntakeStorage {
    private const val PREFS_NAME = "water_intake_prefs"
    private const val KEY_AMOUNT = "water_amount"
    private const val KEY_DATE = "last_date"

    @RequiresApi(Build.VERSION_CODES.O)
    fun getWaterIntake(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_DATE, "")
        val todayDate = LocalDate.now().toString()

        return if (lastDate != todayDate) {
            saveWaterIntake(context, 0)
            0
        } else {
            prefs.getInt(KEY_AMOUNT, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveWaterIntake(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_AMOUNT, amount)
            putString(KEY_DATE, LocalDate.now().toString())
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WaterIntakeScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val maxWater = 2640 // Recommended daily intake in ml

    var currentWaterLevel by remember { mutableIntStateOf(0) }
    var selectedAmount by remember { mutableIntStateOf(250) } // Default to 250ml (a glass)

    // Load saved data
    LaunchedEffect(Unit) {
        currentWaterLevel = WaterIntakeStorage.getWaterIntake(context)
    }

    // Animations
    val targetPercentage = (currentWaterLevel.toFloat() / maxWater).coerceIn(0f, 1f)
    val animatedPercentage by animateFloatAsState(
        targetValue = targetPercentage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "WaterLevel"
    )

    val animatedIntDisplay by animateIntAsState(
        targetValue = currentWaterLevel,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TextDisplay"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WaterSageGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button (matching ProfileScreen style)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.6f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Back",
                        tint = WaterCharcoal
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Hydration",
                    style = MaterialTheme.typography.titleMedium,
                    color = WaterCharcoal.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Water amount display card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WaterWhiteSoft),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Today's Intake",
                        color = WaterMutedSlate,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$animatedIntDisplay ml",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WaterSageAccent
                    )

                    // Progress bar
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(WaterMutedSlate.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedPercentage)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(WaterSageAccent, WaterBlue)
                                    )
                                )
                        )
                    }

                    Text(
                        text = "of $maxWater ml",
                        color = WaterMutedSlate,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Water tank visual
            WaterTankVisual(
                percentage = animatedPercentage,
                modifier = Modifier.size(width = 200.dp, height = 200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Amount selector
            Text(
                text = "Add amount",
                color = WaterMutedSlate,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            WaterAmountSelector(
                selectedAmount = selectedAmount,
                onAmountSelected = { selectedAmount = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Add water button
            WaterSageButton(
                text = "Add Water",
                onClick = {
                    if (currentWaterLevel < maxWater) {
                        val newAmount = (currentWaterLevel + selectedAmount).coerceAtMost(maxWater)
                        currentWaterLevel = newAmount
                        WaterIntakeStorage.saveWaterIntake(context, newAmount)
                    }
                }
            )

            // Reset button (optional)
            TextButton(
                onClick = {
                    currentWaterLevel = 0
                    WaterIntakeStorage.saveWaterIntake(context, 0)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Reset",
                    color = WaterMutedSlate,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun WaterTankVisual(percentage: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val radius = width / 2.5f

            // Background circle (glass effect)
            drawCircle(
                color = WaterWhiteSoft.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Water fill
            if (percentage > 0) {
                val waterHeight = height * percentage
                val waterTop = height - waterHeight

                val waterPath = Path().apply {
                    addOval(
                        Rect(
                            left = center.x - radius,
                            top = waterTop,
                            right = center.x + radius,
                            bottom = height
                        )
                    )
                }

                drawPath(
                    path = waterPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            WaterBlue.copy(alpha = 0.7f),
                            WaterSageAccent
                        ),
                        startY = waterTop,
                        endY = height
                    )
                )
            }

            // Percentage text in center
            drawContext.canvas.nativeCanvas.apply {
                val text = "${(percentage * 100).toInt()}%"
                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#3E4E42")
                    textSize = 32.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawText(text, center.x, center.y + 12.sp.toPx() / 3, paint)
            }

            // Water droplets decoration
            if (percentage < 0.95f) {
                drawCircle(
                    color = WaterBlue.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = Offset(center.x - radius * 0.7f, center.y - radius * 0.5f)
                )
                drawCircle(
                    color = WaterBlue.copy(alpha = 0.2f),
                    radius = 6.dp.toPx(),
                    center = Offset(center.x + radius * 0.6f, center.y - radius * 0.3f)
                )
            }
        }
    }
}

@Composable
fun WaterAmountSelector(
    selectedAmount: Int,
    onAmountSelected: (Int) -> Unit
) {
    val amounts = listOf(50, 100, 150, 200, 250, 300, 350, 400, 500)

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(amounts) { amount ->
            val isSelected = amount == selectedAmount

            Surface(
                onClick = { onAmountSelected(amount) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) WaterSageAccent else WaterWhiteSoft,
                tonalElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier.width(70.dp)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${amount}ml",
                        color = if (isSelected) Color.White else WaterCharcoal,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WaterSageButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WaterSageAccent,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}