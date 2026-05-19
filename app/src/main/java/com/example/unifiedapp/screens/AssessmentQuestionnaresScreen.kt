package com.example.unifiedapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.camera.core.UseCase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unifiedapp.R
import com.example.unifiedapp.ui.views.CameraViewModel
import com.example.unifiedapp.utils.UserSessionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

// ============ MOCHAN COLOR PALETTE ============
val MochanPurplePrimary = Color(0xFF8B5CF6)
val MochanPurpleSecondary = Color(0xFFA78BFA)
val MochanBackgroundLight = Color(0xFFF5F3FF)
val MochanSurfaceWhite = Color.White

// ============ SAHAY COLOR PALETTE ============
val SahaySageLight = Color(0xFFF1F7F3)
val SahaySageMedium = Color(0xFFD3E4D6)
val SahaySageAccent = Color(0xFF6B9071)
val SahayCharcoal = Color(0xFF3E4E42)
val SahayMutedSlate = Color(0xFF5D6D66)

// --- DATA CLASSES ---
data class AnswerOption(
    val value: String,
    val label: String,
    val score: Int,
    val emoji: String
)

// --- CONSTANTS ---
val PHQ9_QUESTIONS = listOf(
    "Little interest or pleasure in doing things",
    "Feeling down, depressed, or hopeless",
    "Trouble falling or staying asleep, or sleeping too much",
    "Feeling tired or having little energy",
    "Poor appetite or overeating",
    "Feeling bad about yourself or that you are a failure",
    "Trouble concentrating on things",
    "Moving or speaking slowly or being fidgety",
    "Thoughts that you would be better off dead",
)

val GAD7_QUESTIONS = listOf(
    "Feeling nervous, anxious, or on edge",
    "Not being able to stop or control worrying",
    "Worrying too much about different things",
    "Trouble relaxing",
    "Being so restless that it's hard to sit still",
    "Becoming easily annoyed or irritable",
    "Feeling afraid as if something awful might happen"
)

val ANSWER_OPTIONS = listOf(
    AnswerOption("0", "Not at all", 0, "😊"),
    AnswerOption("1", "Several days", 1, "😐"),
    AnswerOption("2", "More than half the days", 2, "😔"),
    AnswerOption("3", "Nearly every day", 3, "😢"),
)

// --- MAIN SCREEN ---
@Composable
fun AssessmentQuestionnairesScreen(
    navController: NavController,
    cameraViewModel: CameraViewModel = viewModel(),
    assessmentType: String = "depression"
) {
    val isDepression = assessmentType == "depression"
    val questions = if (isDepression) PHQ9_QUESTIONS else GAD7_QUESTIONS

    val backgroundColor = if (isDepression) MochanBackgroundLight else SahaySageLight
    val primaryColor = if (isDepression) MochanPurplePrimary else SahaySageAccent
    val gradientColors = if (isDepression)
        listOf(MochanPurplePrimary, MochanPurpleSecondary)
    else
        listOf(SahaySageAccent, SahaySageMedium)

    var answers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    var cameraActive by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var isCompleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val session = UserSessionHelper.getUserData(context)
    val anonymousId = session.anonymousId

    // Collect state flows from CameraViewModel
    val faceDetected by cameraViewModel.faceDetected.collectAsState()
    val frameCount by cameraViewModel.frameCount.collectAsState()
    val isRecording by cameraViewModel.isRecording.collectAsState()
    val cameraError by cameraViewModel.cameraError.collectAsState()
    val au17Value by cameraViewModel.au17Value.collectAsState()
    val isInitialized by cameraViewModel.isInitialized.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraActive = isGranted
        permissionRequested = true
        if (isGranted) {
            permissionDeniedPermanently = false
            cameraViewModel.initialize(context, anonymousId)
            coroutineScope.launch {
                delay(500)
                cameraViewModel.startRecording()
            }
        } else {
            cameraActive = false
            val activity = context as android.app.Activity
            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            } else {
                false
            }
            if (!shouldShowRationale) {
                permissionDeniedPermanently = true
            }
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        when (permissionState) {
            PackageManager.PERMISSION_GRANTED -> {
                cameraActive = true
                cameraViewModel.initialize(context, anonymousId)
                delay(500)
                cameraViewModel.startRecording()
            }
            else -> {
                if (!permissionRequested) {
                    val activity = context as android.app.Activity
                    val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    } else {
                        false
                    }
                    if (shouldShowRationale) {
                        showPermissionRationale = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }

    LaunchedEffect(cameraError) {
        cameraError?.let { error ->
            android.widget.Toast.makeText(context, "Camera error: $error", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording && recordingTime < 60) {
                delay(1000)
                recordingTime++
            }
            if (recordingTime >= 60) {
                cameraViewModel.stopRecording()
                android.widget.Toast.makeText(context, "Recording complete! Please finish the questionnaire.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val totalQuestions = questions.size
    val progress = ((currentQuestion + 1).toFloat() / totalQuestions) * 100
    val isLastQuestion = currentQuestion == totalQuestions - 1
    val isFirstQuestion = currentQuestion == 0
    val hasAnsweredCurrent = answers[currentQuestion] != null

    fun saveQuestionnaireCsv(context: Context, anonymousId: String, assessmentType: String, answers: Map<Int, Int>): String? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "unifiedapp")
            val userFolder = File(appFolder, anonymousId)
            if (!userFolder.exists()) userFolder.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${anonymousId}_${assessmentType}_PHQ_${timestamp}.csv"
            val csvFile = File(userFolder, fileName)

            val csvContent = StringBuilder()
            csvContent.append("question_index,question_text,answer_score\n")

            answers.forEach { (index, score) ->
                val questionText = questions.getOrNull(index) ?: "Unknown Question"
                csvContent.append("$index,\"$questionText\",$score\n")
            }

            csvFile.writeText(csvContent.toString())

            val prefs = context.getSharedPreferences("file_paths", Context.MODE_PRIVATE)
            prefs.edit().putString("phq9_csv_path", csvFile.absolutePath).apply()

            return csvFile.absolutePath
        } catch (e: Exception) {
            Log.e("AssessmentScreen", "Error saving questionnaire CSV: ${e.message}")
            return null
        }
    }

    fun handleFinish(bypassValidation: Boolean = false) {
        if (isCompleting) return

        if (!bypassValidation && !cameraViewModel.validateFacialData()) {
            showValidationDialog = true
            return
        }

        isCompleting = true
        cameraViewModel.stopRecording()

        // Get AI Prediction
        val prediction = cameraViewModel.getPrediction()

        // Save AU CSV
        cameraViewModel.saveCSVWithAUData(context)

        // Save Questionnaire CSV
        saveQuestionnaireCsv(context, anonymousId, assessmentType, answers)

        val totalScore = answers.values.sum()

        // Save to preferences
        val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("lastAssessmentScore", totalScore)
            putString("lastAssessmentDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
            putString("assessment_type", assessmentType)

            // Save AI prediction details
            prediction?.let {
                putFloat("ai_raw_score", it.score.toFloat())
                putString("ai_prediction_label", it.prediction)
                putFloat("ai_confidence", it.confidence)
            }
            apply()
        }

        // Delay slightly to allow the UI to finish animations and avoid flicker
        coroutineScope.launch {
            delay(500) // Increased from 100ms to give a smoother transition
            try {
                // Navigate without popUpTo to ensure the result screen stays in the stack
                if (isDepression) {
                    navController.navigate("mochan_result/$totalScore")
                } else {
                    navController.navigate("sahay_result/$totalScore")
                }
            } catch (e: Exception) {
                Log.e("AssessmentScreen", "Navigation error: ${e.message}")
                // Fallback navigation if specific route fails
                navController.navigate("launcher")
            }
        }
    }

    fun handlePrevious() {
        if (!isFirstQuestion) currentQuestion -= 1
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(backgroundColor),
        containerColor = backgroundColor,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDepression) Color(0xFFFFFDF9).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.9f),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        cameraViewModel.stopRecording()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (isDepression) "Mochan Assessment" else "Sahay Assessment",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDepression) Color(0xFF1D2335) else SahayCharcoal
                        )
                        Text(
                            if (isDepression) "PHQ-9 with AI Analysis" else "GAD-7 with AI Analysis",
                            fontSize = 14.sp,
                            color = if (isDepression) Color(0xFF4B5563) else SahayMutedSlate
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(14.dp))

                // Camera preview card
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F5F5))) {
                                    if (cameraActive && isInitialized) {
                                        AndroidView(
                                            factory = { ctx ->
                                                PreviewView(ctx).apply {
                                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                                    cameraProviderFuture.addListener({
                                                        val cameraProvider = cameraProviderFuture.get()
                                                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                                        val imageAnalyzer = ImageAnalysis.Builder()
                                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                                            .build()
                                                        imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                                            cameraViewModel.processFrame(imageProxy, true)
                                                        }
                                                        val videoCapture = cameraViewModel.getVideoCapture()
                                                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                                        try {
                                                            cameraProvider.unbindAll()
                                                            if (videoCapture != null) {
                                                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer, videoCapture as UseCase)
                                                            } else {
                                                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("CameraPreview", "Error binding camera: ${e.message}")
                                                        }
                                                    }, ContextCompat.getMainExecutor(ctx))
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.outline_album_24),
                                            contentDescription = "Camera",
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(24.dp).align(Alignment.Center)
                                        )
                                    }
                                    if (analyzing) {
                                        Box(modifier = Modifier.fillMaxSize().background(MochanPurplePrimary.copy(alpha = 0.2f))) {
                                            CircularProgressIndicator(color = MochanPurplePrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp).align(Alignment.Center))
                                        }
                                    }
                                    if (cameraActive && frameCount > 0) {
                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(8.dp).clip(CircleShape).background(Color.Red))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(when {
                                            cameraError != null -> Color.Red
                                            !faceDetected && frameCount > 0 -> Color.Yellow
                                            faceDetected -> Color.Green
                                            else -> Color.Gray
                                        }))
                                        Text(text = when {
                                            cameraError != null -> "Camera Error"
                                            !isInitialized -> "Initializing..."
                                            !faceDetected && frameCount > 0 -> "No Face Detected"
                                            faceDetected -> "Face Detected"
                                            else -> "Ready"
                                        }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (cameraActive && frameCount > 0) {
                                        Text(text = "Recording: ${recordingTime}s / 60s", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                    Text(text = "Frames: $frameCount / 50", fontSize = 12.sp, color = if (frameCount >= 50) Color.Green else Color(0xFF666666))
                                    if (au17Value > 0) {
                                        Text(text = "AU17: ${String.format(Locale.US, "%.2f", au17Value)}", fontSize = 10.sp, color = Color(0xFF999999))
                                    }
                                    if (frameCount < 50 && frameCount > 0) {
                                        Text(text = "⚠️ Need ${50 - frameCount} more frames", fontSize = 10.sp, color = Color(0xFFFF6F91))
                                    }
                                    cameraError?.let { Text(text = it, fontSize = 10.sp, color = Color.Red) }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Question card
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MochanSurfaceWhite),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Question ${currentQuestion + 1} of $totalQuestions", fontSize = 14.sp, color = Color.Gray)
                                    Text("${progress.toInt()}%", fontSize = 14.sp, color = primaryColor)
                                }
                                Spacer(Modifier.height(8.dp))
                                val animatedProgress by animateFloatAsState(targetValue = progress / 100f, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "Progress")
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = primaryColor,
                                    trackColor = Color(0xFFEDE7F6)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(questions[currentQuestion], fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.padding(vertical = 16.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        AnswerBox(ANSWER_OPTIONS[0], answers[currentQuestion]) { score ->
                                            answers = answers + (currentQuestion to score)
                                            analyzing = true
                                            coroutineScope.launch {
                                                delay(800)
                                                analyzing = false
                                                if (currentQuestion < questions.size - 1) {
                                                    currentQuestion += 1
                                                } else {
                                                    handleFinish()
                                                }
                                            }
                                        }
                                        AnswerBox(ANSWER_OPTIONS[1], answers[currentQuestion]) { score ->
                                            answers = answers + (currentQuestion to score)
                                            analyzing = true
                                            coroutineScope.launch {
                                                delay(800)
                                                analyzing = false
                                                if (currentQuestion < questions.size - 1) {
                                                    currentQuestion += 1
                                                } else {
                                                    handleFinish()
                                                }
                                            }
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        AnswerBox(ANSWER_OPTIONS[2], answers[currentQuestion]) { score ->
                                            answers = answers + (currentQuestion to score)
                                            analyzing = true
                                            coroutineScope.launch {
                                                delay(800)
                                                analyzing = false
                                                if (currentQuestion < questions.size - 1) {
                                                    currentQuestion += 1
                                                } else {
                                                    handleFinish()
                                                }
                                            }
                                        }
                                        AnswerBox(ANSWER_OPTIONS[3], answers[currentQuestion]) { score ->
                                            answers = answers + (currentQuestion to score)
                                            analyzing = true
                                            coroutineScope.launch {
                                                delay(800)
                                                analyzing = false
                                                if (currentQuestion < questions.size - 1) {
                                                    currentQuestion += 1
                                                } else {
                                                    handleFinish()
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { handlePrevious() },
                                        enabled = !isFirstQuestion,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEF2)),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("← Previous", color = primaryColor)
                                    }
                                    Text("${currentQuestion + 1} of $totalQuestions", color = Color.Gray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }

            if (showValidationDialog) {
                AlertDialog(
                    onDismissRequest = { showValidationDialog = false },
                    title = { Text("Insufficient Facial Data", color = Color(0xFFFF6F91), fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("We need at least 50 frames with clear facial expressions for accurate analysis.", color = Color(0xFF4B5563))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Current frames: $frameCount/50", fontWeight = FontWeight.Bold, color = if (frameCount < 50) Color(0xFFFF6F91) else Color.Green)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Options:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("• Continue recording (recommended)", fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                            Text("• Submit anyway (AI analysis may be inaccurate)", fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    },
                    confirmButton = { TextButton(onClick = { showValidationDialog = false }) { Text("Continue Recording") } },
                    dismissButton = { TextButton(onClick = { showValidationDialog = false; handleFinish(bypassValidation = true) }) { Text("Submit Anyway", color = Color.Red) } }
                )
            }

            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationale = false },
                    title = { Text("Camera Permission Needed", color = Color(0xFFFF6F91), fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(if (permissionDeniedPermanently) "Camera permission is permanently denied. Please enable it in app settings to use the AI facial analysis feature." else "This app needs camera access to analyze facial expressions during your mental health assessment. Your privacy is protected - no images are stored.")
                            if (permissionDeniedPermanently) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("To enable: Settings → Apps → UnifiedApp → Permissions → Camera", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showPermissionRationale = false
                            if (permissionDeniedPermanently) {
                                val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) { Text(if (permissionDeniedPermanently) "Open Settings" else "Request Again") }
                    },
                    dismissButton = { TextButton(onClick = { showPermissionRationale = false }) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
fun RowScope.AnswerBox(
    option: AnswerOption,
    selectedAnswer: Int?,
    onAnswerSelected: (Int) -> Unit
) {
    val isSelected = selectedAnswer == option.score
    Card(
        modifier = Modifier
            .weight(1f)
            .height(110.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF8A65) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onAnswerSelected(option.score) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFEEF2) else Color.White),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(option.emoji, fontSize = 32.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                option.label,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color(0xFF333333),
                minLines = 2
            )
        }
    }
}