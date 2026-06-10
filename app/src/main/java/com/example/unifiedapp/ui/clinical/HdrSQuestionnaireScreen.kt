package com.example.unifiedapp.ui.clinical

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

// HDRS questions with full descriptions
data class HdrsQuestion(val text: String, val maxScore: Int)

val hdrsQuestions = listOf(
    HdrsQuestion(
        "1. DEPRESSED MOOD (sadness, hopeless, helpless, worthless)\n0 – Absent\n1 – These feeling states indicated only on questioning\n2 – These feeling states spontaneously reported verbally\n3 – Communicates feeling states non-verbally (facial expression, posture, voice, tendency to weep)\n4 – Patient reports virtually only these feeling states in spontaneous verbal and non-verbal communication",
        4
    ),
    HdrsQuestion(
        "2. FEELINGS OF GUILT\n0 – Absent\n1 – Self reproach, feels he/she has let people down\n2 – Ideas of guilt or rumination over past errors or sinful deeds\n3 – Present illness is a punishment. Delusions of guilt\n4 – Hears accusatory or denunciatory voices and/or experiences threatening visual hallucination",
        4
    ),
    HdrsQuestion(
        "3. SUICIDE\n0 – Absent\n1 – Feels life is not worth living\n2 – Wishes he/she were dead or any thoughts of possible death to self\n3 – Ideas or gestures of suicide\n4 – Attempts at suicide (any serious attempt rate 4)",
        4
    ),
    HdrsQuestion(
        "4. INSOMNIA: EARLY IN THE NIGHT\n0 – No difficulty falling asleep\n1 – Complains of occasional difficulty falling asleep (more than ½ hour)\n2 – Complains of nightly difficulty falling asleep",
        2
    ),
    HdrsQuestion(
        "5. INSOMNIA: MIDDLE OF THE NIGHT\n0 – No difficulty\n1 – Patient complains of being restless and disturbed during the night\n2 – Waking during the night – any getting out of bed rates 2 (except for voiding)",
        2
    ),
    HdrsQuestion(
        "6. INSOMNIA: EARLY HOURS OF THE MORNING\n0 – No difficulty\n1 – Waking in early hours of the morning but goes back to sleep\n2 – Unable to fall asleep again if he/she gets out of bed",
        2
    ),
    HdrsQuestion(
        "7. WORK AND ACTIVITIES\n0 – No difficulty\n1 – Thoughts and feelings of incapacity, fatigue or weakness related to activities, work or hobbies\n2 – Loss of interest in activity, hobbies or work (feels he/she has to push self to work or activities)\n3 – Decrease in actual time spent in activities or decrease in productivity (<3 hours/day excluding routine chores)\n4 – Stopped working because of present illness, or engages in no activities except routine chores",
        4
    ),
    HdrsQuestion(
        "8. RETARDATION (slowness of thought and speech, impaired concentration, decreased motor activity)\n0 – Normal speech and thought\n1 – Slight retardation during the interview\n2 – Obvious retardation during the interview\n3 – Interview difficult\n4 – Complete stupor",
        4
    ),
    HdrsQuestion(
        "9. AGITATION\n0 – None\n1 – Fidgetiness\n2 – Playing with hands, hair, etc.\n3 – Moving about, can’t sit still\n4 – Hand wringing, nail biting, hair-pulling, biting of lips",
        4
    ),
    HdrsQuestion(
        "10. ANXIETY PSYCHIC\n0 – No difficulty\n1 – Subjective tension and irritability\n2 – Worrying about minor matters\n3 – Apprehensive attitude apparent in face or speech\n4 – Fears expressed without questioning",
        4
    ),
    HdrsQuestion(
        "11. ANXIETY SOMATIC (physiological concomitants: dry mouth, wind, indigestion, diarrhea, cramps, belching, palpitations, headaches, hyperventilation, sighing, urinary frequency, sweating)\n0 – Absent\n1 – Mild\n2 – Moderate\n3 – Severe\n4 – Incapacitating",
        4
    ),
    HdrsQuestion(
        "12. SOMATIC SYMPTOMS GASTRO-INTESTINAL\n0 – None\n1 – Loss of appetite but eating without staff encouragement. Heavy feelings in abdomen.\n2 – Difficulty eating without staff urging. Requests or requires laxatives or medication for bowel or gastro-intestinal symptoms",
        2
    ),
    HdrsQuestion(
        "13. GENERAL SOMATIC SYMPTOMS\n0 – None\n1 – Heaviness in limbs, back or head. Backaches, headaches, muscle aches. Loss of energy and fatigability.\n2 – Any clear‑cut symptom rates 2",
        2
    ),
    HdrsQuestion(
        "14. GENITAL SYMPTOMS (loss of libido, menstrual disturbances)\n0 – Absent\n1 – Mild\n2 – Severe",
        2
    ),
    HdrsQuestion(
        "15. HYPOCHONDRIASIS\n0 – Not present\n1 – Self-absorption (bodily)\n2 – Preoccupation with health\n3 – Frequent complaints, requests for help, etc.\n4 – Hypochondriacal delusions",
        4
    ),
    HdrsQuestion(
        "16. LOSS OF WEIGHT (rate either a or b)\na) According to patient:\n   0 – No weight loss\n   1 – Probable weight loss associated with present illness\n   2 – Definite weight loss\nb) According to weekly measurements:\n   0 – Less than 1 lb weight loss in week\n   1 – Greater than 1 lb weight loss in week\n   2 – Greater than 2 lb weight loss in week",
        2
    ),
    HdrsQuestion(
        "17. INSIGHT\n0 – Acknowledges being depressed and ill\n1 – Acknowledges illness but attributes cause to bad food, climate, overwork, virus, need for rest, etc.\n2 – Denies being ill at all",
        2
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HdrSQuestionnaireScreen(
    navController: NavController,
    patientId: String,
    patientName: String,
    registrationId: String,   // ✅ new parameter
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = UserPreferences(context)

    var answers by remember { mutableStateOf(List(17) { 0 }) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    val submissionState by authViewModel.submissionState.collectAsState()

    // Log for debugging
    LaunchedEffect(currentQuestion) {
        Log.d("HDRS_DEBUG", "Current question: $currentQuestion, total: ${hdrsQuestions.size}")
    }

    LaunchedEffect(submissionState) {
        when (val state = submissionState) {
            is AuthViewModel.ClinicalSubmissionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                // ✅ Include registrationId at the end of the route
                navController.navigate("clinical_result/${state.score}/${state.severity}/hdrs/${state.assessmentId}/${registrationId}") {
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
                title = { Text("HAM-D Assessment", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
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
                text = "Question ${currentQuestion + 1} of ${hdrsQuestions.size}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentQuestion + 1).toFloat() / hdrsQuestions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Validate index
            if (currentQuestion in hdrsQuestions.indices) {
                val question = hdrsQuestions[currentQuestion]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = question.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column {
                                for (score in 0..question.maxScore) {
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
                    }
                }
            } else {
                Text("Invalid question index", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        if (currentQuestion == hdrsQuestions.size - 1) {
                            // Submit
                            scope.launch {
                                val clinicianId = userPreferences.getClinicianUserId()
                                if (clinicianId.isNullOrBlank()) {
                                    Toast.makeText(context, "Clinician ID not found. Please login again.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                Log.d("HDRS", "Submitting: patientId=$patientId, clinicianId=$clinicianId, scores=$answers")
                                authViewModel.submitHdrs(patientId, clinicianId, answers)
                            }
                        } else {
                            // Next question – safely increment
                            if (currentQuestion + 1 < hdrsQuestions.size) {
                                currentQuestion++
                            }
                        }
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (currentQuestion == hdrsQuestions.size - 1) "Submit" else "Next")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}