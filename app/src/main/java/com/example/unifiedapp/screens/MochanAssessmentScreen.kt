package com.example.unifiedapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.unifiedapp.ui.theme.*
import com.example.unifiedapp.ui.views.CameraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Mochan PHQ-9 Questions (9 questions for Depression)
val MOCHAN_PHQ9_QUESTIONS = listOf(
    "Little interest or pleasure in doing things",
    "Feeling down, depressed, or hopeless",
    "Trouble falling or staying asleep, or sleeping too much",
    "Feeling tired or having little energy",
    "Poor appetite or overeating",
    "Feeling bad about yourself or that you are a failure",
    "Trouble concentrating on things",
    "Moving or speaking slowly or being fidgety",
    "Thoughts that you would be better off dead"
)

val MOCHAN_PHQ9_OPTIONS = listOf(
    "Not at all" to 0,
    "Several days" to 1,
    "More than half the days" to 2,
    "Nearly every day" to 3
)

@Composable
fun MochanAssessmentScreen(
    navController: NavController,
    cameraViewModel: CameraViewModel
) {
    var answers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val frameCount by cameraViewModel.frameCount.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraViewModel.initialize(context, "mochan_user")
            coroutineScope.launch {
                delay(500)
                cameraViewModel.startRecording()
            }
        } else {
            Toast.makeText(context, "Camera permission is required for assessment", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            cameraViewModel.initialize(context, "mochan_user")
            delay(500)
            cameraViewModel.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val totalQuestions = MOCHAN_PHQ9_QUESTIONS.size
    val progress = ((currentQuestion + 1).toFloat() / totalQuestions) * 100
    val isLastQuestion = currentQuestion == totalQuestions - 1
    val isFirstQuestion = currentQuestion == 0
    val hasAnsweredCurrent = answers[currentQuestion] != null

    fun handleFinish() {
        cameraViewModel.stopRecording()
        val totalScore = answers.values.sum()
        navController.navigate("mochan_result/$totalScore") {
            popUpTo("mochan_assessment") { inclusive = true }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MochanPurpleUltraLight,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        cameraViewModel.stopRecording()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MochanPurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MochanPurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Mochan Assessment", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MochanPurplePrimary)
                        Text("PHQ-9 with AI Analysis", fontSize = 14.sp, color = MochanTextSecondary)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MochanGradient)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (frameCount >= 50) Color.Green else Color.Yellow)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (frameCount >= 50) "✓ Face detected - Good!" else "Recording: $frameCount/50 frames needed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (frameCount >= 50) Color.Green else MochanPurplePrimary
                        )
                    }
                }
            }

            // Question Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Question ${currentQuestion + 1} of $totalQuestions", fontSize = 14.sp, color = MochanTextSecondary)
                            Text("${progress.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MochanPurplePrimary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MochanPurplePrimary,
                            trackColor = MochanPurpleUltraLight
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            MOCHAN_PHQ9_QUESTIONS[currentQuestion],
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MochanPurplePrimary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Options
                        MOCHAN_PHQ9_OPTIONS.forEach { (label, score) ->
                            val isSelected = answers[currentQuestion] == score
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        if (hasAnsweredCurrent) {
                                            answers = answers + (currentQuestion to score)
                                        } else {
                                            answers = answers + (currentQuestion to score)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MochanPurplePrimary.copy(alpha = 0.1f) else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MochanPurplePrimary else MochanTextSecondary.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    color = if (isSelected) MochanPurplePrimary else MochanTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Navigation Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { if (!isFirstQuestion) currentQuestion-- },
                                enabled = !isFirstQuestion,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MochanPurpleUltraLight,
                                    contentColor = MochanPurplePrimary
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Previous")
                            }

                            Button(
                                onClick = {
                                    if (isLastQuestion) {
                                        if (hasAnsweredCurrent) {
                                            handleFinish()
                                        }
                                    } else {
                                        if (hasAnsweredCurrent) {
                                            currentQuestion++
                                        }
                                    }
                                },
                                enabled = hasAnsweredCurrent,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MochanPurplePrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(if (isLastQuestion) "Submit" else "Next →")
                            }
                        }
                    }
                }
            }
        }
    }
}