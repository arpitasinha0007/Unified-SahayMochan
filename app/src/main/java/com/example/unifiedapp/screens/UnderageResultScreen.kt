package com.example.unifiedapp.screens

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
import androidx.navigation.NavController
import com.example.unifiedapp.utils.NotificationHelper
import com.example.unifiedapp.utils.UserSessionHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ============ SEVERITY LEVELS FOR DEPRESSION (used only for email) ============
enum class SeverityLevel { MILD, MODERATE, SEVERE }

fun getSeverityFromPHQ9(score: Int): SeverityLevel = when (score) {
    in 0..9 -> SeverityLevel.MILD
    in 10..18 -> SeverityLevel.MODERATE
    else -> SeverityLevel.SEVERE
}

fun getSeverityFromAI(aiPrediction: String): SeverityLevel {
    return when {
        aiPrediction.contains("mild", ignoreCase = true) -> SeverityLevel.MILD
        aiPrediction.contains("moderate", ignoreCase = true) -> SeverityLevel.MODERATE
        aiPrediction.contains("severe", ignoreCase = true) -> SeverityLevel.SEVERE
        else -> SeverityLevel.MILD
    }
}

fun getOverallSeverity(phq9Score: Int, aiPrediction: String): SeverityLevel {
    val phq9Severity = getSeverityFromPHQ9(phq9Score)
    val aiSeverity = getSeverityFromAI(aiPrediction)
    return maxOf(phq9Severity, aiSeverity, compareBy { it.ordinal })
}

fun getSeverityDisplay(severity: SeverityLevel): String = when (severity) {
    SeverityLevel.MILD -> "Mild"
    SeverityLevel.MODERATE -> "Moderate"
    SeverityLevel.SEVERE -> "Severe"
}

// ============ MAIN UNDERAGE RESULT SCREEN (NO RESULTS DISPLAYED) ============
@Composable
fun UnderageResultScreen(
    navController: NavController,
    score: Int,  // PHQ-9 score (used only for email)
    aiPrediction: String = "",
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get user data from UserSessionHelper
    val session = UserSessionHelper.getUserData(context)
    val userName = session.name
    val registrationId = session.registrationId

    // Get parent email from user_prefs (stored during registration)
    val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val parentEmail = userPrefs.getString("parent_email", "") ?: ""

    var isSendingEmail by remember { mutableStateOf(false) }
    var emailSent by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Calculate severity for email (not displayed to user)
    val overallSeverity = getOverallSeverity(score, aiPrediction)
    val severityDisplay = getSeverityDisplay(overallSeverity)

    Log.d("UnderageResult", "User: $userName, Parent Email: $parentEmail")
    Log.d("UnderageResult", "PHQ-9 Score: $score, Severity: $severityDisplay")

    // Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_LONG).show()
        }
    }

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

    // ============================================================
    // SEND REPORT TO PARENT - MOCHAN DEPRESSION API
    // ============================================================
    fun sendReportToParent() {
        Toast.makeText(context, "Sending report to parent...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                Log.d("UnderageResult", "sendReportToParent - parentEmail: '$parentEmail'")
                Log.d("UnderageResult", "sendReportToParent - userName: '$userName'")
                Log.d("UnderageResult", "sendReportToParent - severityDisplay: '$severityDisplay'")

                if (parentEmail.isEmpty()) {
                    Log.e("UnderageResult", "No parent email found!")
                    withContext(Dispatchers.Main) {
                        emailError = "No parent email found. Please contact support."
                        isSendingEmail = false
                    }
                    return@launch
                }

                // Generate AI message without numeric score
                var aiMessage = aiPrediction
                // Remove any parentheses and their content (e.g., "Low (0-9)" -> "Low")
                aiMessage = aiMessage.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
                if (aiMessage.isEmpty() || aiMessage == "No Data") {
                    aiMessage = when (severityDisplay.lowercase()) {
                        "mild" -> "Mild"
                        "moderate" -> "Moderate"
                        else -> "Severe"
                    }
                }

                // Create JSON payload for Mochan endpoint
                val jsonPayload = JSONObject().apply {
                    put("to_email", parentEmail)
                    put("user_name", userName)
                    put("assessment_type", "depression")
                    put("severity", severityDisplay)
                    put("ai_prediction", aiMessage)
                    put("phq9_score", score)
                }

                Log.d("UnderageResult", "JSON Payload: $jsonPayload")

                // Mochan's endpoint (depression specific)
                val url = URL("http://203.110.243.202:8000/api/send-report-via-google-mochan")
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
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                val success = responseCode in 200..299

                Log.d("UnderageResult", "Response Code: $responseCode")
                Log.d("UnderageResult", "Response: $response")
                Log.d("UnderageResult", "Success: $success")

                withContext(Dispatchers.Main) {
                    if (success) {
                        emailSent = true
                        Toast.makeText(context, "Report sent to $parentEmail", Toast.LENGTH_LONG).show()
                    } else {
                        emailError = "Failed to send email. Please contact support."
                    }
                    isSendingEmail = false
                }

            } catch (e: Exception) {
                Log.e("UnderageResult", "Error sending email: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    emailError = e.message ?: "Unknown error occurred"
                    isSendingEmail = false
                }
            }
        }
    }

    // UI - Simple confirmation screen (NO RESULTS SHOWN)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF1F7F3), Color(0xFFFAFAFA))
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
                // Animated Icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6B9071).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "scale")
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
                        tint = Color(0xFF6B9071),
                        modifier = Modifier.size(56.dp).scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (emailSent) "Report Sent Successfully!" else "Assessment Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E4E42),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (emailSent) {
                    Text(
                        text = "Your assessment report has been sent to your parent/guardian.",
                        fontSize = 16.sp,
                        color = Color(0xFF5D6D66),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        text = "Since you're under 18, your assessment results will be shared with your parent/guardian.\n\nPlease click below to send the report.",
                        fontSize = 16.sp,
                        color = Color(0xFF5D6D66),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Send Report Button (shown only if email not sent yet)
                if (!emailSent) {
                    Button(
                        onClick = { sendReportToParent() },
                        enabled = !isSendingEmail && parentEmail.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B9071),
                            disabledContainerColor = Color(0xFFD3E4D6)
                        )
                    ) {
                        if (isSendingEmail) {
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
                                Text("Sending...", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Report to Parent", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (parentEmail.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ No parent email found. Please contact support.",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (emailError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            emailError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Success message after email sent
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Report sent successfully to $parentEmail",
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Return Home button (always visible)
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9071))
                ) {
                    Text("Return Home", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}