package com.example.unifiedapp.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.unifiedapp.ui.views.AssessmentViewModel
import com.example.unifiedapp.ui.views.AssessmentData
import com.example.unifiedapp.ui.vision.FaceLandmarkerHelper
import com.example.unifiedapp.ui.vision.VideoRecorderHelper
import com.example.unifiedapp.ui.vision.CameraPreview
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.PrintWriter
import java.io.StringWriter
import android.content.Context
import java.io.File
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

fun setupCrashHandler(context: Context) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()
            Log.e("CRASH", "=== CRASH DETECTED ===")
            Log.e("CRASH", stackTrace)
            val crashFile = File(context.filesDir, "crash_${System.currentTimeMillis()}.log")
            crashFile.writeText(stackTrace)
            defaultHandler?.uncaughtException(thread, throwable)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

@Composable
fun ProctoredAssessmentScreen(
    onAssessmentComplete: (Int, Int, AssessmentData?) -> Unit,
    onExit: () -> Unit,
    viewModel: AssessmentViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var isCameraReady by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }
    val isCleaningUp = remember { AtomicBoolean(false) }
    val shutdownMutex = remember { Mutex() }
    var isFrameProcessingActive by remember { mutableStateOf(true) }
    val assessmentData by viewModel.assessmentResult.collectAsState()

    // Permission state - Mutable so it can update
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var allPermissionsGranted = hasCameraPermission && hasAudioPermission
    var isRequestingPermission by remember { mutableStateOf(false) }

    // ✅ Permission launcher - MUST be created before being used
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isRequestingPermission = false
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions[Manifest.permission.RECORD_AUDIO] ?: false
        } else true

        if (hasCameraPermission && hasAudioPermission) {
            // Permission granted, restart the composable to initialize camera
            android.widget.Toast.makeText(context, "Camera permission granted!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required for this assessment", android.widget.Toast.LENGTH_LONG).show()
            onExit()
        }
    }

    // ✅ Request permissions when needed - using SideEffect to ensure it runs after composition
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted && !isRequestingPermission) {
            isRequestingPermission = true
            val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val videoHelper = remember(context, viewModel) {
        VideoRecorderHelper(context, viewModel).also {
            Log.d("Screen", "Created VideoRecorderHelper: $it")
        }
    }

    val faceHelper = remember(context) {
        FaceLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            faceLandmarkerHelperListener = viewModel
        )
    }

    LaunchedEffect(Unit) {
        setupCrashHandler(context)
    }

    // Setup camera - only if permissions are granted
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted && !isCameraReady) {
            viewModel.attachVideoHelper(videoHelper)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider
                    val videoCapture = videoHelper.buildVideoCapture()
                    videoHelper.bind(cameraProvider, lifecycleOwner)
                    isCameraReady = true
                    Log.d("Screen", "✅ Camera ready!")
                } catch (e: Exception) {
                    Log.e("Screen", "Camera setup failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Start assessment when ready
    LaunchedEffect(allPermissionsGranted, isCameraReady) {
        if (allPermissionsGranted && isCameraReady && !quizCompleted) {
            Log.d("Screen", "Starting assessment...")
            viewModel.setFaceHelper(faceHelper)
            viewModel.startAssessment(context)
            delay(500)
            viewModel.startSession("Student_22")
            Log.d("Screen", "Assessment and recording started")
        }
    }

    LaunchedEffect(quizCompleted) {
        if (quizCompleted) {
            Log.d("Screen", "⚠️ Quiz completed - stopping frame processing")
            isFrameProcessingActive = false
            viewModel.stopFrameProcessing()
            delay(100)
        }
    }

    LaunchedEffect(assessmentData, quizCompleted) {
        if (assessmentData != null && quizCompleted) {
            Log.d("Screen", "Assessment data ready, navigating to results")
            val data = assessmentData!!
            val finalScore = data.questionnaireScore
            val gad7Score = data.gad7Score
            delay(500)
            onAssessmentComplete(finalScore, gad7Score, data)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                if (!isCleaningUp.getAndSet(true)) {
                    Log.d("Screen", "========== SCREEN DISPOSING ==========")
                    isFrameProcessingActive = false
                    viewModel.stopFrameProcessing()
                    shutdownMutex.withLock {
                        delay(200)
                        try { faceHelper.shutdown() } catch (e: Exception) { Log.e("Screen", "Error shutting down face helper: ${e.message}") }
                        try { viewModel.emergencyStop() } catch (e: Exception) { Log.e("Screen", "Error in emergency stop: ${e.message}") }
                        try { videoHelper.stopRecording() } catch (e: Exception) { Log.e("Screen", "Error stopping recording", e) }
                        try { cameraProviderRef?.let { provider -> provider.unbindAll() } } catch (e: Exception) { Log.e("Screen", "Error unbinding camera: ${e.message}") }
                        if (!quizCompleted) {
                            try {
                                viewModel.stopSession()
                                viewModel.stopAssessment()
                            } catch (e: Exception) { Log.e("Screen", "Error stopping assessment", e) }
                        }
                    }
                    Log.d("Screen", "========== CLEANUP COMPLETE ==========")
                }
            }
        }
    }

    val isSuspicious = state.anxietyScore >= 15 || state.anxietyPrediction.startsWith("High")

    Box(Modifier.fillMaxSize()) {

        // ✅ Show loading while waiting for permission
        if (!allPermissionsGranted || isRequestingPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Requesting camera permission...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onExit) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
            return@Box
        }

        // Wait for camera to be ready
        if (!isCameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initializing camera...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            return@Box
        }

        // Loading overlay while processing assessment data
        if (quizCompleted && assessmentData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Processing assessment data...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "This may take a few moments", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // MAIN QUIZ
        if (!quizCompleted && allPermissionsGranted && isCameraReady) {
            MoodCheckInFlow(
                onFlowComplete = { examScore, responses ->
                    Log.d("Screen", "========== QUIZ COMPLETED ==========")
                    Log.d("Screen", "Score: $examScore")
                    isFrameProcessingActive = false
                    viewModel.stopFrameProcessing()
                    quizCompleted = true
                    val convertedResponses = responses.mapValues { (_, response) ->
                        com.example.unifiedapp.ui.views.QuestionnaireResponse(
                            selectedOption = response.selectedOption,
                            score = response.score,
                            timestamp = response.timestamp
                        )
                    }
                    scope.launch {
                        delay(200)
                        viewModel.completeSession(examScore, convertedResponses)
                    }
                },
                onExit = {
                    Log.d("Screen", "User exited quiz")
                    viewModel.stopAssessment()
                    onExit()
                }
            )
        }

        val borderColor by animateColorAsState(
            targetValue = if (isSuspicious) Color(0xFFFF4C4C) else Color.White,
            animationSpec = tween(durationMillis = 600),
            label = "borderAnimation"
        )

        val borderWidth by animateDpAsState(
            targetValue = if (isSuspicious) 4.dp else 2.dp,
            label = "widthAnimation"
        )

        // CAMERA PIP
        if (allPermissionsGranted && isCameraReady && !quizCompleted && isFrameProcessingActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(borderWidth, borderColor),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 0.dp, top = 0.dp, end = 16.dp, bottom = 95.dp)
                    .width(80.dp)
                    .aspectRatio(3f / 4f)
                    .shadow(elevation = if (isSuspicious) 15.dp else 8.dp, shape = RoundedCornerShape(16.dp), spotColor = if (isSuspicious) Color.Red else Color.Black)
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { imageProxy ->
                        if (isFrameProcessingActive && !quizCompleted && !isCleaningUp.get()) {
                            try {
                                viewModel.processImageFrame(imageProxy, isFrontCamera = true)
                            } catch (e: Exception) {
                                Log.e("Screen", "Frame processing error: ${e.message}")
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    },
                    recorderHelper = videoHelper
                )
            }
        }

        // ALERT
        AnimatedVisibility(
            visible = isSuspicious && !quizCompleted && isFrameProcessingActive,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(20.dp)) {
                Text(text = "⚠️ High Anxiety Detected", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}