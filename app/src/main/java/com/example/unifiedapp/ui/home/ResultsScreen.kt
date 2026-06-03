//package com.example.unifiedapp.ui.home
//
//import android.Manifest
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.*
//import androidx.compose.ui.draw.scale
//import kotlinx.coroutines.delay
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.scaleIn
//import androidx.compose.animation.slideInVertically
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.CloudUpload
//import androidx.compose.material.icons.filled.Download
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.SelfImprovement
//import androidx.compose.material.icons.filled.Spa
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material.icons.filled.Error
//import androidx.compose.material.icons.filled.Warning
//import androidx.compose.material.icons.filled.Psychology
//import androidx.compose.material.icons.filled.Assessment
//import androidx.compose.material.icons.filled.CloudDone
//import androidx.compose.material.icons.filled.Pending
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.core.content.ContextCompat
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import com.example.unifiedapp.ui.remote.SimpleServerClient
//import com.example.unifiedapp.ui.views.AssessmentData
//import com.example.unifiedapp.ui.views.EmailStatus
//import com.example.unifiedapp.ui.views.QuizResultViewModel
//import com.example.unifiedapp.ui.views.UserData
//import com.example.unifiedapp.ui.views.UserPreferences
//import com.example.unifiedapp.ui.repository.EmailRepository
//import com.example.unifiedapp.vision.ReportDownloadHelper
//import com.example.unifiedapp.vision.NotificationHelper
//import com.example.unifiedapp.ui.home.MenuScreen
//import android.util.Log
//import kotlinx.coroutines.launch
//import android.widget.Toast
//import androidx.navigation.NavController
//import com.example.unifiedapp.ui.views.AuthViewModel
//import com.example.unifiedapp.ui.navigation.Screen
//
//
//// Color palette - Updated for better contrast
//val SageGreen = Color(0xFF2E7D32) // Darker green for better visibility
//val SoftSage = Color(0xFF388E3C) // Medium green
//val LightSage = Color(0xFFE8F5E9) // Very light green background
//
//val TextMedium = Color(0xFF757575) // Medium gray
//val SuccessGreen = Color(0xFF4CAF50)
//val WarningOrange = Color(0xFFFF9800)
//val AlertRed = Color(0xFFF44336)
//val SurfaceWhite = Color(0xFFFEFDF7)
//val TextPrimary = Color(0xFF4A4A4A)
//
//
//val BorderColor = Color(0xFFE0E0E0) // Light gray border
//
//// Data classes for report generation (keeping them for local use if needed)
//data class ResultStateData(
//    val title: String,
//    val message: String,
//    val emoji: String,
//    val color: Int
//)
//
//data class AiPredictionData(
//    val anxietyPrediction: String,
//    val anxietyScore: Float,
//    val anxietyConfidence: Float
//)
//
//// Severity levels - UPDATED to 3 categories only
//enum class SeverityLevel {
//    MILD, MODERATE, SEVERE
//}
//
//// UPDATED: Get severity from GAD-7 score (3 categories)
//fun getSeverityFromGAD7(score: Int): SeverityLevel = when (score) {
//    in 0..7 -> SeverityLevel.MILD      // Mild: 0-7
//    in 8..14 -> SeverityLevel.MODERATE // Moderate: 8-14
//    else -> SeverityLevel.SEVERE       // Severe: 15-21
//}
//
//// UPDATED: Get severity from AI prediction (3 categories)
//fun getSeverityFromAI(prediction: String): SeverityLevel {
//    return when {
//        prediction.contains("mild", ignoreCase = true) -> SeverityLevel.MILD
//        prediction.contains("moderate", ignoreCase = true) -> SeverityLevel.MODERATE
//        prediction.contains("severe", ignoreCase = true) -> SeverityLevel.SEVERE
//        else -> SeverityLevel.MILD // Default to mild if not recognized
//    }
//}
//
//// UPDATED: Get overall severity
//fun getOverallSeverity(gad7Score: Int, aiPrediction: String): SeverityLevel {
//    val gad7Severity = getSeverityFromGAD7(gad7Score)
//    val aiSeverity = getSeverityFromAI(aiPrediction)
//
//    // Return the higher severity level
//    return maxOf(gad7Severity, aiSeverity, compareBy { it.ordinal })
//}
//
//// UPDATED: Display strings for 3 categories
//fun getSeverityDisplay(severity: SeverityLevel): String = when (severity) {
//    SeverityLevel.MILD -> "Mild Anxiety"
//    SeverityLevel.MODERATE -> "Moderate Anxiety"
//    SeverityLevel.SEVERE -> "Severe Anxiety"
//}
//
//// UPDATED: Colors for 3 categories
//fun getSeverityColor(severity: SeverityLevel): Color = when (severity) {
//    SeverityLevel.MILD -> SuccessGreen      // Green for mild
//    SeverityLevel.MODERATE -> WarningOrange // Orange for moderate
//    SeverityLevel.SEVERE -> AlertRed       // Red for severe
//}
//
//// UPDATED: Emojis for 3 categories
//fun getSeverityEmoji(severity: SeverityLevel): String = when (severity) {
//    SeverityLevel.MILD -> "🌱"
//    SeverityLevel.MODERATE -> "🤝"
//    SeverityLevel.SEVERE -> "🫂"
//}
//
//// UPDATED: Message titles for 3 categories
//fun getMessageTitle(severity: SeverityLevel): String = when (severity) {
//    SeverityLevel.MILD -> "You're Doing Well"
//    SeverityLevel.MODERATE -> "Here for You"
//    SeverityLevel.SEVERE -> "Take a Moment"
//}
//
//// UPDATED: Recommendations for 3 categories
//fun getRecommendations(severity: SeverityLevel): List<String> = when (severity) {
//    SeverityLevel.MILD -> listOf(
//        "🌿 Keep up your healthy routines",
//        "🧘 Practice 5-minute morning mindfulness",
//        "📚 Read something uplifting today",
//        "💭 Try journaling your thoughts"
//    )
//    SeverityLevel.MODERATE -> listOf(
//        "🫁 Practice 4-7-8 breathing technique",
//        "🧘 Try a guided meditation",
//        "📞 Talk to someone you trust",
//        "🎯 Break tasks into smaller steps"
//    )
//    SeverityLevel.SEVERE -> listOf(
//        "🆘 Consider speaking with a counselor",
//        "📞 Call a mental health helpline",
//        "🧘 Focus on deep breathing techniques",
//        "🌙 Prioritize rest and self-care"
//    )
//}
//
//class QuizResultViewModelFactory(
//    private val context: Context,
//    private val repository: EmailRepository = EmailRepository()
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(QuizResultViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return QuizResultViewModel(context, repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ResultsScreen(
//    score: Int,
//    gad7Score: Int,
//    assessmentData: AssessmentData?,
//    onFinish: () -> Unit,
//    userPreferences: UserPreferences,
//    navController: NavController,
//    authViewModel: AuthViewModel  // NEW: for trial decrement
//) {
//    val context = LocalContext.current
//    val viewModel: QuizResultViewModel = viewModel(
//        factory = QuizResultViewModelFactory(context)
//    )
//    val state by viewModel.uiState.collectAsState()
//    val emailStatus by viewModel.status.collectAsState()
//
//    var isUploading by remember { mutableStateOf(false) }
//    var uploadComplete by remember { mutableStateOf(false) }
//    var uploadError by remember { mutableStateOf<String?>(null) }
//    var uploadProgress by remember { mutableStateOf(0) }
//    var uploadStage by remember { mutableStateOf("") }
//    var uploadedBytes by remember { mutableStateOf(0L) }
//    var totalBytes by remember { mutableStateOf(0L) }
//
//    val overallSeverity = getOverallSeverity(gad7Score, state.anxietyPrediction)
//    val severityDisplay = getSeverityDisplay(overallSeverity)
//    val severityColor = getSeverityColor(overallSeverity)
//    val severityEmoji = getSeverityEmoji(overallSeverity)
//    val messageTitle = getMessageTitle(overallSeverity)
//    val recommendations = getRecommendations(overallSeverity)
//
//    val gad7Severity = getSeverityFromGAD7(gad7Score)
//    val gad7Display = getSeverityDisplay(gad7Severity)
//    val gad7Color = getSeverityColor(gad7Severity)
//
//    val aiSeverity = getSeverityFromAI(state.anxietyPrediction)
//    val aiDisplay = getSeverityDisplay(aiSeverity)
//    val aiColor = getSeverityColor(aiSeverity)
//
//    val scope = rememberCoroutineScope()
//    val userData by userPreferences.userData.collectAsState(
//        initial = UserData(false, "", "", "", age = 0, id = "", "")
//    )
//
//    // ──────────────────────────────────────────────────────────────
//    // DECREMENT ANXIETY TRIAL WHEN RESULTS ARE FIRST DISPLAYED
//    // Uses registration_id from UserPreferences (stored as userData.id)
//    // ──────────────────────────────────────────────────────────────
//    LaunchedEffect(Unit) {
//        // Get registration ID directly from preferences (suspending)
//        val registrationId = userPreferences.getRegistrationId()
//        if (!registrationId.isNullOrBlank()) {
//            Log.d("TRIAL", "Decrementing anxiety trial for registration_id: $registrationId")
//            authViewModel.decrementAnxietyTrial(assessmentData?.anonymousId)
//        } else {
//            Log.e("TRIAL", "Cannot decrement trial: registration_id is missing")
//        }
//    }
//
//    // Permission launcher for notifications (Android 13+)
//    val notificationPermissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (!isGranted) {
//            Toast.makeText(
//                context,
//                "Notification permission denied. You won't receive download notifications.",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    }
//
//    // Function to generate and download report
//    fun generateAndDownloadReport(showNotification: Boolean = true) {
//        scope.launch {
//            try {
//                // Convert local data classes to ReportDownloadHelper's versions
//                val resultState = ReportDownloadHelper.ResultStateData(
//                    title = messageTitle,
//                    message = "Based on your responses, you're experiencing $severityDisplay",
//                    emoji = severityEmoji,
//                    color = severityColor.hashCode()
//                )
//
//                val aiData = ReportDownloadHelper.AiPredictionData(
//                    anxietyPrediction = state.anxietyPrediction,
//                    anxietyScore = state.anxietyScore.toFloat(),
//                    anxietyConfidence = state.anxietyConfidence.toFloat()
//                )
//
//                val filePath = ReportDownloadHelper.generateReport(
//                    context = context,
//                    score = gad7Score,
//                    resultState = resultState,
//                    aiData = aiData,
//                    anonymousId = userData.id.ifEmpty { "user" }
//                )
//
//                if (filePath != null) {
//                    if (showNotification) {
//                        Toast.makeText(
//                            context,
//                            "Report saved to Downloads/Sahay/Reports/",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                } else {
//                    Toast.makeText(
//                        context,
//                        "Failed to generate report",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } catch (e: Exception) {
//                Toast.makeText(
//                    context,
//                    "Error: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }
//
//    // Create notification channel when screen loads
//    LaunchedEffect(Unit) {
//        NotificationHelper.createNotificationChannel(context)
//
//        // Request notification permission for Android 13+
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//            }
//        }
//    }
//
//    Scaffold(
//        containerColor = SurfaceWhite,
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        "Assessment Results",
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold,
//                        color = TextPrimary
//                    )
//                },
//                actions = {
//                    IconButton(onClick = { /* Show info */ }) {
//                        Icon(Icons.Default.Info, contentDescription = "Info", tint = TextMedium)
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.White,
//                    scrolledContainerColor = Color.White
//                )
//            )
//        }
//    ) { padding ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding),
//            contentPadding = PaddingValues(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            // Upload Card - Now at the top with improved progress bar
//            if (assessmentData != null) {
//                item {
//                    UploadCard(
//                        assessmentData = assessmentData,
//                        isUploading = isUploading,
//                        uploadComplete = uploadComplete,
//                        uploadError = uploadError,
//                        uploadProgress = uploadProgress,
//                        uploadStage = uploadStage,
//                        uploadedBytes = uploadedBytes,
//                        totalBytes = totalBytes,
//                        onUpload = { data ->
//                            isUploading = true
//                            uploadProgress = 0
//                            uploadedBytes = 0
//                            totalBytes = 15 * 1024 * 1024 // Example: 15MB total
//                            scope.launch {
//                                try {
//                                    val serverClient = SimpleServerClient(context)
//
//                                    // Simulate progress stages with byte counts
//                                    uploadStage = "Preparing files..."
//                                    delay(500)
//
//                                    uploadStage = "Uploading video..."
//                                    for (i in 1..3) {
//                                        delay(300)
//                                        uploadProgress = i * 10
//                                        uploadedBytes = (uploadProgress * totalBytes / 100).coerceAtMost(totalBytes)
//                                    }
//
//                                    uploadStage = "Uploading AU data..."
//                                    for (i in 4..6) {
//                                        delay(300)
//                                        uploadProgress = i * 10
//                                        uploadedBytes = (uploadProgress * totalBytes / 100).coerceAtMost(totalBytes)
//                                    }
//
//                                    uploadStage = "Uploading GAD-7 data..."
//                                    for (i in 7..9) {
//                                        delay(300)
//                                        uploadProgress = i * 10
//                                        uploadedBytes = (uploadProgress * totalBytes / 100).coerceAtMost(totalBytes)
//                                    }
//
//                                    uploadStage = "Finalizing..."
//                                    uploadProgress = 95
//                                    uploadedBytes = (95 * totalBytes / 100).coerceAtMost(totalBytes)
//                                    delay(400)
//
//                                    // Actual upload call
//                                    serverClient.uploadAnonymousAssessment(data, object : SimpleServerClient.UploadCallback {
//                                        override fun onProgress(progress: Int, message: String) {
//                                            scope.launch {
//                                                uploadProgress = progress
//                                                uploadStage = message
//                                                uploadedBytes = (progress * totalBytes / 100).coerceAtMost(totalBytes)
//                                            }
//                                        }
//                                        override fun onSuccess(message: String) {
//                                            scope.launch {
//                                                uploadProgress = 100
//                                                uploadedBytes = totalBytes
//                                                uploadStage = "Complete!"
//                                                delay(500)
//                                                isUploading = false
//                                                uploadComplete = true
//                                                uploadError = null
//                                                Toast.makeText(context, "✅ Upload successful!", Toast.LENGTH_LONG).show()
//                                            }
//                                        }
//                                        override fun onError(error: String) {
//                                            scope.launch {
//                                                isUploading = false
//                                                uploadComplete = false
//                                                uploadError = error
//                                                Toast.makeText(context, "❌ Upload failed: $error", Toast.LENGTH_LONG).show()
//                                            }
//                                        }
//                                    })
//                                } catch (e: Exception) {
//                                    isUploading = false
//                                    uploadError = e.message
//                                }
//                            }
//                        }
//                    )
//                }
//            }
//
//            // Welcome Message Card
//            item {
//                WelcomeMessageCard(
//                    title = messageTitle,
//                    emoji = severityEmoji,
//                    message = "Based on your responses, you're experiencing $severityDisplay",
//                    color = severityColor
//                )
//            }
//
//            // Side-by-side Analysis Cards
//            item {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // GAD-7 Analysis Card
//                    AnalysisCard(
//                        title = "Questionnaire",
//                        icon = Icons.Default.Assessment,
//                        severity = gad7Display,
//                        color = gad7Color,
//                        details = "",
//                        modifier = Modifier.weight(1f)
//                    )
//
//                    // AI Analysis Card
//                    AnalysisCard(
//                        title = "AI Analysis",
//                        icon = Icons.Default.Psychology,
//                        severity = aiDisplay,
//                        color = aiColor,
//                        details="",
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//            }
//
//            // AI Insight Details (if available and different from summary)
//            if (state.anxietyPrediction.isNotBlank() &&
//                !state.anxietyPrediction.contains(aiDisplay, ignoreCase = true)) {
//                item {
//                    AiDetailCard(
//                        prediction = state.anxietyPrediction
//                    )
//                }
//            }
//
//            // Recommendations Section
//            item {
//                RecommendationsSection(
//                    recommendations = recommendations,
//                    severity = severityDisplay
//                )
//            }
//
//            // SETU Counseling Card - Show for Moderate and Severe only
//            if (overallSeverity == SeverityLevel.MILD || overallSeverity == SeverityLevel.MODERATE || overallSeverity == SeverityLevel.SEVERE) {
//                item {
//                    SetuCounselingCard()
//                }
//            }
//
//            // Quick Actions
//            item {
//                QuickActionsRow(
//                    onDownload = {
//                        generateAndDownloadReport(showNotification = true)
//                    },
//                    onCope = {
//                        // Navigate to WealthScreen (Coping Strategies)
//                        navController.navigate(Screen.Menu.route)
//                    }
//                )
//            }
//
//            // Return Home Button
//            item {
//                ReturnHomeButton(
//                    onClick = onFinish,
//                    color = severityColor
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun WelcomeMessageCard(
//    title: String,
//    emoji: String,
//    message: String,
//    color: Color
//) {
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Surface(
//                color = color.copy(alpha = 0.1f),
//                shape = CircleShape,
//                modifier = Modifier.size(48.dp)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Text(
//                        text = emoji,
//                        fontSize = 24.sp
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = title,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = TextPrimary
//                )
//                Text(
//                    text = message,
//                    fontSize = 13.sp,
//                    color = TextMedium,
//                    lineHeight = 18.sp
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun AnalysisCard(
//    title: String,
//    icon: ImageVector,
//    severity: String,
//    color: Color,
//    details: String,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
//        modifier = modifier
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    icon,
//                    contentDescription = null,
//                    tint = color,
//                    modifier = Modifier.size(20.dp)
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//                Text(
//                    text = title,
//                    fontSize = 13.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = TextMedium
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Surface(
//                color = color.copy(alpha = 0.1f),
//                shape = RoundedCornerShape(8.dp),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = severity,
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = color,
//                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(4.dp))
//
//            Text(
//                text = details,
//                fontSize = 11.sp,
//                color = TextMedium,
//                modifier = Modifier.padding(start = 4.dp)
//            )
//        }
//    }
//}
//
//@Composable
//fun AiDetailCard(
//    prediction: String
//) {
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Text(
//                text = "AI Insight",
//                fontSize = 13.sp,
//                fontWeight = FontWeight.Medium,
//                color = SageGreen
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = prediction,
//                fontSize = 13.sp,
//                color = TextPrimary,
//                lineHeight = 18.sp
//            )
//        }
//    }
//}
//
//@Composable
//fun UploadCard(
//    assessmentData: AssessmentData,
//    isUploading: Boolean,
//    uploadComplete: Boolean,
//    uploadError: String?,
//    uploadProgress: Int,
//    uploadStage: String,
//    uploadedBytes: Long,
//    totalBytes: Long,
//    onUpload: (AssessmentData) -> Unit
//) {
//    var expanded by remember { mutableStateOf(true) }
//
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            // Header
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        Icons.Default.CloudUpload,
//                        contentDescription = null,
//                        tint = SageGreen
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = "Upload Data",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = TextPrimary
//                    )
//                }
//                IconButton(onClick = { expanded = !expanded }) {
//                    Icon(
//                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = "Toggle",
//                        tint = TextMedium
//                    )
//                }
//            }
//
//            // Quick Status Chips
//            Row(
//                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                FileStatusChip("Video", assessmentData.videoFile?.exists() == true)
//                FileStatusChip("AU Data", assessmentData.auCsvFile?.exists() == true)
//                FileStatusChip("GAD-7", assessmentData.gad7CsvFile?.exists() == true)
//            }
//
//            AnimatedVisibility(
//                visible = expanded,
//                enter = fadeIn() + slideInVertically(),
//                exit = fadeOut()
//            ) {
//                Column {
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Progress Section
//                    if (isUploading || uploadComplete) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .background(
//                                    color = LightSage,
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//                                .padding(16.dp)
//                        ) {
//                            if (isUploading) {
//                                // Current Stage with Icon
//                                Row(
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    modifier = Modifier.fillMaxWidth()
//                                ) {
//                                    Icon(
//                                        Icons.Default.CloudUpload,
//                                        contentDescription = null,
//                                        tint = SageGreen,
//                                        modifier = Modifier.size(18.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(6.dp))
//                                    Text(
//                                        text = uploadStage,
//                                        fontSize = 14.sp,
//                                        fontWeight = FontWeight.Medium,
//                                        color = TextPrimary
//                                    )
//                                }
//
//                                Spacer(modifier = Modifier.height(12.dp))
//
//                                // Main Progress Bar
//                                Box(
//                                    modifier = Modifier.fillMaxWidth()
//                                ) {
//                                    LinearProgressIndicator(
//                                        progress = { uploadProgress / 100f },
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .height(12.dp)
//                                            .clip(RoundedCornerShape(6.dp)),
//                                        color = SageGreen,
//                                        trackColor = SageGreen.copy(alpha = 0.2f)
//                                    )
//                                }
//
//                                Spacer(modifier = Modifier.height(8.dp))
//
//                                // Progress Stats
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceBetween,
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    // Percentage
//                                    Text(
//                                        text = "$uploadProgress%",
//                                        fontSize = 18.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = SageGreen
//                                    )
//
//                                    // Bytes transferred
//                                    if (totalBytes > 0) {
//                                        Text(
//                                            text = "${formatBytes(uploadedBytes)} / ${formatBytes(totalBytes)}",
//                                            fontSize = 12.sp,
//                                            color = TextMedium
//                                        )
//                                    }
//                                }
//
//                                Spacer(modifier = Modifier.height(12.dp))
//
//                                // Individual file progress
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceEvenly
//                                ) {
//                                    FileProgressChip(
//                                        label = "Video",
//                                        progress = when {
//                                            uploadProgress >= 30 -> 100
//                                            uploadProgress > 0 -> (uploadProgress / 30f * 100).toInt().coerceIn(0, 100)
//                                            else -> 0
//                                        }
//                                    )
//                                    FileProgressChip(
//                                        label = "AU Data",
//                                        progress = when {
//                                            uploadProgress >= 60 -> 100
//                                            uploadProgress > 30 -> ((uploadProgress - 30) / 30f * 100).toInt().coerceIn(0, 100)
//                                            else -> 0
//                                        }
//                                    )
//                                    FileProgressChip(
//                                        label = "GAD-7",
//                                        progress = when {
//                                            uploadProgress >= 90 -> 100
//                                            uploadProgress > 60 -> ((uploadProgress - 60) / 30f * 100).toInt().coerceIn(0, 100)
//                                            else -> 0
//                                        }
//                                    )
//                                }
//
//                            }
//
//                            if (uploadComplete) {
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.Center
//                                ) {
//                                    Icon(
//                                        Icons.Default.CloudDone,
//                                        contentDescription = null,
//                                        tint = SuccessGreen,
//                                        modifier = Modifier.size(24.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
//                                    Column {
//                                        Text(
//                                            text = "Upload Complete!",
//                                            fontSize = 16.sp,
//                                            color = SuccessGreen,
//                                            fontWeight = FontWeight.Bold
//                                        )
//                                        if (totalBytes > 0) {
//                                            Text(
//                                                text = "${formatBytes(totalBytes)} uploaded",
//                                                fontSize = 11.sp,
//                                                color = TextMedium
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    if (uploadError != null) {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Surface(
//                            color = AlertRed.copy(alpha = 0.1f),
//                            shape = RoundedCornerShape(8.dp),
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text(
//                                text = uploadError,
//                                fontSize = 12.sp,
//                                color = AlertRed,
//                                modifier = Modifier.padding(8.dp)
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Upload Button
//                    Button(
//                        onClick = { onUpload(assessmentData) },
//                        enabled = !isUploading && !uploadComplete,
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(12.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = SageGreen,
//                            disabledContainerColor = SageGreen.copy(alpha = 0.5f)
//                        )
//                    ) {
//                        if (isUploading) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(20.dp),
//                                strokeWidth = 2.dp,
//                                color = Color.White
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Uploading... $uploadProgress%")
//                        } else if (uploadComplete) {
//                            Icon(Icons.Default.CheckCircle, contentDescription = null)
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Uploaded")
//                        } else {
//                            Text("Upload to Server")
//                        }
//                    }
//
//                    Text(
//                        text = "Your data is encrypted and securely stored",
//                        fontSize = 10.sp,
//                        color = TextMedium,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(top = 8.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun FileProgressChip(
//    label: String,
//    progress: Int
//) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Surface(
//            color = if (progress >= 100) SuccessGreen.copy(alpha = 0.1f)
//            else WarningOrange.copy(alpha = 0.1f),
//            shape = CircleShape,
//            modifier = Modifier.size(36.dp)
//        ) {
//            Box(contentAlignment = Alignment.Center) {
//                if (progress >= 100) {
//                    Icon(
//                        Icons.Default.CheckCircle,
//                        contentDescription = null,
//                        tint = SuccessGreen,
//                        modifier = Modifier.size(20.dp)
//                    )
//                } else {
//                    Box(
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator(
//                            progress = { progress / 100f },
//                            modifier = Modifier.size(28.dp),
//                            strokeWidth = 2.dp,
//                            color = WarningOrange
//                        )
//                        Text(
//                            text = "$progress",
//                            fontSize = 8.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = WarningOrange
//                        )
//                    }
//                }
//            }
//        }
//        Text(
//            text = label,
//            fontSize = 10.sp,
//            color = TextMedium,
//            modifier = Modifier.padding(top = 2.dp)
//        )
//    }
//}
//
//@Composable
//fun FileStatusChip(label: String, exists: Boolean) {
//    Surface(
//        color = if (exists) SuccessGreen.copy(alpha = 0.1f) else AlertRed.copy(alpha = 0.1f),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                if (exists) Icons.Default.CheckCircle else Icons.Default.Pending,
//                contentDescription = null,
//                tint = if (exists) SuccessGreen else AlertRed,
//                modifier = Modifier.size(12.dp)
//            )
//            Spacer(modifier = Modifier.width(4.dp))
//            Text(
//                text = label,
//                fontSize = 10.sp,
//                color = if (exists) SuccessGreen else AlertRed
//            )
//        }
//    }
//}
//
//@Composable
//fun RecommendationsSection(
//    recommendations: List<String>,
//    severity: String
//) {
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = "Recommendations",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold,
//                color = TextPrimary
//            )
//
//            Text(
//                text = "Based on your results",
//                fontSize = 12.sp,
//                color = TextMedium,
//                modifier = Modifier.padding(top = 2.dp)
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            recommendations.take(4).forEach { recommendation ->
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 6.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = recommendation.split(" ").first(),
//                        fontSize = 20.sp,
//                        modifier = Modifier.width(32.dp)
//                    )
//                    Text(
//                        text = recommendation.substringAfter(" "),
//                        fontSize = 14.sp,
//                        color = TextPrimary,
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun QuickActionsRow(
//    onDownload: () -> Unit,
//    onCope: () -> Unit
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        ActionButton(
//            icon = Icons.Default.Download,
//            label = "Download Report",
//            onClick = onDownload,
//            color = SageGreen,
//            modifier = Modifier.weight(1f)
//        )
//        ActionButton(
//            icon = Icons.Default.SelfImprovement,
//            label = "Coping Strategies",
//            onClick = onCope,
//            color = SoftSage,
//            modifier = Modifier.weight(1f)
//        )
//    }
//}
//
//@Composable
//fun ActionButton(
//    icon: ImageVector,
//    label: String,
//    onClick: () -> Unit,
//    color: Color,
//    modifier: Modifier = Modifier
//) {
//    Surface(
//        onClick = onClick,
//        color = color.copy(alpha = 0.1f),
//        shape = RoundedCornerShape(16.dp),
//        modifier = modifier.height(70.dp)
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = label,
//                tint = color,
//                modifier = Modifier.size(24.dp)
//            )
//            Text(
//                text = label,
//                fontSize = 11.sp,
//                fontWeight = FontWeight.Medium,
//                color = color,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//@Composable
//fun SetuCounselingCard() {
//    val context = LocalContext.current
//
//    Card(
//        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Surface(
//                color = LightSage,
//                shape = CircleShape,
//                modifier = Modifier.size(48.dp)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Icon(
//                        Icons.Default.Spa,
//                        contentDescription = null,
//                        tint = SageGreen,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = "National Mental Health Support (Tele MANAS)",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = TextPrimary
//                )
//                Text(
//                    text = "Professional counsellors available 24/7",
//                    fontSize = 12.sp,
//                    color = TextMedium
//                )
//            }
//
//            Button(
//                onClick = {
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telemanas.mohfw.gov.in/home"))
//                    context.startActivity(intent)
//                },
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = SageGreen
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text("Connect", fontSize = 12.sp)
//            }
//        }
//    }
//}
//
//@Composable
//fun ReturnHomeButton(
//    onClick: () -> Unit,
//    color: Color
//) {
//    Button(
//        onClick = onClick,
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(56.dp),
//        colors = ButtonDefaults.buttonColors(
//            containerColor = color
//        ),
//        shape = RoundedCornerShape(16.dp)
//    ) {
//        Text(
//            "Return Home",
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Bold,
//            color = Color.White
//        )
//    }
//}
//
//// Helper function to format bytes
//fun formatBytes(bytes: Long): String {
//    val kb = bytes / 1024
//    val mb = kb / 1024
//    return when {
//        mb > 0 -> "${mb}MB"
//        kb > 0 -> "${kb}KB"
//        else -> "${bytes}B"
//    }
//}