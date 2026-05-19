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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.unifiedapp.utils.UploadHelper
import kotlinx.coroutines.launch

// ✅ IMPORT the existing UploadStatus from ResultScreen
// If UploadStatus is not accessible, we'll define it here with a different name
// or just use local state

// ============ MAIN UNDERAGE RESULT SCREEN ============
@Composable
fun UnderageResultScreen(
    navController: NavController,
    score: Int,
    aiPrediction: String = "",
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get user data
    val session = UserSessionHelper.getUserData(context)
    val userName = session.name
    val anonymousId = session.anonymousId
    val userAge = session.age
    val registrationId = session.registrationId

    // State for upload (using simple states instead of UploadStatus class)
    var isUploading by remember { mutableStateOf(false) }
    var uploadSuccess by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadStarted by remember { mutableStateOf(false) }

    // Get saved scores from preferences
    val prefs = context.getSharedPreferences("assessment_prefs", Context.MODE_PRIVATE)
    val phq9Score = prefs.getInt("lastAssessmentScore", score)
    val aiRawScore = prefs.getFloat("ai_raw_score", 0f)
    val savedEmail = session.email

    Log.d("UnderageResult", "User: $userName, Age: $userAge, Score: $phq9Score")
    Log.d("UnderageResult", "Registration ID: $registrationId, Anonymous ID: $anonymousId")

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
    }

    Scaffold(
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
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF6B9071),
                        modifier = Modifier.size(56.dp).scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (uploadSuccess) "Assessment Submitted!" else "Assessment Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E4E42),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uploadSuccess) {
                    Text(
                        text = "Your assessment has been successfully uploaded to the server.",
                        fontSize = 16.sp,
                        color = Color(0xFF5D6D66),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else if (uploadError != null) {
                    Text(
                        text = "Upload failed: ${uploadError}",
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        text = "Your assessment is complete. Click below to upload your data to the server.",
                        fontSize = 16.sp,
                        color = Color(0xFF5D6D66),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Upload Status Card
                if (uploadStarted && isUploading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Uploading...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    "Please wait while your data is uploaded",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else if (uploadSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp)
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
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Upload Successful!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    "Your data has been saved to the server",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else if (uploadError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Upload Failed",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        uploadError!!.take(50),
                                        fontSize = 12.sp,
                                        color = Color(0xFFC62828).copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    // Reset and retry
                                    uploadError = null
                                    uploadSuccess = false
                                    uploadStarted = false
                                    isUploading = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Text("Retry Upload", color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Upload Button (shown only if upload not successful)
                if (!uploadSuccess) {
                    Button(
                        onClick = {
                            if (!isUploading && anonymousId.isNotBlank()) {
                                uploadStarted = true
                                isUploading = true
                                uploadError = null

                                scope.launch {
                                    UploadHelper.uploadAssessment(
                                        context = context,
                                        coroutineScope = scope,
                                        anonymousId = anonymousId,
                                        age = userAge,
                                        aiRawScore = aiRawScore,
                                        email = savedEmail,
                                        registrationId = registrationId,
                                        onProgress = { progress, message ->
                                            Log.d("UnderageResult", "Upload progress: $progress% - $message")
                                        },
                                        onSuccess = { message ->
                                            isUploading = false
                                            uploadSuccess = true
                                            Toast.makeText(context, "Assessment uploaded successfully!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { error ->
                                            isUploading = false
                                            uploadError = error
                                            Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Please login to upload data", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B9071),
                            disabledContainerColor = Color(0xFFD3E4D6)
                        )
                    ) {
                        if (isUploading) {
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
                                Text("Uploading...", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload to Server", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Return Home button
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