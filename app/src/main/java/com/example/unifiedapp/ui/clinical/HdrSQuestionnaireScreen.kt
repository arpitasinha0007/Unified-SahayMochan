package com.example.unifiedapp.ui.clinical

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.launch

// HDRS questions (17 items) with their max scores
data class HdrsQuestion(val text: String, val maxScore: Int)

val hdrsQuestions = listOf(
    HdrsQuestion("1. Depressed mood", 4),
    HdrsQuestion("2. Feelings of guilt", 4),
    HdrsQuestion("3. Suicide", 4),
    HdrsQuestion("4. Insomnia early", 2),
    HdrsQuestion("5. Insomnia middle", 2),
    HdrsQuestion("6. Insomnia late", 2),
    HdrsQuestion("7. Work and activities", 4),
    HdrsQuestion("8. Retardation", 4),
    HdrsQuestion("9. Agitation", 4),
    HdrsQuestion("10. Anxiety psychic", 4),
    HdrsQuestion("11. Anxiety somatic", 4),
    HdrsQuestion("12. Gastrointestinal symptoms", 2),
    HdrsQuestion("13. General somatic symptoms", 2),
    HdrsQuestion("14. Genital symptoms", 2),
    HdrsQuestion("15. Hypochondriasis", 4),
    HdrsQuestion("16. Weight loss", 2),
    HdrsQuestion("17. Insight", 2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HdrSQuestionnaireScreen(
    navController: NavController,
    patientId: String,
    patientName: String,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = UserPreferences(context)

    var answers by remember { mutableStateOf(List(17) { 0 }) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    val submissionState by authViewModel.submissionState.collectAsState()

    LaunchedEffect(submissionState) {
        when (val state = submissionState) {
            is AuthViewModel.ClinicalSubmissionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                navController.navigate("clinical_result/${state.score}/${state.severity}/hdrs") {
                    popUpTo("clinician_dashboard") { inclusive = false }
                }
                authViewModel.resetSubmissionState()
            }
            is AuthViewModel.ClinicalSubmissionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetSubmissionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HDRS Assessment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8FF))
            )
        },
        containerColor = Color(0xFFFAF8FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = "Patient: $patientName",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Question ${currentQuestion + 1} of 17",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentQuestion + 1).toFloat() / 17f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = hdrsQuestions[currentQuestion].text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val maxScore = hdrsQuestions[currentQuestion].maxScore
                    for (score in 0..maxScore) {
                        val isSelected = answers[currentQuestion] == score
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    if (isSelected) Color(0xFF10B981).copy(alpha = 0.1f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    answers = answers.toMutableList().apply {
                                        this[currentQuestion] = score
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    answers = answers.toMutableList().apply {
                                        this[currentQuestion] = score
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                            )
                            Text(
                                text = "$score",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (currentQuestion > 0) currentQuestion-- },
                    enabled = currentQuestion > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = {
                        if (currentQuestion == 16) {
                            scope.launch {
                                // ✅ Get clinician ID from UserPreferences (UUID)
                                val clinicianId = userPreferences.getClinicianUserId()
                                if (clinicianId.isNullOrBlank()) {
                                    Toast.makeText(context, "Clinician ID not found. Please login again.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                Log.d("HDRS", "Submitting: patientId=$patientId, clinicianId=$clinicianId, scores=$answers")
                                authViewModel.submitHdrs(patientId, clinicianId, answers)
                            }
                        } else {
                            currentQuestion++
                        }
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (currentQuestion == 16) "Submit" else "Next")
                }
            }
        }
    }
}