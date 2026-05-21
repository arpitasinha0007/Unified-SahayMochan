package com.example.unifiedapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import com.example.unifiedapp.utils.UserSessionHelper
import com.example.unifiedapp.utils.UploadHelper
import com.example.unifiedapp.utils.ReportDownloadHelper
import java.io.File
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import com.example.unifiedapp.utils.TrialHelper



// ============ DATA CLASSES ============
// test123
data class SeverityData(
    val level: String,
    val levelEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val gradient: Brush,
    val bgColor: Color,
    val lightBgColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val description: String,
    val recommendations: List<RecommendationItem>
)

data class RecommendationItem(
    val icon: String,
    val title: String,
    val description: String,
    val actionColor: Color
)

data class AiPredictionData(
    val score: Int,
    val label: String,
    val confidence: Float,
    val modelVersion: String,
    val frameCount: Int,
    val rawScore: Float
)

enum class SeverityClass {
    MILD, MODERATE, SEVERE
}

// ============ COLOR DEFINITIONS ============

val GradientBlueCyan = Brush.linearGradient(
    colors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE))
)

val GradientPurplePink = Brush.linearGradient(
    colors = listOf(Color(0xFFA855F7), Color(0xFFEC4899))
)

val GradientGreenTeal = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
)

val GradientOrangeRed = Brush.linearGradient(
    colors = listOf(Color(0xFFF97316), Color(0xFFEF4444))
)

// Severity colors
val MildColor = Color(0xFF10B981)      // Green
val MildSecondary = Color(0xFF34D399)   // Light Green
val MildGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF34D399))
)

val ModerateColor = Color(0xFFF59E0B)   // Orange
val ModerateSecondary = Color(0xFFFBBF24) // Light Orange
val ModerateGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
)

val SevereColor = Color(0xFFEF4444)     // Red
val SevereSecondary = Color(0xFFF87171) // Light Red
val SevereGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEF4444), Color(0xFFF87171))
)

val MildLightColor = Color(0xFFECFDF5)   // Light Green
val ModerateLightColor = Color(0xFFFFFBEB) // Light Orange
val SevereLightColor = Color(0xFFFFF1F2)   // Light Red

// ============ MAIN SCREEN COMPOSABLE ============

@Composable
fun ResultScreen(
    navController: NavController,
    score: Int, // Questionnaire score
    assessmentType: String = "depression" // ✅ ADDED
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDepression = assessmentType == "depression"
    val maxQuestionnaireScore = if (isDepression) 27 else 21

    val session = UserSessionHelper.getUserData(context)
    val savedEmail = session.email
    val userName = session.name
    val anonymousId = session.anonymousId
    val userAge = session.age
    val userGender = session.gender
    val registrationId = session.registrationId

    Log.d("RESULT_SCREEN", "User session - Name: $userName, Reg ID: $registrationId")

    val isUnderage = userAge < 18
    var hasRedirected by remember { mutableStateOf(false) }

    LaunchedEffect(isUnderage) {
        if (isUnderage && !hasRedirected) {
            hasRedirected = true
            val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
            val aiLabel = prefs.getString("ai_prediction_label", "N/A") ?: "N/A"
            try {
                val encodedLabel = java.net.URLEncoder.encode(aiLabel, "UTF-8")
                navController.navigate("underage_result?score=$score&aiPrediction=$encodedLabel") {
                    popUpTo(navController.currentBackStackEntry?.destination?.route ?: "launcher") { inclusive = true }
                }
            } catch (e: Exception) {
                Log.e("RESULT_SCREEN", "Navigation failed: ${e.message}")
            }
        }
    }

    if (isUnderage) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF6B9071))
        }
        return
    }

    // Retrieve AI prediction data
    val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
    val aiScore = remember { prefs.getInt("ai_prediction_score", -1) }
    val aiLabel = remember { prefs.getString("ai_prediction_label", "") ?: "" }
    val aiConfidence = remember { prefs.getFloat("ai_prediction_confidence", 0f) }
    val aiModelVersion = remember { prefs.getString("ai_model_version", "") ?: "" }
    val aiFrameCount = remember { prefs.getInt("ai_frame_count", 0) }
    val aiRawScore = remember { prefs.getFloat("ai_raw_score", 0f) }

    // Create AI data if available
    val aiData = if (aiScore != -1 && aiLabel.isNotBlank()) {
        AiPredictionData(
            score = aiScore,
            label = aiLabel,
            confidence = aiConfidence,
            modelVersion = aiModelVersion,
            frameCount = aiFrameCount,
            rawScore = aiRawScore
        )
    } else null

    // Get severity data for Questionnaire
    val phq9SeverityData = getSeverityDataFromScore(score, isDepression)

    // Get severity class for AI
    val aiSeverityData = if (aiData != null) {
        val severityClass = when {
            aiData.score <= 9 -> SeverityClass.MILD
            aiData.score <= 14 -> SeverityClass.MODERATE
            else -> SeverityClass.SEVERE
        }
        getSeverityDataFromClass(severityClass)
    } else null

    // Determine which severity is higher for recommendations
    val higherSeverity = when {
        aiSeverityData == null -> phq9SeverityData
        getSeverityLevel(phq9SeverityData.level) >= getSeverityLevel(aiSeverityData.level) -> phq9SeverityData
        else -> aiSeverityData
    }

    // State for upload
    var uploadStatus by remember { mutableStateOf<UploadStatus?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStarted by remember { mutableStateOf(false) }

    // State for download
    var isDownloading by remember { mutableStateOf(false) }

    var trialDecremented by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!trialDecremented && registrationId.isNotBlank()) {
            trialDecremented = true
            val success = TrialHelper.useDepressionTrial(registrationId)
            if (!success) {
                Toast.makeText(context, "Failed to update trial count", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Header
            item { AssessmentHeader(navController, phq9SeverityData.primaryColor) }

            // 2. Upload Button and Status Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Upload Button
                    UploadDataButton(
                        isUploading = isUploading,
                        uploadStatus = uploadStatus,
                        onUploadClick = {
                            if (!isUploading && anonymousId.isNotBlank()) {
                                coroutineScope.launch {
                                    startUpload(
                                        context = context,
                                        coroutineScope = coroutineScope,
                                        anonymousId = anonymousId,
                                        userAge = userAge,
                                        aiRawScore = aiRawScore,
                                        savedEmail = savedEmail,
                                        registrationId = registrationId,
                                        isUploading = { isUploading = it },
                                        uploadStatus = { uploadStatus = it },
                                        uploadStarted = { uploadStarted = it }

                                    )
                                }
                            }
                        }
                    )

                    // Show upload status card if upload has been started
                    if (uploadStarted && uploadStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        when (val status = uploadStatus) {
                            is UploadStatus.SUCCESS -> {
                                UploadSuccessCard(
                                    message = status.message,
                                    onDismiss = { uploadStarted = false }
                                )
                            }
                            is UploadStatus.ERROR -> {
                                UploadErrorCard(
                                    errorMessage = status.message,
                                    onRetry = {
                                        coroutineScope.launch {
                                            startUpload(
                                                context = context,
                                                coroutineScope = coroutineScope,
                                                anonymousId = anonymousId,
                                                userAge = userAge,
                                                aiRawScore = aiRawScore,
                                                savedEmail = savedEmail,
                                                registrationId = registrationId,
                                                isUploading = { isUploading = it },
                                                uploadStatus = { uploadStatus = it },
                                                uploadStarted = { uploadStarted = it }
                                            )
                                        }
                                    },
                                    isUploading = isUploading
                                )
                            }
                            is UploadStatus.UPLOADING -> {
                                UploadProgressCard()
                            }
                            null -> {}
                        }
                    }
                }
            }

            // 3. User Info Card
            item {
                UserInfoCard(
                    userName = userName,
                    userAge = userAge,
                    userGender = userGender,
                    anonymousId = anonymousId,
                    registrationId = registrationId,
                    date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                )
            }

            // 4. Side-by-side Assessment Cards
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Assessment Results",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Questionnaire Card (left)
                        EnhancedAssessmentCard(
                            title = if (isDepression) "PHQ-9" else "GAD-7",
                            severityData = phq9SeverityData,
                            icon = Icons.Default.Description,
                            gradient = GradientOrangeRed,
                            score = score,
                            maxScore = maxQuestionnaireScore,
                            modifier = Modifier.weight(1f)
                        )

                        // AI Card (right)
                        if (aiSeverityData != null) {
                            EnhancedAssessmentCard(
                                title = "AI Analysis",
                                severityData = aiSeverityData,
                                icon = Icons.Default.Face,
                                gradient = GradientBlueCyan,
                                score = aiData?.score ?: 0,
                                maxScore = 24,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Placeholder if AI data not available
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "AI Analysis\nNot Available",
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Enhanced Recommendations Card
            item {
                EnhancedRecommendationsCard(
                    severityData = higherSeverity,
                    recommendations = higherSeverity.recommendations
                )
            }

            // 6. Download Report Button
            item {
                DownloadReportButton(
                    userName = userName,
                    userAge = userAge,
                    userGender = userGender,
                    anonymousId = anonymousId,
                    registrationId = registrationId,
                    score = score,
                    phq9Severity = phq9SeverityData,
                    aiData = aiData,
                    isDownloading = isDownloading,
                    onDownloadStart = { isDownloading = true },
                    onDownloadComplete = { success ->
                        isDownloading = false
                        if (success) {
                            Toast.makeText(
                                context,
                                "PDF Report downloaded successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // 7. SETU Promotional Card
            item { EnhancedSetuPromoCard(navController) }

            // 8. Wellness Tools Card
            item { EnhancedWellnessToolsCard(navController) }
        }
    }
}

// ============ UPLOAD BUTTON COMPOSABLE ============

@Composable
fun UploadDataButton(
    isUploading: Boolean,
    uploadStatus: UploadStatus?,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GradientPurplePink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Upload Assessment Data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "Securely store your results",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Upload your assessment data to our secure server. This helps track your progress over time and enables better analysis of your mental health journey.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show upload status if exists
            if (uploadStatus is UploadStatus.SUCCESS) {
                Surface(
                    color = MildLightColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MildColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✓ Data uploaded successfully",
                            color = MildColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Button(
                onClick = onUploadClick,
                enabled = !isUploading && !(uploadStatus is UploadStatus.SUCCESS),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                isUploading -> Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF9CA3AF),
                                        Color(0xFF6B7280)
                                    )
                                )
                                uploadStatus is UploadStatus.SUCCESS -> Brush.linearGradient(
                                    colors = listOf(
                                        MildColor,
                                        MildSecondary
                                    )
                                )
                                else -> GradientPurplePink
                            },
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Uploading...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        uploadStatus is UploadStatus.SUCCESS -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Uploaded Successfully",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Upload Now",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ UPLOAD PROGRESS CARD ============

@Composable
fun UploadProgressCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F9FF))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBFDBFE)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2563EB)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Uploading Data",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = "Please wait and don't close the screen while your data is being uploaded...",
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6)
                )
            }
        }
    }
}

// ============ UPLOAD SUCCESS CARD ============

@Composable
fun UploadSuccessCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MildColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MildLightColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MildColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MildColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload Successful",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MildColor
                )
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MildColor.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MildColor
                )
            }
        }
    }
}

// ============ HELPER FUNCTION FOR UPLOAD ============

suspend fun startUpload(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    anonymousId: String,
    userAge: Int,
    aiRawScore: Float,
    savedEmail: String?,
    registrationId: String,
    isUploading: (Boolean) -> Unit,
    uploadStatus: (UploadStatus) -> Unit,
    uploadStarted: (Boolean) -> Unit
) {
    // Check if all required files exist
    val filePrefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
    val videoPath = filePrefs.getString("video_path", null)
    val auCsvPath = filePrefs.getString("au_csv_path", null)
    val phq9CsvPath = filePrefs.getString("phq9_csv_path", null)

    val missingFiles = mutableListOf<String>()
    if (videoPath == null || !File(videoPath).exists()) missingFiles.add("Video")
    if (auCsvPath == null || !File(auCsvPath).exists()) missingFiles.add("AU Data")
    if (phq9CsvPath == null || !File(phq9CsvPath).exists()) missingFiles.add("PHQ-9 Data")

    if (missingFiles.isNotEmpty()) {
        isUploading(false)
        uploadStatus(UploadStatus.ERROR("Missing files: ${missingFiles.joinToString()}"))
        uploadStarted(true)
        return
    }

    isUploading(true)
    uploadStarted(true)
    uploadStatus(UploadStatus.UPLOADING)

    try {
        UploadHelper.uploadAssessment(
            context = context,
            coroutineScope = coroutineScope,
            anonymousId = anonymousId,
            age = userAge,
            aiRawScore = aiRawScore,
            email = savedEmail,
            registrationId = registrationId,
            onProgress = { progress, message ->
                Log.d("RESULT_SCREEN", "Upload progress: $progress% - $message")
            },
            onSuccess = { message ->
                isUploading(false)
                uploadStatus(UploadStatus.SUCCESS(message))
                Toast.makeText(context, "Data uploaded successfully!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                isUploading(false)
                uploadStatus(UploadStatus.ERROR(error))
                Log.e("RESULT_SCREEN", "Upload error: $error")
            }
        )
    } catch (e: Exception) {
        isUploading(false)
        uploadStatus(UploadStatus.ERROR(e.message ?: "Unknown error"))
    }
}

// ============ UPLOAD STATUS ============

sealed class UploadStatus {
    object UPLOADING : UploadStatus()
    data class SUCCESS(val message: String) : UploadStatus()
    data class ERROR(val message: String) : UploadStatus()
}

// ============ UPLOAD ERROR CARD ============

@Composable
fun UploadErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    isUploading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF1F2))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SevereColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload Failed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevereColor
                )

                Text(
                    text = errorMessage.take(50) + if (errorMessage.length > 50) "..." else "",
                    fontSize = 13.sp,
                    color = Color(0xFF991B1B),
                    maxLines = 1
                )
            }

            Button(
                onClick = onRetry,
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SevereColor,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Retry", fontSize = 14.sp)
                }
            }
        }
    }
}

// ============ USER INFO CARD ============

@Composable
fun UserInfoCard(
    userName: String,
    userAge: Int,
    userGender: String,
    anonymousId: String,
    registrationId: String,
    date: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GradientPurplePink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Patient Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        date,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User details in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChipEnhanced(
                        label = "Name",
                        value = userName,
                        icon = Icons.Default.Person,
                        color = Color(0xFF8B5CF6)
                    )
                    InfoChipEnhanced(
                        label = "Age",
                        value = "$userAge years",
                        icon = Icons.Default.Numbers,
                        color = Color(0xFFEC4899)
                    )
                }

                // Right column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChipEnhanced(
                        label = "Gender",
                        value = userGender,
                        icon = when (userGender.lowercase(Locale.getDefault())) {
                            "male" -> Icons.Default.Male
                            "female" -> Icons.Default.Female
                            else -> Icons.Default.Transgender
                        },
                        color = Color(0xFF3B82F6)
                    )
                    InfoChipEnhanced(
                        label = "ID",
                        value = anonymousId.take(8) + "...",
                        icon = Icons.Default.Fingerprint,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Registration ID
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Registration ID: $registrationId",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChipEnhanced(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 12.sp
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    maxLines = 1
                )
            }
        }
    }
}

// ============ ENHANCED ASSESSMENT CARD ============

@Composable
fun EnhancedAssessmentCard(
    title: String,
    severityData: SeverityData,
    icon: ImageVector,
    gradient: Brush,
    score: Int,
    maxScore: Int,
    confidence: Float? = null,
    modifier: Modifier = Modifier
) {
    val progress = score.toFloat() / maxScore.toFloat()

    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Top gradient section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                severityData.primaryColor.copy(alpha = 0.15f),
                                severityData.secondaryColor.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                // Decorative circles
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = severityData.primaryColor.copy(alpha = 0.1f),
                        radius = 50.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width - 20.dp.toPx(),
                            15.dp.toPx()
                        )
                    )
                    drawCircle(
                        color = severityData.secondaryColor.copy(alpha = 0.1f),
                        radius = 30.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width - 10.dp.toPx(),
                            45.dp.toPx()
                        )
                    )
                }

                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Progress
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color(0xFFE2E8F0),
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }

                    // Progress arc
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawArc(
                            color = severityData.primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Center text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            severityData.levelEmoji,
                            fontSize = 16.sp
                        )
                        Text(
                            severityData.level,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityData.primaryColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    severityData.description,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

// ============ ENHANCED RECOMMENDATIONS CARD ============

@Composable
fun EnhancedRecommendationsCard(
    severityData: SeverityData,
    recommendations: List<RecommendationItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with severity color
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(severityData.lightBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = severityData.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "${severityData.levelEmoji} ${severityData.level} — Recommended Actions",
                        fontWeight = FontWeight.Bold,
                        color = severityData.primaryColor,
                        fontSize = 18.sp
                    )
                    Text(
                        "Based on your assessment results",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recommendations list
            recommendations.forEachIndexed { index, rec ->
                EnhancedRecommendationItem(
                    recommendation = rec,
                    isLast = index == recommendations.size - 1
                )
            }

            // Note about which assessment was used
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "These recommendations prioritize the higher severity assessment to ensure your safety and wellbeing.",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EnhancedRecommendationItem(
    recommendation: RecommendationItem,
    isLast: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(recommendation.actionColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    recommendation.icon,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recommendation.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    recommendation.description,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                color = Color(0xFFE5E7EB),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ============ DOWNLOAD REPORT BUTTON ============

@Composable
fun DownloadReportButton(
    userName: String,
    userAge: Int,
    userGender: String,
    anonymousId: String,
    registrationId: String,
    score: Int,
    phq9Severity: SeverityData,
    aiData: AiPredictionData?,
    isDownloading: Boolean,
    onDownloadStart: () -> Unit,
    onDownloadComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check notification permission for Android 13+
    val hasNotificationPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Notification permission denied. You won't receive download updates.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Show permission dialog if needed
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Enable Notifications") },
            text = {
                Text(
                    "unifiedapp needs notification permission to show you download progress " +
                            "and completion status for your assessment reports."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GradientPurplePink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Download PDF Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "Complete assessment report",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick summary of both assessments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF3F4F6),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // PHQ-9
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Questionnaire",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            phq9Severity.levelEmoji,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            phq9Severity.level,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = phq9Severity.primaryColor
                        )
                    }
                }

                // AI (if available)
                if (aiData != null) {
                    val aiSeverityClass = when {
                        aiData.score <= 9 -> SeverityClass.MILD
                        aiData.score <= 14 -> SeverityClass.MODERATE
                        else -> SeverityClass.SEVERE
                    }
                    val aiSeverity = getSeverityDataFromClass(aiSeverityClass)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "AI Analysis",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                aiSeverity.levelEmoji,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                aiSeverity.level,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = aiSeverity.primaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Download a professionally formatted PDF containing your personal information, both assessment results, and personalized recommendations.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                lineHeight = 18.sp
            )

            // Show notification permission warning if not granted on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Enable notifications to receive download updates",
                            fontSize = 12.sp,
                            color = Color(0xFF856404),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showPermissionDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF856404)
                            )
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show progress bar when downloading
            if (isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Progress bar
                    LinearProgressIndicator(
                        progress = downloadProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF8B5CF6),
                        trackColor = Color(0xFFE5E7EB)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress text with dynamic message
                    val progressMessage = when {
                        downloadProgress < 10 -> "Initializing..."
                        downloadProgress < 30 -> "Creating document..."
                        downloadProgress < 50 -> "Adding patient info..."
                        downloadProgress < 60 -> "Adding PHQ-9 results..."
                        downloadProgress < 70 -> "Adding AI analysis..."
                        downloadProgress < 80 -> "Adding recommendations..."
                        downloadProgress < 90 -> "Saving to storage..."
                        downloadProgress < 100 -> "Finalizing..."
                        else -> "Complete!"
                    }

                    Text(
                        text = "$progressMessage $downloadProgress%",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    onDownloadStart()
                    coroutineScope.launch {
                        try {
                            val filePath = ReportDownloadHelper.generateReport(
                                context = context,
                                userName = userName,
                                userAge = userAge,
                                userGender = userGender,
                                anonymousId = anonymousId,
                                registrationId = registrationId,
                                phq9Score = score,
                                phq9Severity = phq9Severity,
                                aiData = aiData,
                                onProgress = { progress ->
                                    downloadProgress = progress
                                }
                            )

                            if (filePath != null) {
                                downloadedFilePath = filePath
                                // Small delay to show 100% progress
                                delay(500)
                            }

                            onDownloadComplete(filePath != null)

                            // Reset progress after completion
                            if (filePath != null) {
                                downloadProgress = 0
                            }
                        } catch (e: Exception) {
                            Log.e("DownloadReport", "Error: ${e.message}")
                            onDownloadComplete(false)
                            downloadProgress = 0
                        }
                    }
                },
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (!isDownloading)
                                GradientPurplePink
                            else
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF9CA3AF),
                                        Color(0xFF6B7280)
                                    )
                                ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    downloadProgress < 30 -> "Preparing..."
                                    downloadProgress < 60 -> "Generating..."
                                    downloadProgress < 90 -> "Saving..."
                                    else -> "Finalizing..."
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Download PDF Report",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Show "View Report" button if a file has been downloaded
            if (downloadedFilePath != null && !isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            val file = File(downloadedFilePath!!)
                            if (!file.exists()) {
                                Toast.makeText(
                                    context,
                                    "File not found. Please download again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                downloadedFilePath = null
                                return@Button
                            }

                            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                Uri.fromFile(file)
                            }

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            }

                            // Verify that there's an app to handle PDF intents
                            val packageManager = context.packageManager
                            if (intent.resolveActivity(packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "No PDF viewer found. Please install a PDF reader app.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            Log.e("ViewReport", "Error opening PDF: ${e.message}")
                            Toast.makeText(
                                context,
                                "Could not open PDF: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Downloaded Report")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "PDF will be saved to Downloads/unifiedapp/Reports/",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Show file path if downloaded
            if (downloadedFilePath != null && !isDownloading) {
                Text(
                    text = "Last saved: ${downloadedFilePath!!.substringAfterLast("/")}",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// ============ HEADER ============

@Composable
fun AssessmentHeader(
    navController: NavController,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp)
    ) {
        TextButton(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Dashboard", fontSize = 14.sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GradientOrangeRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Assessment Results",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Questionnaire + AI Analysis",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

// ============ SETU PROMO CARD ============

@Composable
fun EnhancedSetuPromoCard(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GradientBlueCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Professional Support",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "National Comprehensive Mental Health Care Service",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "If you're facing persistent difficulties, Tele MANAS provides confidential help with trained professionals 24/7 over the phone.",
                fontSize = 14.sp,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill("One-on-one counselling", Color(0xFF3B82F6), Modifier.weight(1f))
                FeaturePill("Trained professionals", Color(0xFF22D3EE), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill("Personalized strategies", Color(0xFF3B82F6), Modifier.weight(1f))
                FeaturePill("Follow-up support", Color(0xFF22D3EE), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://telemanas.mohfw.gov.in/home")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Could not open link: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            GradientBlueCyan,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Connect with Counsellors",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
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

// ============ WELLNESS TOOLS CARD ============

@Composable
fun EnhancedWellnessToolsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GradientGreenTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Wellness Tools",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "Support your mental wellness journey",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tools grid
            val tools = listOf(
                "🧘 Guided meditation" to "Stress reduction",
                "📝 Mood tracking" to "Daily check-ins",
                "🌿 Breathing exercises" to "Anxiety relief",
                "😴 Sleep improvement" to "Better rest"
            )

            tools.chunked(2).forEach { rowTools ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTools.forEach { tool ->
                        ToolItemEnhanced(
                            title = tool.first,
                            subtitle = tool.second,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { navController.navigate("wellness") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            GradientGreenTeal,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Visit Wellness Section",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
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

@Composable
fun ToolItemEnhanced(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun FeaturePill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============ HELPER FUNCTIONS ============

fun getSeverityLevel(level: String): Int {
    return when (level.uppercase()) {
        "MILD" -> 1
        "MODERATE" -> 2
        "SEVERE" -> 3
        else -> 0
    }
}

fun getSeverityDataFromScore(score: Int, isDepression: Boolean = true): SeverityData {
    if (isDepression) {
        return when {
            score <= 9 -> SeverityData(
                level = "Mild",
                levelEmoji = "🟢",
                primaryColor = MildColor,
                secondaryColor = MildSecondary,
                gradient = MildGradient,
                bgColor = MildLightColor,
                lightBgColor = MildLightColor,
                borderColor = Color(0xFF34D399),
                icon = Icons.Default.Description,
                description = "Your responses suggest mild symptoms. Consider preventive measures and self-care.",
                recommendations = listOf(
                    RecommendationItem(
                        icon = "🧘",
                        title = "Practice daily mindfulness",
                        description = "Start with just 5-10 minutes of meditation or deep breathing exercises each day to reduce stress.",
                        actionColor = MildColor
                    ),
                    RecommendationItem(
                        icon = "🏃",
                        title = "Maintain regular exercise",
                        description = "Gentle activities like walking, yoga, or stretching can significantly improve your mood and energy levels.",
                        actionColor = MildColor
                    ),
                    RecommendationItem(
                        icon = "👪",
                        title = "Connect with others",
                        description = "Regular social connections, even brief check-ins with friends or family, help maintain emotional balance.",
                        actionColor = MildColor
                    ),
                    RecommendationItem(
                        icon = "📓",
                        title = "Keep a mood journal",
                        description = "Track your daily emotions, triggers, and coping strategies to better understand your patterns.",
                        actionColor = MildColor
                    )
                )
            )
            score <= 18 -> SeverityData(
                level = "Moderate",
                levelEmoji = "🟡",
                primaryColor = ModerateColor,
                secondaryColor = ModerateSecondary,
                gradient = ModerateGradient,
                bgColor = ModerateLightColor,
                lightBgColor = ModerateLightColor,
                borderColor = Color(0xFFFBBF24),
                icon = Icons.Default.Description,
                description = "Your responses indicate moderate symptoms. Professional guidance is recommended.",
                recommendations = listOf(
                    RecommendationItem(
                        icon = "🏥",
                        title = "Consult a mental health professional",
                        description = "Consider scheduling a session with a therapist or counselor to discuss your symptoms.",
                        actionColor = ModerateColor
                    ),
                    RecommendationItem(
                        icon = "💬",
                        title = "Build a support network",
                        description = "Connect with trusted friends, family, or support groups who can provide emotional support.",
                        actionColor = ModerateColor
                    ),
                    RecommendationItem(
                        icon = "📋",
                        title = "Create a daily routine",
                        description = "Maintain regular sleep, meals, and activities to support emotional stability.",
                        actionColor = ModerateColor
                    ),
                    RecommendationItem(
                        icon = "📊",
                        title = "Track your symptoms",
                        description = "Keep a log of your moods and triggers to identify patterns and share with your healthcare provider.",
                        actionColor = ModerateColor
                    )
                )
            )
            else -> SeverityData(
                level = "Severe",
                levelEmoji = "🔴",
                primaryColor = SevereColor,
                secondaryColor = SevereSecondary,
                gradient = SevereGradient,
                bgColor = SevereLightColor,
                lightBgColor = SevereLightColor,
                borderColor = Color(0xFFFB7185),
                icon = Icons.Default.Description,
                description = "Your responses indicate severe symptoms. Immediate professional support is advised.",
                recommendations = listOf(
                    RecommendationItem(
                        icon = "🏥",
                        title = "Seek professional help immediately",
                        description = "Contact a mental health professional, hospital, or clinic for immediate assessment and care.",
                        actionColor = SevereColor
                    ),
                    RecommendationItem(
                        icon = "📞",
                        title = "Contact crisis services",
                        description = "Call a mental health helpline (988) or local emergency number if you feel unsafe.",
                        actionColor = SevereColor
                    ),
                    RecommendationItem(
                        icon = "👥",
                        title = "Reach out to trusted people",
                        description = "Don't stay alone. Connect with someone you trust and let them know you need support.",
                        actionColor = SevereColor
                    ),
                    RecommendationItem(
                        icon = "⚕️",
                        title = "Consider intensive treatment",
                        description = "Discuss intensive outpatient programs or other treatment options with a healthcare provider.",
                        actionColor = SevereColor
                    )
                )
            )
        }
    } else {
        // GAD-7 range: 0-5 Mild, 6-10 Moderate, 11-15 Moderately Severe, 16-21 Severe
        // (Simplifying to 3 categories to match UI)
        return when {
            score <= 5 -> SeverityData(
                level = "Mild",
                levelEmoji = "🟢",
                primaryColor = MildColor,
                secondaryColor = MildSecondary,
                gradient = MildGradient,
                bgColor = MildLightColor,
                lightBgColor = MildLightColor,
                borderColor = Color(0xFF34D399),
                icon = Icons.Default.Description,
                description = "Your responses suggest mild anxiety. Continue with self-care and stress management.",
                recommendations = listOf(
                    RecommendationItem("🧘", "Breathing exercises", "Practice deep breathing when feeling tense.", MildColor),
                    RecommendationItem("🚶", "Active lifestyle", "Regular walks can help reduce anxiety levels.", MildColor)
                )
            )
            score <= 10 -> SeverityData(
                level = "Moderate",
                levelEmoji = "🟡",
                primaryColor = ModerateColor,
                secondaryColor = ModerateSecondary,
                gradient = ModerateGradient,
                bgColor = ModerateLightColor,
                lightBgColor = ModerateLightColor,
                borderColor = Color(0xFFFBBF24),
                icon = Icons.Default.Description,
                description = "Your responses indicate moderate anxiety. Consider professional guidance.",
                recommendations = listOf(
                    RecommendationItem("🏥", "Professional consultation", "Discuss your feelings with a counsellor.", ModerateColor),
                    RecommendationItem("💬", "Social support", "Talk to people you trust about your worries.", ModerateColor)
                )
            )
            else -> SeverityData(
                level = "Severe",
                levelEmoji = "🔴",
                primaryColor = SevereColor,
                secondaryColor = SevereSecondary,
                gradient = SevereGradient,
                bgColor = SevereLightColor,
                lightBgColor = SevereLightColor,
                borderColor = Color(0xFFFB7185),
                icon = Icons.Default.Description,
                description = "Your responses indicate severe anxiety. Professional support is advised.",
                recommendations = listOf(
                    RecommendationItem("🏥", "Immediate assistance", "Contact a mental health provider right away.", SevereColor),
                    RecommendationItem("📞", "Crisis support", "Reach out to Tele MANAS for immediate help.", SevereColor)
                )
            )
        }
    }
}

fun getSeverityDataFromClass(severityClass: SeverityClass): SeverityData {
    return when (severityClass) {
        SeverityClass.MILD -> SeverityData(
            level = "Mild",
            levelEmoji = "🟢",
            primaryColor = MildColor,
            secondaryColor = MildSecondary,
            gradient = MildGradient,
            bgColor = MildLightColor,
            lightBgColor = MildLightColor,
            borderColor = Color(0xFF34D399),
            icon = Icons.Default.Face,
            description = "AI analysis indicates mild symptoms. Continue with self-care practices.",
            recommendations = listOf(
                RecommendationItem(
                    icon = "🧘",
                    title = "Practice daily mindfulness",
                    description = "Start with just 5-10 minutes of meditation or deep breathing exercises each day.",
                    actionColor = MildColor
                ),
                RecommendationItem(
                    icon = "🏃",
                    title = "Maintain regular exercise",
                    description = "Gentle activities like walking or yoga can improve your mood and energy levels.",
                    actionColor = MildColor
                ),
                RecommendationItem(
                    icon = "👪",
                    title = "Connect with others",
                    description = "Regular social connections help maintain emotional balance.",
                    actionColor = MildColor
                ),
                RecommendationItem(
                    icon = "📓",
                    title = "Keep a mood journal",
                    description = "Track your daily emotions and triggers to better understand your patterns.",
                    actionColor = MildColor
                )
            )
        )
        SeverityClass.MODERATE -> SeverityData(
            level = "Moderate",
            levelEmoji = "🟡",
            primaryColor = ModerateColor,
            secondaryColor = ModerateSecondary,
            gradient = ModerateGradient,
            bgColor = ModerateLightColor,
            lightBgColor = ModerateLightColor,
            borderColor = Color(0xFFFBBF24),
            icon = Icons.Default.Face,
            description = "AI analysis indicates moderate symptoms. Professional guidance is recommended.",
            recommendations = listOf(
                RecommendationItem(
                    icon = "🏥",
                    title = "Consider professional consultation",
                    description = "Schedule a session with a mental health professional to discuss your symptoms.",
                    actionColor = ModerateColor
                ),
                RecommendationItem(
                    icon = "💬",
                    title = "Build a support network",
                    description = "Connect with trusted friends, family, or support groups.",
                    actionColor = ModerateColor
                ),
                RecommendationItem(
                    icon = "📋",
                    title = "Create a daily routine",
                    description = "Maintain regular sleep, meals, and activities for emotional stability.",
                    actionColor = ModerateColor
                ),
                RecommendationItem(
                    icon = "📊",
                    title = "Track your symptoms",
                    description = "Keep a log of your moods to identify patterns.",
                    actionColor = ModerateColor
                )
            )
        )
        SeverityClass.SEVERE -> SeverityData(
            level = "Severe",
            levelEmoji = "🔴",
            primaryColor = SevereColor,
            secondaryColor = SevereSecondary,
            gradient = SevereGradient,
            bgColor = SevereLightColor,
            lightBgColor = SevereLightColor,
            borderColor = Color(0xFFFB7185),
            icon = Icons.Default.Face,
            description = "AI analysis indicates severe symptoms. Immediate support is advised.",
            recommendations = listOf(
                RecommendationItem(
                    icon = "🏥",
                    title = "Seek professional help immediately",
                    description = "Contact a mental health professional or hospital for immediate assessment.",
                    actionColor = SevereColor
                ),
                RecommendationItem(
                    icon = "📞",
                    title = "Contact crisis services",
                    description = "Call a mental health helpline (988) if you feel unsafe.",
                    actionColor = SevereColor
                ),
                RecommendationItem(
                    icon = "👥",
                    title = "Reach out for support",
                    description = "Don't stay alone. Connect with someone you trust.",
                    actionColor = SevereColor
                ),
                RecommendationItem(
                    icon = "⚕️",
                    title = "Consider intensive treatment",
                    description = "Discuss treatment options with a healthcare provider.",
                    actionColor = SevereColor
                )
            )
        )
    }
}