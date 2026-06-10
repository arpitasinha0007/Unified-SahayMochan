package com.example.unifiedapp.ui.clinical

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unifiedapp.remote.ApiClient
import com.example.unifiedapp.ui.views.AuthViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalResultScreen(
    navController: NavController,
    score: Int,
    @Suppress("UNUSED_PARAMETER") severity: String,
    type: String,
    assessmentId: String,
    registrationId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val maxScore = if (type == "ham_a") 56 else 52
    val color = if (type == "ham_a") Color(0xFF9D8DF1) else Color(0xFF10B981)

    // State for self‑assessment scores
    var gad7Score by remember { mutableStateOf<Int?>(null) }
    var phq9Score by remember { mutableStateOf<Int?>(null) }
    var isLoadingSelfScores by remember { mutableStateOf(false) }

    // Fetch latest GAD-7 and PHQ-9 scores for this patient
    LaunchedEffect(registrationId) {
        if (registrationId.isNotBlank()) {
            isLoadingSelfScores = true
            val (gad, phq) = authViewModel.getLatestSelfAssessmentScores(registrationId)
            gad7Score = gad
            phq9Score = phq
            isLoadingSelfScores = false
        }
    }

    val severityOptions = listOf("Minimal", "Mild", "Moderate", "Moderately Severe", "Severe")
    var selectedSeverity by remember { mutableStateOf("Moderate") }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun saveSeverity() {
        scope.launch {
            isSaving = true
            try {
                val apiType = when (type) {
                    "ham_a" -> "ham-a"
                    "hdrs" -> "ham-d"
                    else -> type
                }
                Log.d("ClinicalResult", "API type: $apiType, assessmentId: $assessmentId, severity: $selectedSeverity")
                val response = ApiClient.authApi.updateSeverityDirect(apiType, assessmentId, selectedSeverity)
                if (response.isSuccessful) {
                    saveSuccess = true
                    Toast.makeText(context, "Severity saved: $selectedSeverity", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(context, "Failed to save: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ClinicalResult", "Exception: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFAF8FF), Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val title = if (type == "ham_a") "HAM-A Assessment Result" else "HAM-D Assessment Result"
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Score",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$score / $maxScore",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Doctor's Severity Rating",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = color
                            )
                        ) {
                            Text(selectedSeverity, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            severityOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedSeverity = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { saveSeverity() },
                        enabled = !isSaving && !saveSuccess,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            disabledContainerColor = color.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (saveSuccess) "Saved" else "Save Severity", color = Color.White)
                        }
                    }
                }
            }

            // Patient's latest self‑assessment scores – always show, with dash for 0 or null
            if (!isLoadingSelfScores) {
                val gad = gad7Score
                val phq = phq9Score
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Patient's Latest Self‑Assessment Scores",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // GAD‑7
                            Column {
                                Text("GAD‑7", fontSize = 12.sp, color = Color(0xFF6B7280))
                                Text(
                                    text = if (gad != null && gad > 0) "$gad / 21" else "--",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gad != null && gad > 0) Color(0xFFF59E0B) else Color(0xFF9CA3AF)
                                )
                            }
                            // PHQ‑9
                            Column {
                                Text("PHQ‑9", fontSize = 12.sp, color = Color(0xFF6B7280))
                                Text(
                                    text = if (phq != null && phq > 0) "$phq / 27" else "--",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (phq != null && phq > 0) Color(0xFFEF4444) else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate("clinician_dashboard") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text("Back to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}