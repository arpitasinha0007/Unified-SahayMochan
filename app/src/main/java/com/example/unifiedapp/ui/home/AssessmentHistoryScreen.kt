package com.example.unifiedapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AssessmentListState
import com.example.unifiedapp.ui.views.Assessment_Data
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.DeleteState
import com.example.unifiedapp.ui.views.UserPreferences
import com.example.unifiedapp.ui.views.UserData
import java.text.SimpleDateFormat
import java.util.*

// Theme colors matching your app's sage green palette
val HistorySageLight = Color(0xFFF1F7F3)
val HistorySageMedium = Color(0xFFD3E4D6)
val HistorySageAccent = Color(0xFF6B9071)
val HistoryCharcoal = Color(0xFF3E4E42)
val HistoryWhiteSoft = Color(0xFFFAFAFA)
val HistoryMutedSlate = Color(0xFF5D6D66)

// Severity colors
val SeverityMinimal = Color(0xFF7FAF7A)  // Soft green
val SeverityMild = Color(0xFFF0B27A)     // Soft orange
val SeverityModerate = Color(0xFFE8894A) // Warm orange
val SeveritySevere = Color(0xFFE57373)   // Soft red

// Gradient background matching your ProfileScreen
val HistorySageGradient = Brush.verticalGradient(
    colors = listOf(HistorySageLight, HistorySageMedium)
)

// Function to get anxiety severity level from GAD-7 score
fun getAnxietySeverity(gad7Score: Float?): String {
    return when (gad7Score?.toInt()) {  // Convert Float to Int
        null -> "Unknown"
        in 0..4 -> "Minimal Anxiety"
        in 5..9 -> "Mild Anxiety"
        in 10..14 -> "Moderate Anxiety"
        else -> "Severe Anxiety"
    }
}

fun getSeverityColor(gad7Score: Float?): Color {
    return when (gad7Score?.toInt()) {  // Convert Float to Int
        null -> HistoryMutedSlate
        in 0..4 -> SeverityMinimal
        in 5..9 -> SeverityMild
        in 10..14 -> SeverityModerate
        else -> SeveritySevere
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentHistoryScreen(
    viewModel: AuthViewModel,
    navController: NavController,
    userPreferences: UserPreferences
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State from ViewModel
    val state by viewModel.assessmentListState.collectAsStateWithLifecycle(
        lifecycleOwner.lifecycle
    )
    val assessments by viewModel.assessments.collectAsStateWithLifecycle(
        lifecycle = lifecycleOwner.lifecycle,
        initialValue = emptyList()
    )
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle(
        lifecycleOwner.lifecycle
    )
    val userData by userPreferences.userData.collectAsStateWithLifecycle(
        lifecycle = lifecycleOwner.lifecycle,
        initialValue = UserData(
            isLoggedIn = false,
            name = "",
            email = "",
            gender = "",
            id = "",
            age = 0,
            TOKEN = ""
        )
    )

    // Local state
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var assessmentToDelete by remember { mutableStateOf<Assessment_Data?>(null) }

    // Filter assessments
    val filteredAssessments = remember(assessments, searchQuery) {
        if (searchQuery.isBlank()) {
            assessments
        } else {
            assessments.filter { assessment ->
                assessment.createdAt.contains(searchQuery, ignoreCase = true) ||
                        assessment.id.toString().contains(searchQuery)
            }
        }
    }

    // Load assessments
    LaunchedEffect(Unit) {
        viewModel.reloadAssessments()
    }

    // Handle delete state
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                snackbarHostState.showSnackbar("Assessment deleted")
                viewModel.resetDeleteState()
            }
            is DeleteState.Error -> {
                snackbarHostState.showSnackbar((deleteState as DeleteState.Error).message)
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HistorySageGradient)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Assessment History",
                                style = MaterialTheme.typography.titleLarge,
                                color = HistoryCharcoal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Your wellness journey",
                                style = MaterialTheme.typography.bodySmall,
                                color = HistoryMutedSlate
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HistoryWhiteSoft)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HistoryCharcoal
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HistoryWhiteSoft.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HistoryWhiteSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = HistoryMutedSlate,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search by date...",
                                            color = HistoryMutedSlate.copy(alpha = 0.5f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = HistoryCharcoal,
                                fontSize = 14.sp
                            )
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = HistoryMutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredAssessments.size} ${if (filteredAssessments.size == 1) "assessment" else "assessments"} found",
                        fontSize = 14.sp,
                        color = HistoryMutedSlate,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assessment list
                when (state) {
                    is AssessmentListState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HistorySageAccent
                            )
                        }
                    }
                    is AssessmentListState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = HistoryWhiteSoft)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = HistorySageAccent,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        (state as AssessmentListState.Error).message,
                                        color = HistoryCharcoal,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.reloadAssessments() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HistorySageAccent
                                        )
                                    ) {
                                        Text("Try Again", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (filteredAssessments.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = HistoryWhiteSoft)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = HistoryMutedSlate,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            if (searchQuery.isNotBlank()) "No matches found" else "No assessments yet",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HistoryCharcoal
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            if (searchQuery.isNotBlank()) "Try a different search term" else "Complete your first check-in to see results",
                                            fontSize = 14.sp,
                                            color = HistoryMutedSlate,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredAssessments, key = { it.id }) { assessment ->
                                    AssessmentHistoryCard(
                                        assessment = assessment,
                                        onDelete = {
                                            assessmentToDelete = assessment
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete dialog with theme
    if (showDeleteDialog && assessmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = HistoryWhiteSoft,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = SeveritySevere,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Assessment",
                        color = HistoryCharcoal,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    "This action cannot be undone. Are you sure you want to delete this assessment?",
                    color = HistoryMutedSlate
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAssessment(assessmentToDelete!!.id)
                        showDeleteDialog = false
                        assessmentToDelete = null
                    }
                ) {
                    Text("Delete", color = SeveritySevere, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel", color = HistoryMutedSlate)
                }
            }
        )
    }
}

@Composable
fun AssessmentHistoryCard(
    assessment: Assessment_Data,
    onDelete: () -> Unit
) {
    val severity = getAnxietySeverity(assessment.gad7Score)
    val severityColor = getSeverityColor(assessment.gad7Score)

    // Format date nicely
    val formattedDate = remember(assessment.createdAt) {
        try {
            val parts = assessment.createdAt.split("T").firstOrNull() ?: assessment.createdAt
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(parts)
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            assessment.createdAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HistoryWhiteSoft),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with date and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small colored indicator for severity
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(severityColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        color = HistoryMutedSlate
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SeveritySevere.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = SeveritySevere,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main content - Severity only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity icon based on level
                val severityIcon = when {
                    assessment.gad7Score == null -> Icons.Outlined.Help
                    assessment.gad7Score <= 4 -> Icons.Outlined.SentimentSatisfiedAlt
                    assessment.gad7Score <= 9 -> Icons.Outlined.SentimentNeutral
                    assessment.gad7Score <= 14 -> Icons.Outlined.SentimentDissatisfied
                    else -> Icons.Outlined.SentimentVeryDissatisfied
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(severityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        severityIcon,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = severity,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Show simple stats instead of numbers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Video count indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = HistoryMutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = assessment.videoCount.toString(),
                                fontSize = 12.sp,
                                color = HistoryMutedSlate
                            )
                        }

                        // Completed indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = HistorySageAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Completed",
                                fontSize = 12.sp,
                                color = HistorySageAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtle divider
            Divider(
                color = HistoryMutedSlate.copy(alpha = 0.2f),
                thickness = 1.dp
            )
        }
    }
}