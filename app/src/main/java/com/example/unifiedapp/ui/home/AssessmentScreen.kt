package com.example.unifiedapp.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Color Palette based on Image ---
val ScreenBackground = Color(0xFF7CA992) // The matte green background
val CardBackground = Color.White
val HillFront = Color(0xFF7AB684)
val HillBack = Color(0xFFA5D2A6)
val SunColor = Color(0xFFFFE082)
val SkyStart = Color(0xFFFFF8E1)
val SkyEnd = Color(0xFFE8F5E9)
val SelectedOptionBg = Color(0xFFFFFDE7) // Pale yellow row
val SelectedCheckBorder = Color(0xFFFFB300) // Golden check circle
val ButtonTeal = Color(0xFF169EA8) // The submit button color

@Composable
fun AssessmentScreen(onComplete: () -> Unit, onBack: () -> Unit) {
    val options = listOf(
        "Calm and relaxed",
        "A bit stressed",
        "Feeling anxious",
        "Pretty down",
        "Overwhelmed"
    )
    var selectedOption by remember { mutableStateOf(options[1]) }

    // 1. Main Screen Background (Solid Green)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 2. The Card Surface
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight() // Hug content but don't fill max height if not needed
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3. Illustration Header (Inside the card, at the top)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(SkyStart, SkyEnd)
                            )
                        )
                ) {
                    CardIllustration(modifier = Modifier.fillMaxSize())
                }

                // 4. Content Area
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How have you been feeling lately?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Options List
                    Column(
                        modifier = Modifier
                            .selectableGroup()
                            .fillMaxWidth()
                    ) {
                        options.forEach { text ->
                            OptionRow(
                                text = text,
                                isSelected = (text == selectedOption),
                                onClick = { selectedOption = text }
                            )
                            Divider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Buttons
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonTeal),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "Submit & See Results",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Optional Back Button (if needed purely for navigation logic)
                    // If visual fidelity to the image is 100% strict, you might hide this,
                    // but I included it since you passed the param.
                    /* TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Back", color = Color.Gray)
                    }
                    */
                }
            }
        }
    }
}

@Composable
fun OptionRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .background(if (isSelected) SelectedOptionBg else Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio Circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) SelectedCheckBorder else Color.LightGray,
                    shape = CircleShape
                )
                .background(
                    color = if (isSelected) SelectedCheckBorder else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isSelected) Color.Black else Color.DarkGray,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}


@Composable
fun CardIllustration(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Sun (Lifted Position)
        drawCircle(
            color = SunColor,
            radius = 35.dp.toPx(),
            // CHANGED: h * 0.8f -> h * 0.65f to move it up
            center = Offset(w / 2, h * 0.65f)
        )

        // 2. Back Hill (Lighter)
        val pathBack = Path().apply {
            moveTo(0f, h)
            quadraticBezierTo(w * 0.25f, h * 0.6f, w * 0.6f, h * 0.8f)
            quadraticBezierTo(w * 0.8f, h * 0.9f, w, h * 0.65f)
            lineTo(w, h)
            close()
        }
        drawPath(pathBack, HillBack)

        // 3. Front Hill (Darker)
        val pathFront = Path().apply {
            moveTo(0f, h)
            quadraticBezierTo(w * 0.2f, h * 0.85f, w * 0.5f, h * 0.9f)
            quadraticBezierTo(w * 0.8f, h * 0.95f, w, h * 0.7f)
            lineTo(w, h)
            close()
        }
        drawPath(pathFront, HillFront)

        // 4. Little Birds
        val birdColor = Color(0xFF5D786E)
        withTransform({
            translate(left = w * 0.75f, top = h * 0.3f)
            scale(scaleX = 0.8f, scaleY = 0.8f)
        }) {
            drawLine(birdColor, Offset(0f, 0f), Offset(5f, 3f), strokeWidth = 3f)
            drawLine(birdColor, Offset(5f, 3f), Offset(10f, 0f), strokeWidth = 3f)
        }
        withTransform({
            translate(left = w * 0.82f, top = h * 0.25f)
            scale(scaleX = 0.6f, scaleY = 0.6f)
        }) {
            drawLine(birdColor, Offset(0f, 0f), Offset(5f, 3f), strokeWidth = 3f)
            drawLine(birdColor, Offset(5f, 3f), Offset(10f, 0f), strokeWidth = 3f)
        }

        fun drawBird(x: Float, y: Float, scale: Float) {
            withTransform({
                translate(left = x, top = y)
                scale(scale, scale)
            }) {
                // Left wing
                drawLine(
                    color = birdColor,
                    start = Offset(-8f, -4f),
                    end = Offset(0f, 0f),
                    strokeWidth = 2.5f
                )
                // Right wing
                drawLine(
                    color = birdColor,
                    start = Offset(0f, 0f),
                    end = Offset(8f, -6f),
                    strokeWidth = 2.5f
                )
            }
        }

        drawBird(x = w * 0.72f, y = h * 0.25f, scale = 1.2f) // Largest, Left
        drawBird(x = w * 0.82f, y = h * 0.18f, scale = 1.0f) // Medium, Highest
        drawBird(x = w * 0.88f, y = h * 0.30f, scale = 0.8f) // Smallest, Right
    }
}