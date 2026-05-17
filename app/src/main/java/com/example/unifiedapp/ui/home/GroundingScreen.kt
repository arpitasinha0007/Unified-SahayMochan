package com.example.unifiedapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Data class for grounding senses - matching the first file's structure
data class GroundingSense(
    val id: Int,
    val label: String,
    val count: Int,
    val subtext: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val promptText: String,
    val placeholder: String
)

// Vibrant, distinct colors for each sense (keeping from second file exactly)
val SenseColors = listOf(
    // See - Sky Blue
    listOf(Color(0xFF87CEEB), Color(0xFF4169E1)),
    // Touch - Warm Orange
    listOf(Color(0xFFFFB347), Color(0xFFFF8C42)),
    // Hear - Vibrant Purple
    listOf(Color(0xFFD68BF8), Color(0xFF9B51E0)),
    // Smell - Fresh Green
    listOf(Color(0xFF98D8A8), Color(0xFF2E7D32)),
    // Taste - Coral Pink
    listOf(Color(0xFFFF9A9A), Color(0xFFE57373))
)

val SenseIcons = listOf(
    Icons.Default.Visibility,
    Icons.Default.TouchApp,
    Icons.Default.VolumeUp,
    Icons.Outlined.Spa,
    Icons.Default.Restaurant
)

val SenseLabels = listOf("See", "Touch", "Hear", "Smell", "Taste")
val SenseDescriptions = listOf(
    "5 things you can see",
    "4 things you can feel",
    "3 things you can hear",
    "2 things you can smell",
    "1 thing you can taste"
)
val SensePrompts = listOf(
    "What do you see around you?",
    "What can you physically feel?",
    "What sounds do you hear?",
    "What scents are in the air?",
    "What can you taste right now?"
)

// Grounding senses data in the style of first file
val groundingSenses = listOf(
    GroundingSense(
        id = 0,
        label = SenseLabels[0],
        count = 5,
        subtext = SenseDescriptions[0],
        icon = SenseIcons[0],
        gradient = SenseColors[0],
        promptText = SensePrompts[0],
        placeholder = "e.g., blue sky, tree, book..."
    ),
    GroundingSense(
        id = 1,
        label = SenseLabels[1],
        count = 4,
        subtext = SenseDescriptions[1],
        icon = SenseIcons[1],
        gradient = SenseColors[1],
        promptText = SensePrompts[1],
        placeholder = "e.g., soft fabric, warm cup..."
    ),
    GroundingSense(
        id = 2,
        label = SenseLabels[2],
        count = 3,
        subtext = SenseDescriptions[2],
        icon = SenseIcons[2],
        gradient = SenseColors[2],
        promptText = SensePrompts[2],
        placeholder = "e.g., birds, fan, music..."
    ),
    GroundingSense(
        id = 3,
        label = SenseLabels[3],
        count = 2,
        subtext = SenseDescriptions[3],
        icon = SenseIcons[3],
        gradient = SenseColors[3],
        promptText = SensePrompts[3],
        placeholder = "e.g., coffee, rain, flowers..."
    ),
    GroundingSense(
        id = 4,
        label = SenseLabels[4],
        count = 1,
        subtext = SenseDescriptions[4],
        icon = SenseIcons[4],
        gradient = SenseColors[4],
        promptText = SensePrompts[4],
        placeholder = "e.g., mint, tea, nothing..."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundingScreen(onBack: () -> Unit) {
    // State for each sense's items - matching first file's structure
    val senseInputs = remember {
        mutableStateMapOf<Int, List<String>>().apply {
            groundingSenses.forEach { sense ->
                this[sense.id] = List(sense.count) { "" }
            }
        }
    }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var completedSenses by remember { mutableStateOf(0) }

    // Calculate completion
    val isFullyComplete = groundingSenses.all { sense ->
        senseInputs[sense.id]?.all { it.isNotBlank() } == true
    }

    // Update completed count
    LaunchedEffect(senseInputs) {
        completedSenses = groundingSenses.count { sense ->
            senseInputs[sense.id]?.all { it.isNotBlank() } == true
        }
    }

    // Success message timer
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
        }
    }

    fun updateSenseItem(senseId: Int, index: Int, value: String) {
        val currentList = senseInputs[senseId] ?: return
        val newList = currentList.toMutableList()
        newList[index] = value
        senseInputs[senseId] = newList
    }

    fun resetAllInputs() {
        groundingSenses.forEach { sense ->
            senseInputs[sense.id] = List(sense.count) { "" }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F0E7),
                        Color(0xFFE8D9CD)
                    )
                )
            )
    ) {
        // Decorative elements (keeping from second file)
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFE5B4).copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFB6C1).copy(alpha = 0.2f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // Header (keeping exactly from second file)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.8f),
                tonalElevation = 0.dp,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button with colored background
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF9A8B), Color(0xFFFF6B6B))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "5-4-3-2-1 Grounding",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Anchor yourself in the present",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Progress indicator
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isFullyComplete)
                                    Brush.linearGradient(listOf(Color(0xFF6B8E4C), Color(0xFF4CAF50)))
                                else
                                    Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$completedSenses/5",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 20.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Card - first file style
                item {
                    ProgressCard(
                        completed = completedSenses,
                        total = groundingSenses.size,
                        isComplete = isFullyComplete
                    )
                }

                // Info Card - first file style with second file colors
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F6)),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = SenseColors[4][1],
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, and 1 you taste.",
                                fontSize = 13.sp,
                                color = Color(0xFF2C3E50),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Grounding Sense Cards - first file layout with second file colors
                items(groundingSenses) { sense ->
                    GroundingSenseCard(
                        sense = sense,
                        items = senseInputs[sense.id] ?: List(sense.count) { "" },
                        onItemChange = { index, value ->
                            updateSenseItem(sense.id, index, value)
                        }
                    )
                }

                // Complete Button - first file style with second file colors
                item {
                    Button(
                        onClick = {
                            showSuccessMessage = true
                            resetAllInputs()
                        },
                        enabled = isFullyComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFullyComplete) Color(0xFF6B8E4C) else Color.LightGray,
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isFullyComplete) "Complete Exercise" else "Complete all senses first",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Success Overlay - second file style with animations
        if (showSuccessMessage) {
            SuccessOverlay(
                message = "Wonderful! 🌟",
                subMessage = "You're grounded and present"
            )
        }
    }
}

@Composable
fun ProgressCard(completed: Int, total: Int, isComplete: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )

                if (isComplete) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = SenseColors[4][1].copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SenseColors[4][1],
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Ready to complete",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SenseColors[4][1]
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (total > 0) completed.toFloat() / total else 0f)
                        .height(8.dp)
                        .background(SenseColors[4][1], RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completed of $total senses completed",
                fontSize = 14.sp,
                color = Color(0xFF7F8C8D)
            )
        }
    }
}

@Composable
fun GroundingSenseCard(
    sense: GroundingSense,
    items: List<String>,
    onItemChange: (Int, String) -> Unit
) {
    val isSenseComplete = items.all { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header - first file style
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(colors = sense.gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        sense.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${sense.label} (${sense.count})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        sense.subtext,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (isSenseComplete) {
                    Surface(
                        shape = CircleShape,
                        color = sense.gradient[1].copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = sense.gradient[1],
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${items.count { it.isNotBlank() }}/${items.size}",
                        color = sense.gradient[1],
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input fields - first file style
            repeat(sense.count) { index ->
                GroundingInputField(
                    value = items.getOrElse(index) { "" },
                    onValueChange = { onItemChange(index, it) },
                    placeholder = if (index == 0) sense.promptText else "",
                    example = sense.placeholder,
                    index = index + 1,
                    total = sense.count,
                    gradient = sense.gradient
                )
            }
        }
    }
}

@Composable
fun GroundingInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    example: String,
    index: Int,
    total: Int,
    gradient: List<Color>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(gradient.first().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradient.first()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = if (index == 1) placeholder else example,
                        fontSize = 13.sp,
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gradient.first(),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFFFFF0F6).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color(0xFF2C3E50),
                    unfocusedTextColor = Color(0xFF34495E)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )
        }
    }
}

@Composable
fun SuccessOverlay(
    message: String,
    subMessage: String
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .padding(32.dp)
        ) {
            // Animated circles
            Box(contentAlignment = Alignment.Center) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$index")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_animation_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp + (index * 40).dp)
                            .scale(pulse)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SenseColors[index % 5][0].copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // Center icon
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFB347),
                                        Color(0xFFFF8C42)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Success text
            Text(
                text = message,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subMessage,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}