package com.example.unifiedapp.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unifiedapp.ui.remote.SimpleServerClient
import com.example.unifiedapp.ui.views.AssessmentData
import com.example.unifiedapp.ui.views.QuizResultViewModel
import com.example.unifiedapp.ui.views.UserData
import com.example.unifiedapp.ui.views.UserPreferences
import com.example.unifiedapp.vision.NotificationHelper
import com.example.unifiedapp.vision.ReportDownloadHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Color palette matching your app's sage green theme
val UnderageSageLight = Color(0xFFF1F7F3)
val UnderageSageMedium = Color(0xFFD3E4D6)
val UnderageSageAccent = Color(0xFF6B9071)
val UnderageCharcoal = Color(0xFF3E4E42)
val UnderageWhiteSoft = Color(0xFFFAFAFA)
val UnderageMutedSlate = Color(0xFF5D6D66)
val UnderageSuccessGreen = Color(0xFF4CAF50)

// ========== Helper functions for severity (copied from ResultScreen) ==========
enum class SeverityLevelUnderage { MILD, MODERATE, SEVERE }

fun getOverallSeverity(gad7Score: Int, aiPrediction: String): SeverityLevelUnderage {
    val gad7Severity = when (gad7Score) {
        in 0..7 -> SeverityLevelUnderage.MILD
        in 8..14 -> SeverityLevelUnderage.MODERATE
        else -> SeverityLevelUnderage.SEVERE
    }
    val aiSeverity = when {
        aiPrediction.contains("mild", ignoreCase = true) -> SeverityLevelUnderage.MILD
        aiPrediction.contains("moderate", ignoreCase = true) -> SeverityLevelUnderage.MODERATE
        aiPrediction.contains("severe", ignoreCase = true) -> SeverityLevelUnderage.SEVERE
        else -> SeverityLevelUnderage.MILD
    }
    return if (gad7Severity.ordinal >= aiSeverity.ordinal) gad7Severity else aiSeverity
}

fun getSeverityDisplay(severity: SeverityLevelUnderage): String = when (severity) {
    SeverityLevelUnderage.MILD -> "Mild"
    SeverityLevelUnderage.MODERATE -> "Moderate"
    SeverityLevelUnderage.SEVERE -> "Severe"
}

fun getSeverityEmoji(severity: SeverityLevelUnderage): String = when (severity) {
    SeverityLevelUnderage.MILD -> "🌱"
    SeverityLevelUnderage.MODERATE -> "🤝"
    SeverityLevelUnderage.SEVERE -> "🫂"
}

fun getMessageTitle(severity: SeverityLevelUnderage): String = when (severity) {
    SeverityLevelUnderage.MILD -> "You're Doing Well"
    SeverityLevelUnderage.MODERATE -> "Here for You"
    SeverityLevelUnderage.SEVERE -> "Take a Moment"
}

fun getSeverityColor(severity: SeverityLevelUnderage): Color = when (severity) {
    SeverityLevelUnderage.MILD -> Color(0xFF10B981)
    SeverityLevelUnderage.MODERATE -> Color(0xFFF59E0B)
    SeverityLevelUnderage.SEVERE -> Color(0xFFEF4444)
}
// ==============================================================================

@Composable
fun UnderageResultScreen(
    score: Int,
    gad7Score: Int,
    assessmentData: AssessmentData?,
    userPreferences: UserPreferences,
    navController: NavController,
    viewModel: QuizResultViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // State for popup visibility
    var showParentNotificationPopup by remember { mutableStateOf(true) }
    var isSendingEmail by remember { mutableStateOf(false) }
    var emailSent by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Get user data
    val userData by userPreferences.userData.collectAsStateWithLifecycle(
        initialValue = UserData(
            isLoggedIn = false,
            name = "",
            email = "",
            gender = "",
            age = 0,
            id = "",
            TOKEN = ""
        )
    )

    // Debug log for user data
    LaunchedEffect(Unit) {
        Log.d("ParentEmail", "========== UNDERAGE RESULT SCREEN ==========")
        Log.d("ParentEmail", "userData.parentEmail: '${userData.parentEmail}'")
        Log.d("ParentEmail", "userData.name: '${userData.name}'")
        Log.d("ParentEmail", "userData.isUnderage: ${userData.isUnderage}")
        Log.d("ParentEmail", "showParentNotificationPopup: $showParentNotificationPopup")
    }

    // Get severity levels (for the report only, not displayed to user)
    val overallSeverity = getOverallSeverity(gad7Score, uiState.anxietyPrediction)
    val severityDisplay = getSeverityDisplay(overallSeverity)
    val severityEmoji = getSeverityEmoji(overallSeverity)
    val messageTitle = getMessageTitle(overallSeverity)

    // Permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notification permission denied.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Create notification channel when screen loads
    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Function to generate the full report PDF (same as in ResultsScreen)
    fun generateFullReport(): String? {
        return try {
            val resultState = ReportDownloadHelper.ResultStateData(
                title = messageTitle,
                message = "Based on your responses, you're experiencing $severityDisplay",
                emoji = severityEmoji,
                color = getSeverityColor(overallSeverity).hashCode()
            )

            val aiData = ReportDownloadHelper.AiPredictionData(
                anxietyPrediction = uiState.anxietyPrediction,
                anxietyScore = uiState.anxietyScore.toFloat(),
                anxietyConfidence = uiState.anxietyConfidence.toFloat()
            )

            val filePath = ReportDownloadHelper.generateReport(
                context = context,
                score = gad7Score,
                resultState = resultState,
                aiData = aiData,
                anonymousId = userData.id.ifEmpty { "user" }
            )

            Log.d("UnderageResult", "PDF generated at: $filePath")
            filePath

        } catch (e: Exception) {
            Log.e("UnderageResult", "Failed to generate report", e)
            null
        }
    }

    fun sendReportToParent() {
        Toast.makeText(context, "Sending report to parent...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val parentEmail = userData.parentEmail
                Log.d("ParentEmail", "sendReportToParent - parentEmail: '$parentEmail'")
                Log.d("ParentEmail", "sendReportToParent - userName: '${userData.name}'")
                Log.d("ParentEmail", "sendReportToParent - severityDisplay: '$severityDisplay'")
                Log.d("ParentEmail", "sendReportToParent - aiPrediction: '${uiState.anxietyPrediction}'")

                if (parentEmail.isEmpty()) {
                    Log.e("ParentEmail", "❌ No parent email found in userData!")
                    withContext(Dispatchers.Main) {
                        emailError = "No parent email found"
                        isSendingEmail = false
                    }
                    return@launch
                }

                val jsonPayload = JSONObject().apply {
                    put("to_email", parentEmail)
                    put("user_name", userData.name)
                    put("assessment_type", "anxiety")
                    put("severity", severityDisplay)
                    put("ai_prediction", uiState.anxietyPrediction)
                }

                Log.d("ParentEmail", "JSON Payload: $jsonPayload")

                val url = URL("http://203.110.243.202:8000/api/send-report-via-google")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                connection.outputStream.use { os ->
                    os.write(jsonPayload.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                val success = responseCode in 200..299

                Log.d("ParentEmail", "Response Code: $responseCode")
                Log.d("ParentEmail", "Success: $success")

                withContext(Dispatchers.Main) {
                    if (success) {
                        emailSent = true
                        Toast.makeText(context, "Report sent to ${userData.parentEmail}", Toast.LENGTH_LONG).show()
                    } else {
                        emailError = "Failed to send email. Please contact support."
                    }
                    isSendingEmail = false
                }

            } catch (e: Exception) {
                Log.e("ParentEmail", "❌ Error sending email: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    emailError = e.message ?: "Unknown error occurred"
                    isSendingEmail = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = UnderageWhiteSoft
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(UnderageSageLight, UnderageWhiteSoft)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Decorative illustration
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(UnderageSageAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = UnderageSageAccent,
                        modifier = Modifier
                            .size(56.dp)
                            .scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Assessment Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnderageCharcoal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your results have been shared with your parent/guardian.",
                    fontSize = 16.sp,
                    color = UnderageMutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Wellness Tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnderageCharcoal,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WellnessToolCard(
                        icon = Icons.Default.SelfImprovement,
                        title = "4-7-8 Breathing",
                        description = "Calming breathing technique",
                        iconColor = Color(0xFF7EC8E3),
                        onClick = {
                            navController.navigate("breathing") {
                                popUpTo("underage_results") { inclusive = true }
                            }
                        }
                    )
                    WellnessToolCard(
                        icon = Icons.Default.MusicNote,
                        title = "Calming Sounds",
                        description = "Nature sounds & ambient music",
                        iconColor = Color(0xFFA8E6CF),
                        onClick = {
                            navController.navigate("sounds") {
                                popUpTo("underage_results") { inclusive = true }
                            }
                        }
                    )
                    WellnessToolCard(
                        icon = Icons.Default.Spa,
                        title = "Grounding Exercise",
                        description = "5-4-3-2-1 technique",
                        iconColor = Color(0xFFFFD7B5),
                        onClick = {
                            navController.navigate("grounding") {
                                popUpTo("underage_results") { inclusive = true }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Manual test button (styled, not red)
                Button(
                    onClick = {
                        Log.d("ParentEmail", "MANUAL TEST BUTTON CLICKED")
                        sendReportToParent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = UnderageSageAccent)
                ) {
                    Text("Send Report to Parent", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Return Home Button
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnderageSageAccent
                    )
                ) {
                    Text(
                        "Return Home",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Parent Notification Popup
    if (showParentNotificationPopup) {
        ParentNotificationPopup(
            parentEmail = userData.parentEmail,
            childName = userData.name,
            isSending = isSendingEmail,
            emailSent = emailSent,
            emailError = emailError,
            onSendReport = {
                Log.d("ParentEmail", "========== ON SEND REPORT CLICKED ==========")
                if (!isSendingEmail) {
                    isSendingEmail = true
                    emailError = null
                    sendReportToParent()
                }
            },
            onDismiss = {
                if (emailSent || (!isSendingEmail && emailError != null)) {
                    showParentNotificationPopup = false
                }
            }
        )
    }
}

@Composable
fun WellnessToolCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnderageCharcoal
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = UnderageMutedSlate
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = UnderageMutedSlate,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ParentNotificationPopup(
    parentEmail: String,
    childName: String,
    isSending: Boolean,
    emailSent: Boolean,
    emailError: String?,
    onSendReport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSending && (emailSent || emailError != null)) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(UnderageSageAccent, UnderageSageMedium)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Parent Notification",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnderageCharcoal
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (emailSent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = UnderageSuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Report sent successfully to your parent/guardian!",
                            fontSize = 14.sp,
                            color = UnderageCharcoal,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = UnderageSageLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "📧 Email sent to:",
                                fontSize = 12.sp,
                                color = UnderageMutedSlate
                            )
                            Text(
                                parentEmail.ifEmpty { "Parent email not provided" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = UnderageSageAccent
                            )
                        }
                    }
                } else if (emailError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Failed to send report",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        emailError,
                        fontSize = 13.sp,
                        color = UnderageMutedSlate
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Please contact support or try again later.",
                        fontSize = 13.sp,
                        color = UnderageMutedSlate
                    )
                } else {
                    Text(
                        "Since you're under 18, your assessment results will be shared with your parent/guardian.",
                        fontSize = 14.sp,
                        color = UnderageCharcoal,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = UnderageSageLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "👨‍👩‍👧 Parent/Guardian:",
                                fontSize = 12.sp,
                                color = UnderageMutedSlate
                            )
                            Text(
                                parentEmail.ifEmpty { "No parent email on file" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (parentEmail.isNotEmpty()) UnderageSageAccent else UnderageMutedSlate
                            )
                        }
                    }

                    if (parentEmail.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ No parent email was provided during registration. Please contact support to update your parent's email.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (emailSent) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnderageSageAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (emailError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = onSendReport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UnderageSageAccent
                        )
                    ) {
                        Text("Try Again")
                    }
                }
            } else {
                Button(
                    onClick = onSendReport,
                    enabled = !isSending && parentEmail.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnderageSageAccent,
                        disabledContainerColor = UnderageSageMedium
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSending) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...", color = Color.White)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Report to Parent", color = Color.White)
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (!emailSent && emailError == null && !isSending) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = UnderageMutedSlate)
                }
            }
        }
    )
}