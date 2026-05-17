package com.example.unifiedapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unifiedapp.R
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.DeleteState
import com.example.unifiedapp.ui.views.UserData
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import com.example.unifiedapp.ui.navigation.Screen

// Sage Green Family
/*val SageLightest = Color(0xFFA9C8A7)
val SageLight = Color(0xFF8FB4A3)
val SagePrimary = Color(0xFF6F9E97)
val SageDark = Color(0xFF5F8E8D)*/

// Accent Colors
val SoftLavender = Color(0xFFE6E0FF)
val SoftPeach = Color(0xFFFFE4D6)
val SoftCoral = Color(0xFFFFB5A7)
val SoftYellow = Color(0xFFFFF0D4)
/*val CardWhite = Color(0xFFFFFFFF)
val SoftGreyText = Color(0xFF6B6B6B)*/

// Gradient Background
val SageGradient = Brush.verticalGradient(
    colors = listOf(
        SageLightest.copy(alpha = 0.3f),
        LogoSoftGreen,
        SageLight.copy(alpha = 0.2f)
    )
)

@Composable
fun ProfileScreen(
    userPreferences: UserPreferences,
    viewModel: AuthViewModel,
    navController: NavController,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTermsPopup by remember { mutableStateOf(false) }
    var showPrivacyPopup by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showParentInfoDialog by remember { mutableStateOf(false) }

    val userData by userPreferences.userData.collectAsState(
        initial = UserData(
            isLoggedIn = false,
            name = "",
            email = "",
            gender = "",
            age = 0,
            id = "",
            TOKEN = "",
            isUnderage = false,
            parentName = "",
            parentEmail = ""
        )
    )

    val deleteAllState by viewModel.deleteAllState.collectAsStateWithLifecycle()

    // Handle delete state
    LaunchedEffect(deleteAllState) {
        when (deleteAllState) {
            is DeleteState.Success -> {
                viewModel.resetDeleteAllState()
                // Clear all back stack and navigate to login
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is DeleteState.Error -> {
                snackbarHostState.showSnackbar(
                    (deleteAllState as DeleteState.Error).message
                )
                viewModel.resetDeleteAllState()
            }
            else -> Unit
        }
    }

    // Show popups when triggered
    if (showTermsPopup) {
        TermsAndConditionsPopup(
            onDismiss = { showTermsPopup = false }
        )
    }

    if (showPrivacyPopup) {
        PrivacyPolicyPopup(
            onDismiss = { showPrivacyPopup = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SageGradient)
                .padding(padding)
        ) {
            // Decorative Elements
            DecorativeBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Enhanced Header with Delete Icon
                ProfileHeader(
                    onBack = onBack,
                    showDeleteIcon = true,
                    onDeleteClick = { showDeleteDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Content Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = SagePrimary.copy(alpha = 0.2f)
                        )
                ) {
                    ProfileContent(
                        userData = userData
                    )
                }

                // ✅ Parent Information Section (only for underage users)
                if (userData.isUnderage && (userData.parentName.isNotBlank() || userData.parentEmail.isNotBlank())) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = SagePrimary.copy(alpha = 0.2f)
                            )
                    ) {
                        ParentInformationSection(
                            parentName = userData.parentName,
                            parentEmail = userData.parentEmail,
                            onViewParentInfo = { showParentInfoDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // History Section - Separate Card
                HistorySection(
                    onNavigateToHistory = {
                        navController.navigate(Screen.AssessmentHistory.route)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Legal Information Section - Separate Card
                LegalInformationSection(
                    onShowTerms = { showTermsPopup = true },
                    onShowPrivacy = { showPrivacyPopup = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Section - Separate Card for better organization
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = SagePrimary.copy(alpha = 0.2f)
                        )
                ) {
                    ActionButtonsSection(
                        onLogout = {
                            scope.launch {
                                userPreferences.logout()
                                snackbarHostState.showSnackbar("Logged out successfully")
                                // Navigate to login screen with cleared back stack
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Delete Account Confirmation Dialog with Type "DELETE" requirement
            if (showDeleteDialog) {
                DeleteAccountConfirmationDialog(
                    onConfirm = {
                        showDeleteDialog = false
                        viewModel.deleteAllUserData()
                    },
                    onDismiss = { showDeleteDialog = false },
                    isLoading = deleteAllState is DeleteState.Loading
                )
            }

            // ✅ Parent Information Dialog
            if (showParentInfoDialog && userData.isUnderage) {
                ParentInfoDialogForProfile(
                    userData = userData,
                    onDismiss = { showParentInfoDialog = false }
                )
            }
        }
    }
}

@Composable
fun DecorativeBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw decorative circles
        drawCircle(
            color = SageLightest.copy(alpha = 0.2f),
            radius = size.width * 0.4f,
            center = Offset(size.width * 0.8f, -50f)
        )
        drawCircle(
            color = LogoOrangeSoft.copy(alpha = 0.15f),
            radius = size.width * 0.3f,
            center = Offset(-50f, size.height * 0.3f)
        )
        drawCircle(
            color = SagePrimary.copy(alpha = 0.1f),
            radius = size.width * 0.35f,
            center = Offset(size.width * 0.2f, size.height * 0.7f)
        )
    }
}

@Composable
fun ProfileHeader(
    onBack: () -> Unit,
    showDeleteIcon: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button with enhanced styling
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .shadow(4.dp, CircleShape),
            color = CardWhite,
            tonalElevation = 2.dp
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LogoDarkBrown
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Title with decoration
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LogoDarkBrown
            )

            // Decorative line
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(LogoOrangeMuted, SagePrimary)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Delete Icon
        if (showDeleteIcon) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .shadow(4.dp, CircleShape),
                color = CardWhite,
                tonalElevation = 2.dp
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Account",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Empty space for balance
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun ProfileContent(
    userData: UserData
) {
    var name by remember(userData) { mutableStateOf(userData.name) }
    var email by remember(userData) { mutableStateOf(userData.email) }
    var ageText by remember(userData) {
        mutableStateOf(if (userData.age > 0) userData.age.toString() else "")
    }
    var gender by remember(userData) { mutableStateOf(userData.gender) }
    var id by remember(userData) { mutableStateOf(userData.id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Profile Avatar Section
        ProfileAvatar(name = name)

        Spacer(modifier = Modifier.height(24.dp))

        // User Info Card (Read-only display)
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftLavender.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                InfoRow(
                    icon = Icons.Outlined.Person,
                    label = "Name",
                    value = name.ifEmpty { "Not set" }
                )
                Divider(color = SageLight.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                InfoRow(
                    icon = Icons.Outlined.Badge,
                    label = "ID",
                    value = id.ifEmpty { "Not set" }
                )
                Divider(color = SageLight.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                InfoRow(
                    icon = Icons.Outlined.Cake,
                    label = "Age",
                    value = if (ageText.isNotEmpty()) "$ageText years" else "Not set"
                )
                Divider(color = SageLight.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                InfoRow(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    value = email.ifEmpty { "Not set" }
                )
                Divider(color = SageLight.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                InfoRow(
                    icon = Icons.Outlined.Wc,
                    label = "Gender",
                    value = gender.ifEmpty { "Not set" }
                )
            }
        }
    }
}

// Parent Information Section for Profile
@Composable
fun ParentInformationSection(
    parentName: String,
    parentEmail: String,
    onViewParentInfo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(LogoOrangeMuted, LogoOrangeSoft)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FamilyRestroom,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Parent/Guardian Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LogoDarkBrown
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Parent Info Card (Summary)
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftPeach.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewParentInfo() }
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Parent Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = LogoOrangeMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Parent/Guardian:",
                        fontSize = 12.sp,
                        color = SoftGreyText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parentName.ifEmpty { "Not provided" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LogoDarkBrown,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Parent Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = LogoOrangeMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Email:",
                        fontSize = 12.sp,
                        color = SoftGreyText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parentEmail.ifEmpty { "Not provided" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = LogoDarkBrown,
                        modifier = Modifier.weight(1f)
                    )
                }

                // View Details Indicator
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Tap to view details →",
                        fontSize = 11.sp,
                        color = LogoOrangeMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Info Note
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftLavender.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your parent/guardian receives important updates about your wellness journey.",
                    fontSize = 11.sp,
                    color = SageDark
                )
            }
        }
    }
}

// Parent Info Dialog for Profile Screen
@Composable
fun ParentInfoDialogForProfile(
    userData: UserData,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(LogoOrangeMuted, LogoOrangeSoft)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FamilyRestroom,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Parent/Guardian Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = LogoDarkBrown
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Since you're under 18, a parent/guardian has been associated with your account. They will receive important updates about your wellness journey.",
                    fontSize = 14.sp,
                    color = SoftGreyText,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Parent Name Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftPeach),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = LogoOrangeMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Parent/Guardian Name",
                                fontSize = 12.sp,
                                color = SoftGreyText
                            )
                            Text(
                                userData.parentName.ifEmpty { "Not provided" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LogoDarkBrown
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Parent Email Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftLavender),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            tint = SagePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Parent/Guardian Email",
                                fontSize = 12.sp,
                                color = SoftGreyText
                            )
                            Text(
                                userData.parentEmail.ifEmpty { "Not provided" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LogoDarkBrown
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Note
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MintCream,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = SagePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Your parent will receive notifications about your assessments and important updates. For any changes to parent information, please contact support.",
                            fontSize = 12.sp,
                            color = SageDark,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SagePrimary
                    )
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HistorySection(
    onNavigateToHistory: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = SagePrimary.copy(alpha = 0.2f)
            )
            .clickable { onNavigateToHistory() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(SageLight, SagePrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Assessment History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LogoDarkBrown
                )
                Text(
                    text = "View your past assessments and progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGreyText
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SagePrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LegalInformationSection(
    onShowTerms: () -> Unit,
    onShowPrivacy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = SagePrimary.copy(alpha = 0.2f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Gavel,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Legal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LogoDarkBrown
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms & Conditions Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onShowTerms() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Terms & Conditions",
                    color = LogoDarkBrown,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                color = SageLight.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            // Privacy Policy Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onShowPrivacy() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Privacy Policy",
                    color = LogoDarkBrown,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SagePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC00404).copy(alpha = 0.15f),  // Light red background
                contentColor = Color(0xFF881A1A)  // Dark red text/icon
                //contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SagePrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = SoftGreyText
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LogoDarkBrown
            )
        }
    }
}

@Composable
fun ProfileAvatar(name: String) {
    val initial = if (name.isNotEmpty()) name[0].toString().uppercase() else "?"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated rings
        Canvas(modifier = Modifier.size(120.dp)) {
            drawCircle(
                color = SageLightest.copy(alpha = 0.3f),
                radius = size.minDimension / 2,
                center = center
            )
            drawCircle(
                color = SagePrimary.copy(alpha = 0.2f),
                radius = size.minDimension / 2.5f,
                center = center
            )
        }

        // Avatar
        Surface(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .shadow(8.dp, CircleShape),
            color = SagePrimary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors)
            )
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = null,
                    tint = LogoOrangeMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                "Are you sure you want to log out?",
                color = SoftGreyText
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LogoOrangeMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Yes, Log Out")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var confirmationText by remember { mutableStateOf("") }
    val isConfirmed = confirmationText.equals("DELETE", ignoreCase = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Warning Icon and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = LogoDarkBrown
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This action is permanent and cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGreyText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All your data will be permanently deleted from our servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGreyText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Type DELETE confirmation field
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Type DELETE to confirm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isConfirmed) SagePrimary else MaterialTheme.colorScheme.error,
                        focusedLabelColor = if (isConfirmed) SagePrimary else MaterialTheme.colorScheme.error,
                        unfocusedBorderColor = SoftGreyText.copy(alpha = 0.3f)
                    ),
                    supportingText = {
                        if (confirmationText.isNotEmpty() && !isConfirmed) {
                            Text(
                                text = "Please type DELETE exactly",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons - Equal size and consistent styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button - Consistent size with Delete button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGreyText.copy(alpha = 0.1f),
                            contentColor = SoftGreyText
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Delete Button - Same size as Cancel button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = isConfirmed && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Delete",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}