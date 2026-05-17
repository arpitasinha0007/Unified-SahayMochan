package com.example.unifiedapp.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import android.os.Environment
import com.example.unifiedapp.utils.UserSessionHelper
import com.example.unifiedapp.utils.loadSavedUser
import com.example.unifiedapp.utils.deleteUserAccount
import com.example.unifiedapp.utils.logoutUser

// ============ LOCAL COLOR DEFINITIONS ============
private val LocalPurplePrimary = Color(0xFF8B5CF6)
private val LocalPurpleSecondary = Color(0xFFA78BFA)
private val LocalPurpleDark = Color(0xFF7C3AED)
private val LocalSoftPurpleBg = Color(0xFFF5F3FF)
private val LocalSoftPurpleBorder = Color(0xFFE0E7FF)
private val LocalColorTextPrimary = Color(0xFF1F2937)
private val LocalColorTextSecondary = Color(0xFF6B7280)
private val LocalColorBorder = Color(0xFFE5E7EB)
private val LocalColorCardBg = Color.White
private val LocalColorSuccess = Color(0xFF10B981)
private val LocalColorError = Color(0xFFEF4444)

private val LocalGradientStart = Color(0xFFFF385C)
private val LocalGradientMid = Color(0xFFFF5E3A)
private val LocalGradientEnd = Color(0xFFFF9345)

private val LocalGradientPurpleDark = Brush.linearGradient(
    colors = listOf(LocalPurpleDark, LocalPurplePrimary)
)

private val LocalGradientPurple = Brush.linearGradient(
    colors = listOf(LocalPurplePrimary, LocalPurpleSecondary)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val currentUser = loadSavedUser(context)
    val isLoggedIn = currentUser != null
    val registrationId = currentUser?.registration_id ?: ""

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
                PrivacyHeaderContent(
                    onBack = { navController.popBackStack() },
                    isLoggedIn = isLoggedIn,
                    onViewHistory = {
                        navController.navigate("assessment_history")
                    }
                )
            }

            if (!isLoggedIn) {
                item {
                    NotLoggedInContent(
                        onLoginClick = {
                            navController.navigate("auth") {
                                popUpTo("privacy_data") { inclusive = true }
                            }
                        }
                    )
                }
            } else {
                item {
                    WelcomeUserContent(
                        userName = currentUser?.name ?: "User",
                        modifier = Modifier.padding(20.dp)
                    )
                }

                if (errorMessage != null) {
                    item {
                        StatusMessageDisplay(
                            message = errorMessage!!,
                            isError = true,
                            onDismiss = { errorMessage = null }
                        )
                    }
                }

                if (successMessage != null) {
                    item {
                        StatusMessageDisplay(
                            message = successMessage!!,
                            isError = false,
                            onDismiss = { successMessage = null }
                        )
                    }
                }

                item {
                    DataManagementContent(
                        onViewHistoryClick = {
                            navController.navigate("assessment_history")
                        },
                        onDeleteAccountClick = {
                            showDeleteAccountDialog = true
                        },
                        modifier = Modifier.padding(20.dp)
                    )
                }

                item {
                    PrivacyInfoContent(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        if (isDeleting) {
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
                        CircularProgressIndicator(
                            color = LocalPurplePrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Deleting account...",
                            color = LocalColorTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                registrationId = registrationId,
                confirmationText = deleteConfirmationText,
                onConfirmationTextChange = { deleteConfirmationText = it },
                onConfirm = {
                    coroutineScope.launch {
                        isDeleting = true
                        showDeleteAccountDialog = false

                        val result = withContext(Dispatchers.IO) {
                            deleteUserAccount(registrationId)
                        }

                        if (result.success) {
                            deleteLocalUserFiles(context)
                            logoutUser(context)
                            successMessage = "Your account has been deleted successfully"
                            Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_LONG).show()
                            delay(1500)
                            navController.popBackStack()
                        } else {
                            errorMessage = result.message
                            isDeleting = false
                        }
                        deleteConfirmationText = ""
                    }
                },
                onDismiss = {
                    showDeleteAccountDialog = false
                    deleteConfirmationText = ""
                }
            )
        }
    }
}

@Composable
fun PrivacyHeaderContent(
    onBack: () -> Unit,
    isLoggedIn: Boolean,
    onViewHistory: () -> Unit
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
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = LocalPurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Privacy & Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LocalColorTextPrimary
                )
                Text(
                    if (isLoggedIn) "Manage your account and privacy"
                    else "Sign in to manage your data",
                    fontSize = 13.sp,
                    color = LocalColorTextSecondary
                )
            }

            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalSoftPurpleBg)
                        .border(1.dp, LocalSoftPurpleBorder, RoundedCornerShape(12.dp))
                        .clickable { onViewHistory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "View History",
                        tint = LocalPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalGradientPurpleDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeUserContent(
    userName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LocalColorCardBg),
        border = BorderStroke(1.dp, LocalColorBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            LocalGradientStart.copy(alpha = 0.05f),
                            LocalGradientEnd.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(LocalGradientStart, LocalGradientMid, LocalGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Welcome, $userName!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalColorTextPrimary
                )
                Text(
                    text = "Manage your privacy and account settings",
                    fontSize = 14.sp,
                    color = LocalColorTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun NotLoggedInContent(
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
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
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = LocalPurplePrimary,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Not Signed In",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LocalColorTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Please sign in to manage your privacy settings and account",
            fontSize = 14.sp,
            color = LocalColorTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(LocalGradientStart, LocalGradientMid, LocalGradientEnd)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Go to Login",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DataManagementContent(
    onViewHistoryClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LocalColorCardBg),
        border = BorderStroke(1.dp, LocalSoftPurpleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Data Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LocalColorTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onViewHistoryClick() },
                color = LocalSoftPurpleBg,
                border = BorderStroke(1.dp, LocalSoftPurpleBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LocalGradientPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Assessment History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalColorTextPrimary
                        )
                        Text(
                            "View and manage your past assessments",
                            fontSize = 12.sp,
                            color = LocalColorTextSecondary
                        )
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = LocalPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDeleteAccountClick() },
                color = LocalColorError.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, LocalColorError.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LocalColorError.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = LocalColorError,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalColorError
                        )
                        Text(
                            "Permanently delete your account and all data",
                            fontSize = 12.sp,
                            color = LocalColorTextSecondary
                        )
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = LocalColorError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = LocalSoftPurpleBg
            ) {
                Text(
                    "⚠️ Deleting your account is permanent and cannot be undone",
                    fontSize = 12.sp,
                    color = LocalColorTextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyInfoContent(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSoftPurpleBg),
        border = BorderStroke(1.dp, LocalSoftPurpleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = LocalPurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Your Privacy Matters",
                    fontWeight = FontWeight.Bold,
                    color = LocalColorTextPrimary,
                    fontSize = 16.sp
                )
            }

            Text(
                "• Your assessment data is encrypted and stored securely",
                fontSize = 13.sp,
                color = LocalColorTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "• You can view and delete your assessment history anytime",
                fontSize = 13.sp,
                color = LocalColorTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "• Deleting your account removes all your data permanently",
                fontSize = 13.sp,
                color = LocalColorTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StatusMessageDisplay(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) LocalColorError.copy(alpha = 0.1f) else LocalColorSuccess.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) LocalColorError else LocalColorSuccess
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = if (isError) LocalColorError else LocalColorSuccess,
                fontSize = 13.sp
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    registrationId: String,
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
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = LocalColorError,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Delete Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalColorTextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    "This will permanently delete:",
                    fontSize = 14.sp,
                    color = LocalColorTextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "Your user account",
                    "All assessment records",
                    "All video recordings",
                    "All CSV data files"
                ).forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = LocalColorError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            item,
                            fontSize = 13.sp,
                            color = LocalColorTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = LocalColorError.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, LocalColorError.copy(alpha = 0.3f))
                ) {
                    Text(
                        "⚠️ This action is IRREVERSIBLE. All your data will be permanently lost and you will not be able to recover it.",
                        fontSize = 12.sp,
                        color = LocalColorError,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = onConfirmationTextChange,
                    label = { Text("Type DELETE to confirm") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = confirmationText.isNotBlank() && !isConfirmed,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalColorError,
                        unfocusedBorderColor = LocalColorBorder,
                        cursorColor = LocalColorError,
                        focusedLabelColor = LocalColorError
                    )
                )

                if (confirmationText.isNotBlank() && !isConfirmed) {
                    Text(
                        "Please type 'DELETE' exactly to confirm",
                        fontSize = 12.sp,
                        color = LocalColorError,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirmed) LocalColorError else LocalColorError.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete My Account", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalColorTextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

fun deleteLocalUserFiles(context: Context) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(downloadsDir, "unifiedapp")

        if (appFolder.exists()) {
            val userSession = UserSessionHelper.getUserData(context)
            if (userSession.isLoggedIn && userSession.anonymousId.isNotBlank()) {
                val userFolder = File(appFolder, userSession.anonymousId)
                if (userFolder.exists()) {
                    userFolder.deleteRecursively()
                    Log.d("PrivacyData", "Deleted local user folder: ${userFolder.absolutePath}")
                }
            }
        }

        val reportsFolder = File(downloadsDir, "unifiedapp/Reports")
        if (reportsFolder.exists()) {
            reportsFolder.deleteRecursively()
            Log.d("PrivacyData", "Deleted reports folder")
        }

    } catch (e: Exception) {
        Log.e("PrivacyData", "Error deleting local files", e)
    }
}