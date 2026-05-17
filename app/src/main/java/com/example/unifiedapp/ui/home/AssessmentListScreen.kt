package com.example.unifiedapp.ui.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AssessmentData
import com.example.unifiedapp.ui.views.AssessmentListState
import com.example.unifiedapp.ui.views.AssessmentViewModel
import com.example.unifiedapp.ui.views.Assessment_Data
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.DeleteState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentListScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val state by viewModel.assessmentListState.collectAsStateWithLifecycle()
    val assessments by viewModel.assessments.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var assessmentToDelete by remember { mutableStateOf<Assessment_Data?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── No produceState or LaunchedEffect needed — init{} handles loading ──

    LaunchedEffect(deleteState) {
        if (deleteState is DeleteState.Error) {
            snackbarHostState.showSnackbar((deleteState as DeleteState.Error).message)
            viewModel.resetDeleteState()
        }
    }
    // ── Safety net: if init{} already fired before screen opened ─────────────
    LaunchedEffect(Unit) {
        Log.d("AUTH_DEBUG", "AssessmentListScreen opened, state = $state")
        if (viewModel.assessmentListState.value is AssessmentListState.Idle) {
            Log.d("AUTH_DEBUG", "State is Idle, triggering reload")
            viewModel.reloadAssessments()
        }
    }
    assessmentToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { assessmentToDelete = null },
            title = { Text("Delete Assessment") },
            text = { Text("Delete assessment #${target.id}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAssessment(target.id)
                    assessmentToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { assessmentToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Assessments") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search by type or date...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            when (state) {
                is AssessmentListState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AssessmentListState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (state as AssessmentListState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.reloadAssessments() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    val filtered = assessments.filter {
                        it.anxietyPrediction?.toString()?.contains(search, ignoreCase = true) == true ||
                                it.createdAt.contains(search)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No assessments found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.id }) { assessment ->
                                AssessmentItem(
                                    assessment = assessment,
                                    onDelete = { assessmentToDelete = assessment }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssessmentItem(
    assessment: Assessment_Data,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // Assessment type badge + ID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = assessment.anxietyPrediction?.let { "Score: $it" } ?: "No Score",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                (assessment.anxietyPrediction ?: 0f) >= 70f -> Color(0xFFE57373) // red - high
                                (assessment.anxietyPrediction?: 0f) >= 40f -> Color(0xFFFFB74D) // orange - moderate
                                else -> Color(0xFF81C784)                                        // green - low
                            }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "#${assessment.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Scores row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    assessment.gad7Score?.let {
                        ScoreChip(label = "GAD-7", value = it)
                    }
                    assessment.questionnaireScore?.let {
                        ScoreChip(label = "Questionnaire", value = it)
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Date + video count
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = assessment.createdAt.take(10), // show YYYY-MM-DD only
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "📹 ${assessment.videoCount} video${if (assessment.videoCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ScoreChip(label: String, value: Float) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}