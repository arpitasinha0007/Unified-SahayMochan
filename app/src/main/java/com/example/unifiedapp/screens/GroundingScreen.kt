package com.example.unifiedapp.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ============ LOCAL COLOR DEFINITIONS (Unique names to avoid conflicts) ============
private val GroundingPurplePrimary = Color(0xFF8B5CF6)
private val GroundingPurpleLight = Color(0xFFC4B5FD)
private val GroundingPurpleUltraLight = Color(0xFFF5F3FF)
private val GroundingSurfaceOffWhite = Color(0xFFF8FAFC)
private val GroundingTextPrimary = Color(0xFF1F2937)
private val GroundingTextSecondary = Color(0xFF4B5563)
private val GroundingTextTertiary = Color(0xFF6B7280)
private val GroundingTextMuted = Color(0xFF9CA3AF)
private val GroundingBorderLight = Color(0xFFE5E7EB)
private val GroundingGlassWhiteHeavy = Color.White.copy(alpha = 0.75f)
private val GroundingBlueBright = Color(0xFF60A5FA)
private val GroundingOrangeWarm = Color(0xFFF97316)
private val GroundingGreenMint = Color(0xFF10B981)
private val GroundingOrangeRed = Color(0xFFEF4444)

// ============ LOCAL GRADIENT DEFINITIONS (Unique names) ============
private val GroundingGradientBlueCyan = Brush.linearGradient(
    colors = listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))
)
private val GroundingGradientPurplePink = Brush.linearGradient(
    colors = listOf(GroundingPurplePrimary, Color(0xFFEC4899))
)
private val GroundingGradientGreenTeal = Brush.linearGradient(
    colors = listOf(GroundingGreenMint, Color(0xFF14B8A6))
)
private val GroundingGradientOrangeRed = Brush.linearGradient(
    colors = listOf(GroundingOrangeWarm, GroundingOrangeRed)
)
private val GroundingGradientPurple = Brush.linearGradient(
    colors = listOf(GroundingPurplePrimary, Color(0xFFA78BFA))
)

private val SenseGradients = listOf(
    GroundingGradientBlueCyan,      // See
    GroundingGradientOrangeRed,     // Touch
    GroundingGradientPurplePink,    // Hear
    GroundingGradientGreenTeal,     // Smell
    GroundingGradientOrangeRed      // Taste
)

private val SenseIcons = listOf(
    Icons.Default.Visibility,
    Icons.Default.TouchApp,
    Icons.Default.VolumeUp,
    Icons.Outlined.Spa,
    Icons.Default.Restaurant
)

private val SenseLabels = listOf("See", "Touch", "Hear", "Smell", "Taste")
private val SenseDescriptions = listOf(
    "5 things you can see",
    "4 things you can feel",
    "3 things you can hear",
    "2 things you can smell",
    "1 thing you can taste"
)
private val SensePrompts = listOf(
    "What do you see around you?",
    "What can you physically feel?",
    "What sounds do you hear?",
    "What scents are in the air?",
    "What can you taste right now?"
)

private val SenseAccentColors = listOf(
    GroundingBlueBright,        // See
    GroundingOrangeWarm,        // Touch
    GroundingPurplePrimary,     // Hear
    GroundingGreenMint,         // Smell
    GroundingOrangeRed          // Taste
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundingScreen(onBack: () -> Unit) {
    val senseInputs = remember {
        mutableStateMapOf<Int, List<String>>().apply {
            (0..4).forEach { index ->
                this[index] = List(5 - index) { "" }
            }
        }
    }

    var showSuccessMessage by remember { mutableStateOf(false) }
    var completedSenses by remember { mutableStateOf(0) }
    var expandedSense by remember { mutableStateOf<Int?>(null) }

    val isFullyComplete = (0..4).all { senseIndex ->
        senseInputs[senseIndex]?.all { it.isNotBlank() } == true
    }

    LaunchedEffect(senseInputs) {
        completedSenses = (0..4).count { senseIndex ->
            senseInputs[senseIndex]?.all { it.isNotBlank() } == true
        }
    }

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
        }
    }

    fun updateSenseItem(senseIndex: Int, itemIndex: Int, value: String) {
        val currentList = senseInputs[senseIndex] ?: return
        val newList = currentList.toMutableList()
        newList[itemIndex] = value
        senseInputs[senseIndex] = newList
    }

    fun resetAllInputs() {
        (0..4).forEach { index ->
            senseInputs[index] = List(5 - index) { "" }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GroundingPurpleUltraLight,
                        GroundingSurfaceOffWhite
                    )
                )
            )
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GroundingPurpleLight.copy(alpha = 0.3f), Color.Transparent),
                        radius = 300f
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
                        colors = listOf(GroundingOrangeWarm.copy(alpha = 0.2f), Color.Transparent),
                        radius = 250f
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
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GroundingGlassWhiteHeavy,
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
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GroundingGradientPurplePink)
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
                            color = GroundingTextPrimary
                        )
                        Text(
                            text = "Anchor yourself in the present",
                            fontSize = 14.sp,
                            color = GroundingTextSecondary
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
                                    GroundingGradientGreenTeal
                                else
                                    Brush.linearGradient(listOf(GroundingTextMuted, GroundingBorderLight))
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
                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "info_pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "info_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GroundingGradientPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "Notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, and 1 you taste.",
                                fontSize = 14.sp,
                                color = GroundingTextSecondary,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sense Cards
                items(5) { senseIndex ->
                    val items = senseInputs[senseIndex] ?: List(5 - senseIndex) { "" }
                    val isExpanded = expandedSense == senseIndex || items.any { it.isNotBlank() }
                    val isComplete = items.all { it.isNotBlank() }
                    val gradient = SenseGradients[senseIndex]
                    val accentColor = SenseAccentColors[senseIndex]

                    GroundingSenseCard(
                        senseIndex = senseIndex,
                        label = SenseLabels[senseIndex],
                        description = SenseDescriptions[senseIndex],
                        items = items,
                        gradient = gradient,
                        accentColor = accentColor,
                        icon = SenseIcons[senseIndex],
                        isExpanded = isExpanded,
                        isComplete = isComplete,
                        onExpandToggle = {
                            expandedSense = if (expandedSense == senseIndex) null else senseIndex
                        },
                        onItemChange = { itemIndex, value ->
                            updateSenseItem(senseIndex, itemIndex, value)
                        }
                    )
                }

                // Complete Button
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
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFullyComplete) GroundingGreenMint else GroundingTextMuted.copy(alpha = 0.3f),
                            disabledContainerColor = GroundingTextMuted.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFullyComplete) "Complete Exercise" else "Complete all senses first",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Success Overlay
        if (showSuccessMessage) {
            SuccessOverlayGrounding(
                message = "Wonderful! 🌟",
                subMessage = "You're grounded and present"
            )
        }
    }
}

@Composable
fun GroundingSenseCard(
    senseIndex: Int,
    label: String,
    description: String,
    items: List<String>,
    gradient: Brush,
    accentColor: Color,
    icon: ImageVector,
    isExpanded: Boolean,
    isComplete: Boolean,
    onExpandToggle: () -> Unit,
    onItemChange: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isExpanded) 8.dp else 2.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            accentColor.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            // Header - Always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpandToggle() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GroundingTextPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = GroundingTextSecondary
                    )
                }

                if (isComplete) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${items.count { it.isNotBlank() }}/${items.size}",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Expandable content
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                ) {
                    items.forEachIndexed { index, value ->
                        GroundingInputField(
                            value = value,
                            onValueChange = { onItemChange(index, it) },
                            prompt = if (index == 0) SensePrompts[senseIndex] else "",
                            index = index + 1,
                            total = items.size,
                            accentColor = accentColor,
                            gradient = gradient,
                            isLast = index == items.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroundingInputField(
    value: String,
    onValueChange: (String) -> Unit,
    prompt: String,
    index: Int,
    total: Int,
    accentColor: Color,
    gradient: Brush,
    isLast: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (!isLast) 12.dp else 0.dp)
    ) {
        // Number indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (index == 1 && prompt.isNotEmpty()) {
                Text(
                    text = prompt,
                    fontSize = 13.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Input field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            placeholder = {
                Text(
                    text = getExampleForIndex(index, total),
                    fontSize = 13.sp,
                    color = GroundingTextTertiary
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = GroundingBorderLight,
                focusedContainerColor = accentColor.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = GroundingTextPrimary,
                unfocusedTextColor = GroundingTextSecondary,
                cursorColor = accentColor
            ),
            singleLine = true,
            interactionSource = interactionSource,
            isError = value.isBlank() && isFocused
        )

        if (value.isBlank() && index == 1) {
            Text(
                text = "Tap to write your observation...",
                fontSize = 11.sp,
                color = GroundingTextTertiary,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp)
            )
        }
    }
}

private fun getExampleForIndex(index: Int, total: Int): String {
    return when (total) {
        5 -> listOf("blue sky", "green plant", "wooden table", "white wall", "phone screen")[index - 1]
        4 -> listOf("soft fabric", "warm mug", "cool breeze", "ground beneath")[index - 1]
        3 -> listOf("birds singing", "fan humming", "distant traffic")[index - 1]
        2 -> listOf("coffee aroma", "fresh air")[index - 1]
        else -> "mint taste"
    }
}

@Composable
fun SuccessOverlayGrounding(
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
                                Brush.radialGradient(
                                    colors = listOf(
                                        when (index % 3) {
                                            0 -> GroundingPurplePrimary.copy(alpha = 0.3f)
                                            1 -> GroundingOrangeWarm.copy(alpha = 0.3f)
                                            else -> GroundingGreenMint.copy(alpha = 0.3f)
                                        },
                                        Color.Transparent
                                    ),
                                    radius = 200f
                                ),
                                CircleShape
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
                            .background(GroundingGradientPurplePink, CircleShape),
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