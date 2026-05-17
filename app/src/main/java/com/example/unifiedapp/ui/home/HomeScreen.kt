package com.example.unifiedapp.ui.home

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.unifiedapp.R
import java.time.format.TextStyle.FULL
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.TrialState
import com.example.unifiedapp.ui.views.UserPreferences
import com.example.unifiedapp.ui.views.UserData
import com.example.unifiedapp.ui.navigation.Screen
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

// Logo-based Color Palette - Harmonized with Sage Green
val LogoOrange = Color(0xFFE8894A)
val LogoOrangeSoft = Color(0xFFF0A36B)
val LogoOrangeMuted = Color(0xFFD4945A)
val LogoDarkBrown = Color(0xFF3B2E2A)
val LogoSoftGreen = Color(0xFFE8F0E8)
val LogoAccentGreen = Color(0xFF7FAF7A)

// New Sage Green Gradient Colors
val SageLightest = Color(0xFFA9C8A7)
val SageLight = Color(0xFF8FB4A3)
val SagePrimary = Color(0xFF6F9E97)
val SageDark = Color(0xFF5F8E8D)

val CardWhite = Color(0xFFFFFFFF)
val InactiveGrey = Color(0xFFB8B8B8)
val SoftGreyText = Color(0xFF6B6B6B)

@RequiresApi(Build.VERSION_CODES.O)
val currentDayInt = LocalDate.now().dayOfWeek.value

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userPreferences: UserPreferences,  // ✅ Added userPreferences
    onToQuestion: () -> Unit,
    onStartCheckIn: () -> Unit,
    onNavigateToGround: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToWater: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToSleep: () -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    // ✅ Get user login state from UserPreferences
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

    // Observe trial state
    val trialState by authViewModel.trialState.collectAsStateWithLifecycle()

    // Track whether the user clicked the button (user-initiated check)
    var isUserInitiated by remember { mutableStateOf(false) }

    // ONLY fetch remaining trials count when screen loads - NO ASSESSMENT START
    LaunchedEffect(userData.isLoggedIn) {
        if (userData.isLoggedIn) {
            authViewModel.fetchTrialCountOnly() // This only fetches count, doesn't trigger assessment
        }
    }

    // Extract remaining trials if state is CanProceed
    val remainingTrials = (trialState as? TrialState.CanProceed)?.remaining
    val totalTrials = (trialState as? TrialState.CanProceed)?.total

    if (showSheet) {
        GroundingScreen(onBack = { showSheet = false })
    }

    val current = LocalDateTime.now()
    val day = current.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
    val month = current.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
    val dayOfMonth = current.dayOfMonth

    // Handle trial state - ONLY navigate if user clicked the button
    LaunchedEffect(trialState) {
        when (trialState) {
            is TrialState.CanProceed -> {
                // ONLY start assessment if this was triggered by button click
                if (isUserInitiated) {
                    onStartCheckIn()
                    isUserInitiated = false // Reset flag
                    authViewModel.resetTrialState() // Reset AFTER navigation
                }
            }
            is TrialState.Blocked -> {
                if (isUserInitiated) {
                    val message = (trialState as TrialState.Blocked).message
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    isUserInitiated = false
                    authViewModel.resetTrialState()
                }
                if (!isUserInitiated) {
                    kotlinx.coroutines.delay(3000)
                    authViewModel.resetTrialState()
                }
            }
            is TrialState.Error -> {
                if (isUserInitiated) {
                    val errorMessage = (trialState as TrialState.Error).message
                    val userFriendlyMessage = if (errorMessage.contains("403") ||
                        errorMessage.contains("no trials", ignoreCase = true) ||
                        errorMessage.contains("FAILED", ignoreCase = true)
                    ) {
                        "No trials left. Please contact admin for more trials."
                    } else {
                        errorMessage
                    }
                    Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
                    isUserInitiated = false
                    authViewModel.resetTrialState()
                } else {
                    authViewModel.resetTrialState()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            EnhancedBottomBar(
                navController,
                onNavigateToHistory,
                onProfileClick,
                onNavigateToMenu
            )
        },
        containerColor = LogoSoftGreen
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LogoSoftGreen)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1️⃣ Header Section with Muted Orange Underline
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sahay_logo),
                    contentDescription = "Sahay Logo",
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Sahay",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LogoDarkBrown,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "$day, $month $dayOfMonth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGreyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(LogoOrangeMuted, LogoOrangeSoft)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3️⃣ Illustration with Soft Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(SageLightest.copy(alpha = 0.3f), LogoSoftGreen)
                            )
                        )
                )

                Image(
                    painter = painterResource(R.drawable.veena),
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .graphicsLayer { translationY = -10f },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4️⃣ Welcome Section with Muted Orange
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "WELCOME BACK",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = LogoOrangeMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (userData.isLoggedIn) "Ready for your daily check-in?" else "Please login to continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = LogoDarkBrown,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Check-in Button with trial info - ✅ Updated for login state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        SageLightest,
                                        SageLight,
                                        SageLight,
                                        SagePrimary,
                                        SageDark
                                    )
                                )
                            )
                            .clickable {
                                if (userData.isLoggedIn) {
                                    // User is logged in - check trials if not already loading
                                    if (trialState !is TrialState.Loading) {
                                        isUserInitiated = true
                                        authViewModel.checkAnxietyTrials()
                                    }
                                } else {
                                    // User is not logged in - navigate to login page
                                    navController.navigate(Screen.Login.route)
                                }
                            }
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(30.dp),
                                spotColor = SagePrimary.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // Not logged in - show "Login to Start"
                            !userData.isLoggedIn -> {
                                Text(
                                    text = "Login to Start",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                            // Logged in but checking trials
                            trialState is TrialState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            // Logged in and ready to start
                            else -> {
                                Text(
                                    text = "Start Assessment",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    // Show remaining trials only if logged in and not loading
                    if (userData.isLoggedIn && remainingTrials != null && totalTrials != null && trialState !is TrialState.Loading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$remainingTrials of $totalTrials assessments left",
                            fontSize = 12.sp,
                            color = LogoDarkBrown.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Rest of your UI remains the same...
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LogoDarkBrown
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = LogoDarkBrown.copy(alpha = 0.1f)
                    )
                    .clickable { onNavigateToMenu() }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(7) { index ->
                            val dayNumber = index + 1
                            val isActive = dayNumber <= currentDayInt
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = if (isActive) LogoAccentGreen else InactiveGrey,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal Wellness",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LogoDarkBrown
                            )
                            Text(
                                text = "Day ${currentDayInt} of 7",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftGreyText
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ArrowCircleRight,
                            contentDescription = "Continue to Wealth",
                            tint = SagePrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to view your wealth journey",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGreyText.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Daily Wellness",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LogoDarkBrown
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SimpleToolCard(
                    label = "Journal",
                    icon = Icons.Outlined.Book,
                    bgColor = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToJournal
                )
                SimpleToolCard(
                    label = "Mood",
                    icon = Icons.Outlined.SentimentSatisfiedAlt,
                    bgColor = Color(0xFFF3E8FF),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMood
                )
                SimpleToolCard(
                    label = "Sleep",
                    icon = Icons.Outlined.Bed,
                    bgColor = Color(0xFFFFF1E0),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSleep
                )
                SimpleToolCard(
                    label = "Water",
                    icon = Icons.Outlined.WaterDrop,
                    bgColor = Color(0xFFFEE4E4),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToWater
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SimpleToolCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(100.dp),
        onClick = onClick,
        shadowElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LogoDarkBrown.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = LogoDarkBrown
            )
        }
    }
}

@Composable
fun EnhancedBottomBar(
    navController: NavHostController,
    onNavigateToHistory: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(
            elevation = 8.dp,
            spotColor = LogoDarkBrown.copy(alpha = 0.1f)
        )
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = LogoAccentGreen
                )
            },
            label = {
                Text(
                    "Home",
                    color = LogoAccentGreen
                )
            },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Outlined.AutoGraph,
                    contentDescription = "Wealth",
                    tint = InactiveGrey
                )
            },
            label = {
                Text(
                    "Wealth",
                    color = InactiveGrey
                )
            },
            selected = false,
            onClick = onNavigateToMenu
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = "History",
                    tint = InactiveGrey
                )
            },
            label = {
                Text(
                    "History",
                    color = InactiveGrey
                )
            },
            selected = false,
            onClick = onNavigateToHistory
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = InactiveGrey
                )
            },
            label = {
                Text(
                    "Me",
                    color = InactiveGrey
                )
            },
            selected = false,
            onClick = onProfileClick
        )
    }
}