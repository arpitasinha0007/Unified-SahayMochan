package com.example.unifiedapp.ui.clinical

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unifiedapp.navigation.Screen
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.PatientItem
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = UserPreferences(context)

    var searchQuery by remember { mutableStateOf("") }
    val patients by authViewModel.patients.collectAsState()
    var clinicianId by remember { mutableStateOf<String?>(null) }

    var allPatients by remember { mutableStateOf<List<PatientItem>>(emptyList()) }
    var filteredPatients by remember { mutableStateOf<List<PatientItem>>(emptyList()) }

    // Define the filtering function before it's used
    fun updateFilteredPatients() {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isEmpty()) {
            filteredPatients = allPatients
            return
        }
        val queryWords = trimmedQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        filteredPatients = allPatients.filter { patient ->
            val nameLower = patient.name.lowercase()
            val idLower = patient.registrationId.lowercase()
            queryWords.any { word ->
                nameLower.contains(word.lowercase()) || idLower.contains(word.lowercase())
            }
        }
    }

    LaunchedEffect(Unit) {
        clinicianId = userPreferences.getClinicianUserId()
        if (clinicianId != null) {
            authViewModel.fetchPatients(clinicianId!!, null)
        } else {
            Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(patients) {
        allPatients = patients
        updateFilteredPatients()
    }

    LaunchedEffect(searchQuery) {
        updateFilteredPatients()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinician Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAF8FF)
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            userPreferences.clearClinicianSession()
                        }
                        navController.navigate(Screen.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9D8DF1),
                    unfocusedBorderColor = Color(0xFFD9D1FF)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Patient List",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPatients.isEmpty() && allPatients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading patients...", color = Color.Gray)
                }
            } else if (filteredPatients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No patients match your search", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatients, key = { it.patientId }) { patient ->
                        PatientCard(
                            patient = patient,
                            onStartHamA = {
                                navController.navigate("ham_a_assessment/${patient.patientId}/${patient.name}/${patient.registrationId}")
                            },
                            onStartHdrs = {
                                navController.navigate("hdrs_assessment/${patient.patientId}/${patient.name}/${patient.registrationId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientCard(
    patient: PatientItem,
    onStartHamA: () -> Unit,
    onStartHdrs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = patient.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "ID: ${patient.registrationId}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "Age: ${patient.age}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (patient.latestHamAScore != null) {
                        Text(
                            text = "HAM-A: ${patient.latestHamAScore}",
                            fontSize = 12.sp,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                    if (patient.latestHdrsScore != null) {
                        Text(
                            text = "HAM-D: ${patient.latestHdrsScore}",
                            fontSize = 12.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartHamA,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9D8DF1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("HAM-A", color = Color.White)
                }
                Button(
                    onClick = onStartHdrs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("HAM-D", color = Color.White)
                }
            }
        }
    }
}