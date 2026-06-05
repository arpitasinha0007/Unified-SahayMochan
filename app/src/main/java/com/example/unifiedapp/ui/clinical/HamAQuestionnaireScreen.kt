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

val hamAQuestions = listOf(
    "1. Anxious mood",
    "2. Tension",
    "3. Fears",
    "4. Insomnia",
    "5. Intellectual (cognitive)",
    "6. Depressed mood",
    "7. Somatic (muscular)",
    "8. Somatic (sensory)",
    "9. Cardiovascular symptoms",
    "10. Respiratory symptoms",
    "11. Gastrointestinal symptoms",
    "12. Genitourinary symptoms",
    "13. Autonomic symptoms",
    "14. Behavior at interview"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamAQuestionnaireScreen(
    navController: NavController,
    patientId: String,
    patientName: String,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = UserPreferences(context)

    var answers by remember { mutableStateOf(List(14) { 0 }) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    val submissionState by authViewModel.submissionState.collectAsState()

    LaunchedEffect(submissionState) {
        when (val state = submissionState) {
            is AuthViewModel.ClinicalSubmissionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                navController.navigate("clinical_result/${state.score}/${state.severity}/ham_a") {
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
                title = { Text("HAM-A Assessment", fontWeight = FontWeight.Bold) },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
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
                text = "Question ${currentQuestion + 1} of 14",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentQuestion + 1).toFloat() / 14f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF9D8DF1),
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
                        text = hamAQuestions[currentQuestion],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    for (score in 0..4) {
                        val isSelected = answers[currentQuestion] == score
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    if (isSelected) Color(0xFF9D8DF1).copy(alpha = 0.1f)
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
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF9D8DF1))
                            )
                            Text(
                                text = when (score) {
                                    0 -> "0 - None"
                                    1 -> "1 - Mild"
                                    2 -> "2 - Moderate"
                                    3 -> "3 - Severe"
                                    else -> "4 - Very Severe"
                                },
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
                        if (currentQuestion == 13) {
                            scope.launch {
                                // ✅ Get clinician ID from UserPreferences (UUID)
                                val clinicianId = userPreferences.getClinicianUserId()
                                if (clinicianId.isNullOrBlank()) {
                                    Toast.makeText(context, "Clinician ID not found. Please login again.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                Log.d("HAM-A", "Submitting: patientId=$patientId, clinicianId=$clinicianId, scores=$answers")
                                authViewModel.submitHamA(patientId, clinicianId, answers)
                            }
                        } else {
                            currentQuestion++
                        }
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D8DF1))
                ) {
                    Text(if (currentQuestion == 13) "Submit" else "Next")
                }
            }
        }
    }
}