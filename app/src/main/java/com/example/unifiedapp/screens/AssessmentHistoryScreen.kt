package com.example.unifiedapp.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.unifiedapp.utils.*
import org.json.JSONObject

// ============ LOCAL COLOR DEFINITIONS ============
private val LocalPurplePrimary = Color(0xFF8B5CF6)
private val LocalPurpleSecondary = Color(0xFFA78BFA)
private val LocalPurpleDark = Color(0xFF7C3AED)
private val LocalSoftPurpleBg = Color(0xFFF5F3FF)
private val LocalSoftPurpleBorder = Color(0xFFE0E7FF)
private val LocalColorTextPrimary = Color(0xFF1F2937)
private val LocalColorTextSecondary = Color(0xFF6B7280)
private val LocalColorTextTertiary = Color(0xFF9CA3AF)
private val LocalColorBorder = Color(0xFFE5E7EB)
private val LocalColorCardBg = Color.White
private val LocalColorSuccess = Color(0xFF10B981)
private val LocalColorError = Color(0xFFEF4444)

private val LocalGradientStart = Color(0xFFFF385C)
private val LocalGradientMid = Color(0xFFFF5E3A)
private val LocalGradientEnd = Color(0xFFFF9345)

private val LocalMildColor = Color(0xFF10B981)
private val LocalMildLightColor = Color(0xFFECFDF5)
private val LocalModerateColor = Color(0xFFF59E0B)
private val LocalModerateLightColor = Color(0xFFFFFBEB)
private val LocalSevereColor = Color(0xFFEF4444)
private val LocalSevereLightColor = Color(0xFFFFF1F2)

// ============ DATA CLASS ============
data class AssessmentHistoryItem(
    val id: Int,
    val assessmentType: String,      // "Depression (PHQ-9)" or "Anxiety (GAD-7)"
    val phqScore: Int?,              // PHQ‑9 score (for depression)
    val gad7Score: Int?,             // GAD‑7 score (for anxiety)
    val phq8Score: Int?,
    val aiScore: Int?,
    val aiConfidence: Float?,
    val aiPrediction: String?,
    val createdAt: String,
    val videoCount: Int,
    val files: AssessmentFiles?,
    // ✅ Store the raw type from API for accurate display
    val rawAssessmentType: String?   // "depression" or "anxiety"
)

data class AssessmentFiles(
    val videoPath: String?,
    val auCsvPath: String?,
    val phq9CsvPath: String?
)

// Helper function to format date
fun formatDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}

// API function to delete single assessment
suspend fun deleteSingleAssessment(assessmentId: Int): DeleteResult {
    return withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://203.110.243.202:8000/api/student/assessment/$assessmentId")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            connection.disconnect()

            if (responseCode in 200..299) {
                DeleteResult(true, "Assessment deleted")
            } else {
                DeleteResult(false, "Failed to delete: $responseCode")
            }
        } catch (e: Exception) {
            DeleteResult(false, "Error: ${e.message}")
        }
    }
}

data class DeleteResult(val success: Boolean, val message: String)

// ============ MAIN SCREEN ============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentHistoryScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var assessments by remember { mutableStateOf<List<AssessmentHistoryItem>>(emptyList()) }
    var selectedAssessment by remember { mutableStateOf<AssessmentHistoryItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deleteAllConfirmationText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var expandedCardId by remember { mutableStateOf<Int?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteProgress by remember { mutableStateOf(0) }
    var totalToDelete by remember { mutableStateOf(0) }

    val session = UserSessionHelper.getUserData(context)
    val isLoggedIn = session.isLoggedIn
    val registrationId = session.registrationId

    fun loadAssessments() {
        if (!isLoggedIn) {
            Log.w("AssessmentHistory", "Not logged in, cannot load history")
            errorMessage = "Please log in to view your assessment history"
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                if (registrationId.isEmpty()) {
                    Log.e("AssessmentHistory", "Registration ID is empty!")
                    errorMessage = "User not properly logged in. Please logout and login again."
                    isLoading = false
                    return@launch
                }

                val (responseCode, response) = withContext(Dispatchers.IO) {
                    val url = java.net.URL("http://203.110.243.202:8000/api/student/${registrationId}/assessments")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/json")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val code = connection.responseCode
                    val resp = if (code == 200) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    connection.disconnect()
                    Pair(code, resp)
                }

                val result = mutableListOf<AssessmentHistoryItem>()

                if (responseCode == 200 && response.isNotEmpty()) {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)
                    val assessmentsArray = json.optJSONArray("assessments")

                    if (success && assessmentsArray != null) {
                        for (i in 0 until assessmentsArray.length()) {
                            val item = assessmentsArray.getJSONObject(i)
                            val id = item.getInt("id")
                            val createdAt = item.getString("created_at")
                            val videoCount = item.optInt("video_count", 0)

                            // Get assessment type from API
                            val rawType = if (item.has("assessment_type") && !item.isNull("assessment_type")) {
                                item.getString("assessment_type")
                            } else null

                            // PHQ‑9 score (depression)
                            val phqScore = if (item.has("phq_score") && !item.isNull("phq_score")) {
                                item.getDouble("phq_score").toInt()
                            } else if (item.has("phq9_score") && !item.isNull("phq9_score")) {
                                item.getDouble("phq9_score").toInt()
                            } else null

                            // GAD‑7 score (anxiety)
                            val gad7Score = if (item.has("gad7_score") && !item.isNull("gad7_score")) {
                                item.getDouble("gad7_score").toInt()
                            } else if (item.has("gad_score") && !item.isNull("gad_score")) {
                                item.getDouble("gad_score").toInt()
                            } else null

                            // ✅ Determine display type correctly
                            val displayType = when {
                                rawType != null -> {
                                    when (rawType.lowercase()) {
                                        "depression" -> "Depression (PHQ-9)"
                                        "anxiety" -> "Anxiety (GAD-7)"
                                        else -> "Mental Health Assessment"
                                    }
                                }
                                phqScore != null && phqScore > 0 -> "Depression (PHQ-9)"
                                gad7Score != null && gad7Score > 0 -> "Anxiety (GAD-7)"
                                else -> "Mental Health Assessment"
                            }

                            Log.d("AssessmentHistory", "Assessment $id: rawType=$rawType, displayType=$displayType, phq=$phqScore, gad=$gad7Score")

                            result.add(
                                AssessmentHistoryItem(
                                    id = id,
                                    assessmentType = displayType,
                                    phqScore = phqScore,
                                    gad7Score = gad7Score,
                                    phq8Score = null,
                                    aiScore = null,
                                    aiConfidence = null,
                                    aiPrediction = null,
                                    createdAt = createdAt,
                                    videoCount = videoCount,
                                    files = null,
                                    rawAssessmentType = rawType
                                )
                            )
                        }
                    }
                }

                assessments = result.sortedByDescending { it.createdAt }
                Log.d("AssessmentHistory", "Loaded ${result.size} assessments")

                if (result.isEmpty() && errorMessage == null) {
                    errorMessage = "No assessments found. Complete an assessment to see it here."
                }

            } catch (e: Exception) {
                Log.e("AssessmentHistory", "Error loading assessments", e)
                errorMessage = "Failed to load assessments: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAssessments()
    }

    fun deleteAssessment(assessmentId: Int) {
        coroutineScope.launch {
            isDeleting = true
            showDeleteDialog = false

            val result = deleteSingleAssessment(assessmentId)

            if (result.success) {
                successMessage = "Assessment deleted successfully"
                assessments = assessments.filter { it.id != assessmentId }
                loadAssessments()
            } else {
                errorMessage = result.message
            }

            isDeleting = false
            selectedAssessment = null
        }
    }

    fun deleteAllAssessments() {
        coroutineScope.launch {
            isDeleting = true
            showDeleteAllDialog = false
            deleteProgress = 0
            totalToDelete = assessments.size

            val currentAssessments = assessments.toList()
            var successCount = 0

            for ((index, assessment) in currentAssessments.withIndex()) {
                deleteProgress = index + 1
                val result = deleteSingleAssessment(assessment.id)
                if (result.success) successCount++
                delay(200)
            }

            loadAssessments()
            deleteProgress = 0
            isDeleting = false
            deleteAllConfirmationText = ""

            if (successCount == totalToDelete) {
                successMessage = "All $totalToDelete assessments deleted successfully"
            } else {
                errorMessage = "Deleted $successCount of $totalToDelete assessments"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                AssessmentHistoryHeaderLocal(
                    onBack = { navController.popBackStack() },
                    isLoggedIn = isLoggedIn,
                    onRefresh = { loadAssessments() },
                    onDeleteAll = { if (assessments.isNotEmpty()) showDeleteAllDialog = true },
                    hasAssessments = assessments.isNotEmpty()
                )
            }

            if (!isLoggedIn) {
                item {
                    HistoryNotLoggedInContentLocal(
                        onLoginClick = {
                            navController.navigate("profile") {
                                popUpTo("assessment_history") { inclusive = true }
                            }
                        }
                    )
                }
            } else {
                if (errorMessage != null) {
                    item {
                        StatusMessageLocal(
                            message = errorMessage!!,
                            isError = true,
                            onDismiss = { errorMessage = null }
                        )
                    }
                }

                if (successMessage != null) {
                    item {
                        StatusMessageLocal(
                            message = successMessage!!,
                            isError = false,
                            onDismiss = { successMessage = null }
                        )
                    }
                }

                item {
                    HistoryStatsHeaderLocal(
                        totalCount = assessments.size,
                        onRefresh = { loadAssessments() },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LocalPurplePrimary, strokeWidth = 2.dp)
                        }
                    }
                }

                if (isDeleting && totalToDelete > 0) {
                    item {
                        DeleteProgressIndicatorLocal(current = deleteProgress, total = totalToDelete)
                    }
                }

                if (assessments.isEmpty() && !isLoading) {
                    item {
                        EmptyHistoryContentLocal()
                    }
                } else {
                    items(items = assessments, key = { it.id }) { assessment ->
                        AssessmentHistoryCardLocal(
                            assessment = assessment,
                            isExpanded = expandedCardId == assessment.id,
                            onExpandToggle = {
                                expandedCardId = if (expandedCardId == assessment.id) null else assessment.id
                            },
                            onDeleteClick = {
                                selectedAssessment = assessment
                                showDeleteDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        if (isDeleting && totalToDelete == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalColorCardBg),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = LocalPurplePrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deleting...", color = LocalColorTextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (showDeleteDialog && selectedAssessment != null) {
            DeleteConfirmationDialogLocal(
                assessment = selectedAssessment!!,
                onConfirm = { deleteAssessment(selectedAssessment!!.id) },
                onDismiss = {
                    showDeleteDialog = false
                    selectedAssessment = null
                }
            )
        }

        if (showDeleteAllDialog) {
            DeleteAllConfirmationDialogLocal(
                assessmentCount = assessments.size,
                confirmationText = deleteAllConfirmationText,
                onConfirmationTextChange = { deleteAllConfirmationText = it },
                onConfirm = { deleteAllAssessments() },
                onDismiss = {
                    showDeleteAllDialog = false
                    deleteAllConfirmationText = ""
                }
            )
        }
    }
}

// ============ LOCAL COMPOSABLES ============

@Composable
fun AssessmentHistoryHeaderLocal(
    onBack: () -> Unit,
    isLoggedIn: Boolean,
    onRefresh: () -> Unit,
    onDeleteAll: () -> Unit,
    hasAssessments: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .border(width = 0.5.dp, color = LocalSoftPurpleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalSoftPurpleBg)
                    .border(1.dp, LocalSoftPurpleBorder, RoundedCornerShape(12.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LocalPurplePrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Assessment History", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = LocalColorTextPrimary)
                Text(if (isLoggedIn) "View and manage your past assessments" else "Sign in to view your history", fontSize = 13.sp, color = LocalColorTextSecondary)
            }

            if (isLoggedIn && hasAssessments) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalColorError.copy(alpha = 0.1f))
                        .border(1.dp, LocalColorError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onDeleteAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Delete All", tint = LocalColorError, modifier = Modifier.size(20.dp))
                }
            }

            if (isLoggedIn) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalSoftPurpleBg)
                        .border(1.dp, LocalSoftPurpleBorder, RoundedCornerShape(12.dp))
                        .clickable { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LocalPurplePrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = listOf(LocalPurplePrimary, LocalPurpleSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun HistoryNotLoggedInContentLocal(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LocalSoftPurpleBg)
                .border(2.dp, LocalSoftPurpleBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = LocalPurplePrimary, modifier = Modifier.size(60.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Not Signed In", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please sign in to view your assessment history", fontSize = 14.sp, color = LocalColorTextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors = listOf(LocalGradientStart, LocalGradientMid, LocalGradientEnd)), RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Go to Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HistoryStatsHeaderLocal(totalCount: Int, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Your Assessments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(20.dp), color = LocalSoftPurpleBg, border = BorderStroke(1.dp, LocalSoftPurpleBorder)) {
            Text("$totalCount ${if (totalCount == 1) "item" else "items"}", fontSize = 12.sp, color = LocalPurplePrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun EmptyHistoryContentLocal() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp)).background(LocalSoftPurpleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = LocalPurplePrimary.copy(alpha = 0.5f), modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Assessments Yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Complete your first assessment to see it here", fontSize = 14.sp, color = LocalColorTextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun AssessmentHistoryCardLocal(
    assessment: AssessmentHistoryItem,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ Determine the correct score and display label based on assessment type
    val isDepression = assessment.assessmentType.contains("Depression", ignoreCase = true) ||
            assessment.rawAssessmentType.equals("depression", ignoreCase = true)
    val isAnxiety = assessment.assessmentType.contains("Anxiety", ignoreCase = true) ||
            assessment.rawAssessmentType.equals("anxiety", ignoreCase = true)

    // Use the appropriate score
    val score = when {
        isDepression -> assessment.phqScore ?: 0
        isAnxiety -> assessment.gad7Score ?: 0
        else -> assessment.phqScore ?: assessment.gad7Score ?: 0
    }

    val scoreLabel = when {
        isDepression -> "PHQ-9"
        isAnxiety -> "GAD-7"
        else -> "Score"
    }

    // Determine severity based on the correct type and score
    val (severityText, severityColor, severityLightColor) = when {
        isDepression -> {
            when {
                score <= 9 -> Triple("Mild", LocalMildColor, LocalMildLightColor)
                score <= 18 -> Triple("Moderate", LocalModerateColor, LocalModerateLightColor)
                else -> Triple("Severe", LocalSevereColor, LocalSevereLightColor)
            }
        }
        isAnxiety -> {
            when {
                score <= 7 -> Triple("Mild", LocalMildColor, LocalMildLightColor)
                score <= 14 -> Triple("Moderate", LocalModerateColor, LocalModerateLightColor)
                else -> Triple("Severe", LocalSevereColor, LocalSevereLightColor)
            }
        }
        else -> Triple("Unknown", LocalColorTextSecondary, LocalSoftPurpleBg)
    }

    Card(
        modifier = modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LocalColorCardBg),
        border = BorderStroke(1.dp, LocalColorBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandToggle() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(severityLightColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = severityColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = LocalColorTextTertiary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatDate(assessment.createdAt), fontSize = 12.sp, color = LocalColorTextTertiary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = severityColor.copy(alpha = 0.1f), border = BorderStroke(1.dp, severityColor.copy(alpha = 0.3f))) {
                        Text("$scoreLabel: $severityText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = severityColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    if (assessment.videoCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = LocalColorTextTertiary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${assessment.videoCount} video${if (assessment.videoCount > 1) "s" else ""}", fontSize = 11.sp, color = LocalColorTextTertiary)
                        }
                    }
                }
                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (isExpanded) "Collapse" else "Expand", tint = LocalColorTextSecondary, modifier = Modifier.size(24.dp))
            }

            if (isExpanded) {
                Divider(color = LocalColorBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Assessment Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    DetailRowLocal(label = "$scoreLabel Severity:", value = severityText, color = severityColor)
                    if (score > 0) {
                        val maxScore = if (isDepression) 27 else if (isAnxiety) 21 else 0
                        DetailRowLocal(label = "$scoreLabel Score:", value = "$score/$maxScore", color = severityColor)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalColorError.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, LocalColorError.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = LocalColorError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Assessment", color = LocalColorError, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRowLocal(label: String, value: String, color: Color = LocalColorTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = LocalColorTextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun StatusMessageLocal(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isError) LocalColorError.copy(alpha = 0.1f) else LocalColorSuccess.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isError) Icons.Default.Error else Icons.Default.CheckCircle, contentDescription = null, tint = if (isError) LocalColorError else LocalColorSuccess)
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, modifier = Modifier.weight(1f), color = if (isError) LocalColorError else LocalColorSuccess, fontSize = 13.sp)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun DeleteProgressIndicatorLocal(current: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalColorCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Deleting assessments...", fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
                Text("$current/$total", fontWeight = FontWeight.Bold, color = LocalPurplePrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = current.toFloat() / total.toFloat(), modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = LocalPurplePrimary, trackColor = LocalColorBorder)
            Text("Please wait while assessments are being deleted...", fontSize = 12.sp, color = LocalColorTextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun DeleteConfirmationDialogLocal(assessment: AssessmentHistoryItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = LocalColorCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = LocalColorError, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Assessment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
            }
        },
        text = {
            Column {
                Text("Are you sure you want to delete this assessment?", fontSize = 14.sp, color = LocalColorTextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = LocalSoftPurpleBg, border = BorderStroke(1.dp, LocalSoftPurpleBorder)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Assessment from ${formatDate(assessment.createdAt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
                        if (assessment.videoCount > 0) Text("Videos: ${assessment.videoCount}", fontSize = 12.sp, color = LocalColorTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("⚠️ This action cannot be undone", fontSize = 12.sp, color = LocalColorError, fontWeight = FontWeight.Medium)
            }
        },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = LocalColorError), shape = RoundedCornerShape(12.dp)) { Text("Delete", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = LocalColorTextSecondary)) { Text("Cancel") } }
    )
}

@Composable
fun DeleteAllConfirmationDialogLocal(
    assessmentCount: Int,
    confirmationText: String,
    onConfirmationTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isConfirmed = confirmationText.equals("DELETE", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = LocalColorCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = LocalColorError, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Assessments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalColorTextPrimary)
            }
        },
        text = {
            Column {
                Text("This will permanently delete all $assessmentCount assessment records from your history.", fontSize = 14.sp, color = LocalColorTextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = LocalColorError.copy(alpha = 0.1f), border = BorderStroke(1.dp, LocalColorError.copy(alpha = 0.3f))) {
                    Text("⚠️ This action is IRREVERSIBLE. All your assessment data will be permanently lost.", fontSize = 12.sp, color = LocalColorError, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmationText, onValueChange = onConfirmationTextChange,
                    label = { Text("Type DELETE to confirm") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    isError = confirmationText.isNotBlank() && !isConfirmed,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalColorError, unfocusedBorderColor = LocalColorBorder, cursorColor = LocalColorError, focusedLabelColor = LocalColorError)
                )
                if (confirmationText.isNotBlank() && !isConfirmed) Text("Please type 'DELETE' exactly to confirm", fontSize = 12.sp, color = LocalColorError, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = isConfirmed, colors = ButtonDefaults.buttonColors(containerColor = if (isConfirmed) LocalColorError else LocalColorError.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) { Text("Delete All", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = LocalColorTextSecondary)) { Text("Cancel") } }
    )
}