package com.example.unifiedapp.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.utils.UserSessionHelper
import com.example.unifiedapp.utils.TrialHelper
import com.example.unifiedapp.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch

val DarkText = Color(0xFF1D2335)
val SecondaryText = Color(0xFF6B7280)

// ============ MAIN COMPOSABLE - FIXED SIGNATURE ============
@Composable
fun AssessmentHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Check login status directly from SharedPreferences
    val isLoggedIn = remember { UserSessionHelper.isUserLoggedIn(context) }
    val registrationId = UserSessionHelper.getRegistrationId(context)

    // State for trial checking
    var showNoTrialsDialog by remember { mutableStateOf(false) }
    var isCheckingTrials by remember { mutableStateOf(false) }

    // State for last assessment result dialog
    var showLastAssessmentDialog by remember { mutableStateOf(false) }

    // Get last assessment info
    val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
    val lastScore = remember { prefs.getInt("lastAssessmentScore", -1) }
    val lastDate = remember { prefs.getString("lastAssessmentDate", "") }
    val lastAiPrediction = remember { prefs.getString("ai_prediction_label", "") ?: "" }
    val hasLastAssessment = remember { lastScore != -1 && lastDate?.isNotEmpty() == true }

    // Clean AI prediction (remove parentheses and content)
    val cleanAiPrediction = remember(lastAiPrediction) {
        lastAiPrediction.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
    }

    // Determine severity based on score
    val severity = remember(lastScore) {
        when {
            lastScore <= 9 -> "Mild"
            lastScore <= 18 -> "Moderate"
            lastScore > 18 -> "Severe"
            else -> ""
        }
    }

    val severityColor = remember(lastScore) {
        when {
            lastScore <= 9 -> MildColor
            lastScore <= 18 -> ModerateColor
            lastScore > 18 -> SevereColor
            else -> Color.Gray
        }
    }

    val severityDescription = remember(lastScore) {
        when {
            lastScore <= 9 -> "You're showing mild symptoms. Self-care and monitoring recommended."
            lastScore <= 18 -> "You're showing moderate symptoms. Consider talking to a professional."
            lastScore > 18 -> "You're showing severe symptoms. Please seek professional help."
            else -> ""
        }
    }

    // Format date for display
    val formattedDate = remember(lastDate) {
        try {
            if (!lastDate.isNullOrEmpty()) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(lastDate)
                outputFormat.format(date)
            } else {
                ""
            }
        } catch (e: Exception) {
            lastDate ?: ""
        }
    }

    // Format full date and time for dialog
    val fullFormattedDate = remember(lastDate) {
        try {
            if (!lastDate.isNullOrEmpty()) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                val date = inputFormat.parse(lastDate)
                outputFormat.format(date)
            } else {
                ""
            }
        } catch (e: Exception) {
            lastDate ?: ""
        }
    }

    // Check if files exist for upload
    val filePrefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
    val hasVideo = remember { filePrefs.getString("video_path", null)?.let { File(it).exists() } == true }
    val hasAuCsv = remember { filePrefs.getString("au_csv_path", null)?.let { File(it).exists() } == true }
    val hasPhq9Csv = remember { filePrefs.getString("phq9_csv_path", null)?.let { File(it).exists() } == true }
    val hasFilesToUpload = hasVideo && hasAuCsv && hasPhq9Csv

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color.Transparent)
        ) {
            // -------- HEADER WITH CREAMY TRANSPARENCY --------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFDF9).copy(alpha = 0.4f))
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulse Icon with Gradient
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF8A80), Color(0xFFFFB74D))
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = "Pulse",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Mental Health Check",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1D2335),
                            lineHeight = 24.sp
                        )
                        Text(
                            text = "Track your mental wellness journey",
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            // -------- CONTENT BODY --------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- FORGOT TO UPLOAD CARD (if assessment was taken and files exist) ---
                if (hasLastAssessment && hasFilesToUpload) {
                    ForgotUploadCard(
                        date = formattedDate,
                        severity = severity,
                        severityColor = severityColor,
                        aiPrediction = cleanAiPrediction,
                        onViewResult = {
                            showLastAssessmentDialog = true
                        }
                    )
                }

                // --- DECORATED PHQ-9 ASSESSMENT CARD ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0x20000000)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFF0F5), Color(0xFFFFF7ED))
                                )
                            )
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        // 1. Background Decoration (Pink Circle)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-30).dp)
                                .size(160.dp)
                                .background(Color(0xFFFECDD3).copy(alpha = 0.4f), CircleShape)
                        )

                        // 2. Main Content
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Icon and Title Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Milky Icon Surface
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.6f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MonitorHeart,
                                            contentDescription = "PHQ-9 Icon",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "AI Mental Health Analysis",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "Powered by Advanced AI",
                                        fontSize = 14.sp,
                                        color = SecondaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Description
                            Text(
                                text = "60-second facial analysis using AI to assess your mental well-being. Get instant insights about your emotional state.",
                                fontSize = 14.sp,
                                color = SecondaryText,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Features Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FeatureItem(
                                    icon = Icons.Default.Timer,
                                    text = "60 Sec",
                                    color = Color(0xFFFF5252)
                                )
                                FeatureItem(
                                    icon = Icons.Default.Security,
                                    text = "Private",
                                    color = Color(0xFFFF5252)
                                )
                                FeatureItem(
                                    icon = Icons.Default.Analytics,
                                    text = "AI-Powered",
                                    color = Color(0xFFFF5252)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // UPDATED BUTTON - Check trials before starting assessment
                            Button(
                                onClick = {
                                    if (isLoggedIn) {
                                        coroutineScope.launch {
                                            isCheckingTrials = true
                                            val canProceed = TrialHelper.checkDepressionTrials(registrationId)
                                            isCheckingTrials = false
                                            if (canProceed) {
                                                navController.navigate("consent")
                                            } else {
                                                showNoTrialsDialog = true
                                            }
                                        }
                                    } else {
                                        navController.navigate("profile")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFF385C),
                                                    Color(0xFFFF5E3A),
                                                    Color(0xFFFF9345)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCheckingTrials) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Checking...",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isLoggedIn) "Start Assessment" else "Login to Continue",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (isLoggedIn) Icons.Default.AutoAwesome else Icons.Default.Login,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- ABOUT SECTION CARD (Milky White) ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 4.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "About AI Analysis",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Our AI analyzes facial expressions and action units (AUs) using advanced machine learning to provide insights into your mental health. The analysis takes 60 seconds and is completely private and secure.",
                            fontSize = 14.sp,
                            color = SecondaryText,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Bottom padding to ensure content clears the bottom bar
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Dialog shown when no trials remain
    if (showNoTrialsDialog) {
        AlertDialog(
            onDismissRequest = { showNoTrialsDialog = false },
            containerColor = Color(0xFFFFFDF9),
            title = { Text("No Trials Left", color = DarkText) },
            text = { Text("You have used all your depression assessment trials. Please contact the admin for more trials.", color = SecondaryText) },
            confirmButton = {
                TextButton(onClick = { showNoTrialsDialog = false }) {
                    Text("OK", color = Color(0xFFFF5252))
                }
            }
        )
    }

    // Dialog showing last assessment result - UPDATED to show both PHQ-9 and AI
    if (showLastAssessmentDialog && hasLastAssessment) {
        AlertDialog(
            onDismissRequest = { showLastAssessmentDialog = false },
            containerColor = Color(0xFFFFFDF9), // Creamy white background
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon circle with gradient
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF8A80), Color(0xFFFFB74D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MonitorHeart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Assessment Result",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Date chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = fullFormattedDate,
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    // Severity Card (PHQ-9)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = severityColor.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PHQ-9 Severity",
                                fontSize = 13.sp,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = severity,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = severityColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = severityDescription,
                                fontSize = 14.sp,
                                color = DarkText,
                                lineHeight = 20.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // AI Analysis Card
                    if (cleanAiPrediction.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PurplePrimary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "AI Analysis",
                                    fontSize = 13.sp,
                                    color = SecondaryText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = cleanAiPrediction,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Based on facial expression analysis",
                                    fontSize = 14.sp,
                                    color = DarkText,
                                    lineHeight = 20.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Recommendation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7) // Soft creamy yellow
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Recommendation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        lastScore <= 9 -> "Continue monitoring your mental health. Consider regular check-ins and maintaining healthy habits like exercise, good sleep, and social connections."
                                        lastScore <= 18 -> "Consider speaking with a mental health professional. Regular exercise, meditation, and talking to someone you trust may help improve your well-being."
                                        else -> "Please reach out to a mental health professional immediately. You don't have to go through this alone. Contact a crisis helpline or schedule an appointment with a therapist."
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Encouragement note
                    Text(
                        text = "✨ Remember, mental health is a journey. Every step you take matters. ✨",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLastAssessmentDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text(
                        text = "Got it",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = SecondaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============ FORGOT TO UPLOAD CARD ============
@Composable
fun ForgotUploadCard(
    date: String,
    severity: String,
    severityColor: Color,
    aiPrediction: String,      // AI prediction (cleaned)
    onViewResult: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x20000000)
            )
            .clickable { onViewResult() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF8A80), Color(0xFFFFB74D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Last Assessment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Text(
                    text = date,
                    fontSize = 13.sp,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Both results as badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PHQ-9 Severity badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = severityColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "PHQ-9: $severity",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // AI Analysis badge
                    if (aiPrediction.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PurplePrimary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "AI: $aiPrediction",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = SecondaryText
            )
        }
    }
}