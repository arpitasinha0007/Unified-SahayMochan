package com.example.unifiedapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.R
import com.example.unifiedapp.data.UserProfile
import com.example.unifiedapp.data.UserSessionManager
import com.example.unifiedapp.navigation.Screen
import com.example.unifiedapp.utils.TrialHelper
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

// New color palette for Home, Wellness, Profile
private val LavenderPrimary = Color(0xFF9D8DF1)
private val LavenderBackground = Color(0xFFFAF8FF)
private val LavenderAccent = Color(0xFFD9D1FF)

sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Filled.Home, "Home")
    object Wellness : BottomNavItem("wellness", Icons.Filled.Spa, "Wellness")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, "Profile")
}

@Composable
fun DashboardScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }

    val userData = remember { sessionManager.getUser() }
    val isLoggedIn = userData?.isLoggedIn ?: false
    val userName = userData?.name ?: ""
    val registrationId = userData?.registrationId ?: ""

    val tabSaver = Saver<BottomNavItem, String>(
        save = { it.route },
        restore = { route ->
            when (route) {
                "home" -> BottomNavItem.Home
                "wellness" -> BottomNavItem.Wellness
                "profile" -> BottomNavItem.Profile
                else -> BottomNavItem.Home
            }
        }
    )

    var selectedTab by rememberSaveable(stateSaver = tabSaver) {
        mutableStateOf(BottomNavItem.Home)
    }

    var showNoTrialsDialog by remember { mutableStateOf(false) }
    var remainingTrials by remember { mutableStateOf<Int?>(null) }
    var isLoadingTrials by remember { mutableStateOf(false) }

    var loginToastShown by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn, registrationId) {
        if (isLoggedIn && registrationId.isNotEmpty()) {
            isLoadingTrials = true
            try {
                remainingTrials = TrialHelper.getRemainingDepressionTrials(registrationId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingTrials = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
            ) {
                NavigationBarItem(
                    icon = { Icon(if (selectedTab == BottomNavItem.Home) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == BottomNavItem.Home,
                    onClick = { selectedTab = BottomNavItem.Home },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LavenderPrimary,
                        selectedTextColor = LavenderPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(if (selectedTab == BottomNavItem.Wellness) Icons.Filled.Spa else Icons.Outlined.Spa, contentDescription = "Wellness") },
                    label = { Text("Wellness") },
                    selected = selectedTab == BottomNavItem.Wellness,
                    onClick = { selectedTab = BottomNavItem.Wellness },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LavenderPrimary,
                        selectedTextColor = LavenderPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(if (selectedTab == BottomNavItem.Profile) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == BottomNavItem.Profile,
                    onClick = { selectedTab = BottomNavItem.Profile },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LavenderPrimary,
                        selectedTextColor = LavenderPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                BottomNavItem.Home -> UnifiedHomeContent(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    remainingTrials = remainingTrials,
                    isLoadingTrials = isLoadingTrials,
                    registrationId = registrationId,
                    onShowNoTrialsDialog = { showNoTrialsDialog = it },
                    onLoginClick = {
                        if (!loginToastShown) {
                            loginToastShown = true
                            Toast.makeText(context, "Please login from Profile tab", Toast.LENGTH_SHORT).show()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loginToastShown = false }, 2000)
                        }
                    }
                )

                BottomNavItem.Wellness -> UnifiedWellnessContent(navController = navController)

                BottomNavItem.Profile -> UnifiedProfileContent(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    userData = userData,
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(Screen.AUTH) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                        onLogout()
                    }
                )
            }
        }
    }

    if (showNoTrialsDialog) {
        AlertDialog(
            onDismissRequest = { showNoTrialsDialog = false },
            title = { Text("No Trials Left") },
            text = { Text("You have used all your depression assessment trials. Please contact the admin for more trials.") },
            confirmButton = {
                TextButton(onClick = { showNoTrialsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// ========== HOME TAB (unchanged) ==========
@Composable
fun UnifiedHomeContent(
    navController: NavController,
    isLoggedIn: Boolean,
    userName: String,
    remainingTrials: Int?,
    isLoadingTrials: Boolean,
    registrationId: String,
    onShowNoTrialsDialog: (Boolean) -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var depressionTrialsRemaining by remember { mutableStateOf(remainingTrials) }
    var anxietyTrialsRemaining by remember { mutableStateOf<Int?>(null) }
    var isLoadingAnxietyTrials by remember { mutableStateOf(false) }

    // Fetch anxiety trials
    LaunchedEffect(isLoggedIn, registrationId) {
        if (isLoggedIn && registrationId.isNotEmpty()) {
            isLoadingAnxietyTrials = true
            try {
                anxietyTrialsRemaining = TrialHelper.getRemainingAnxietyTrials(registrationId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingAnxietyTrials = false
            }
        }
    }

    LaunchedEffect(remainingTrials) {
        depressionTrialsRemaining = remainingTrials
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = LavenderBackground,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mochan_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(70.dp).aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        "Mental Health",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        if (isLoggedIn) "Welcome, $userName" else "Your mental wellness partner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== SAHAY CARD (Anxiety) ==========
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLoggedIn) {
                            coroutineScope.launch {
                                if (TrialHelper.checkAnxietyTrials(registrationId)) {
                                    navController.navigate(Screen.SAHAY_CONSENT)
                                } else {
                                    Toast.makeText(context, "No anxiety trials left. Contact admin.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            onLoginClick()
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = "Anxiety", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Surface(color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp)) {
                                Text("GAD-7", color = Color(0xFF1565C0), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Anxiety Assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("GAD-7 questionnaire with AI-powered facial analysis", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))

                        // Anxiety trial display
                        when {
                            isLoggedIn && !isLoadingAnxietyTrials && anxietyTrialsRemaining != null -> {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFFE4E8), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE91E63))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (anxietyTrialsRemaining) {
                                            0 -> "No anxiety assessments remaining"
                                            1 -> "1 anxiety assessment remaining"
                                            else -> "$anxietyTrialsRemaining anxiety assessments remaining"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                            isLoggedIn && isLoadingAnxietyTrials -> {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFFE4E8), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFFE91E63))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking anxiety trials...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = Color(0xFFC62828))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Takes 3–4 minutes • Camera required • Private and Secure", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                    }
                }
            }

            // ========== MOCHAN CARD (Depression) ==========
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLoggedIn) {
                            coroutineScope.launch {
                                if (TrialHelper.checkDepressionTrials(registrationId)) {
                                    navController.navigate(Screen.MOCHAN_CONSENT)
                                } else {
                                    onShowNoTrialsDialog(true)
                                }
                            }
                        } else {
                            onLoginClick()
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = listOf(Color(0xFFFFF0F5), Color(0xFFFFF7ED))))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Brush.linearGradient(listOf(Color(0xFFFF8A80), Color(0xFFFFB74D))), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = "Depression", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Surface(color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp)) {
                                Text("PHQ-9", color = Color(0xFFBE185D), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Depression Assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("PHQ-9 questionnaire with AI facial analysis", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))

                        // Depression trial display
                        when {
                            isLoggedIn && !isLoadingTrials && depressionTrialsRemaining != null -> {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFFE4E8), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE91E63))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (depressionTrialsRemaining) {
                                            0 -> "No depression assessments remaining"
                                            1 -> "1 depression assessment remaining"
                                            else -> "$depressionTrialsRemaining depression assessments remaining"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                            isLoggedIn && isLoadingTrials -> {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFFE4E8), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFFE91E63))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking depression trials...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = Color(0xFFC62828))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Takes 3-4 minutes • Camera required • Private & Secure", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                    }
                }
            }
        }
    }
}

// ========== WELLNESS TAB (unchanged) ==========
@Composable
fun UnifiedWellnessContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Wellness Tools", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.BREATHING) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Breathing Exercises", fontWeight = FontWeight.Bold)
                    Text("Guided breathing for relaxation", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOUNDS) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Calming Sounds", fontWeight = FontWeight.Bold)
                    Text("Soothing sounds for relaxation", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.MOOD_TRACKER) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Mood Tracker", fontWeight = FontWeight.Bold)
                    Text("Track your daily mood", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.JOURNAL) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Journal", fontWeight = FontWeight.Bold)
                    Text("Write down your thoughts", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.GROUNDING) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nature, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Grounding Exercises", fontWeight = FontWeight.Bold)
                    Text("5-4-3-2-1 grounding technique", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ========== PROFILE TAB (UPDATED – added Assessment History card) ==========
@Composable
fun UnifiedProfileContent(
    navController: NavController,
    isLoggedIn: Boolean,
    userName: String,
    userData: UserProfile?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var loginToastShown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn && userData != null) {
                // Main Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(LavenderAccent, LavenderPrimary.copy(alpha = 0.7f))
                                    ),
                                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userData.name.take(1).uppercase(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LavenderPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(userData.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(userData.email, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileInfoCardItem(
                                icon = Icons.Default.Person,
                                label = "Name",
                                value = userData.name,
                                iconColor = LavenderPrimary,
                                cardColor = LavenderAccent.copy(alpha = 0.3f)
                            )
                            ProfileInfoCardItem(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = userData.email,
                                iconColor = LavenderPrimary,
                                cardColor = LavenderAccent.copy(alpha = 0.3f)
                            )
                            ProfileInfoCardItem(
                                icon = Icons.Outlined.ConfirmationNumber,
                                label = "Registration ID",
                                value = userData.registrationId,
                                iconColor = LavenderPrimary,
                                cardColor = LavenderAccent.copy(alpha = 0.3f)
                            )
                            ProfileInfoCardItem(
                                icon = Icons.Default.Numbers,
                                label = "Age",
                                value = "${userData.age} years",
                                iconColor = LavenderPrimary,
                                cardColor = LavenderAccent.copy(alpha = 0.3f)
                            )
                            ProfileInfoCardItem(
                                icon = Icons.Default.Person,
                                label = "Gender",
                                value = userData.gender,
                                iconColor = LavenderPrimary,
                                cardColor = LavenderAccent.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // ✅ NEW: Assessment History Card
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.ASSESSMENT_HISTORY) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFFA78BFA)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Assessment History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "View your past assessments",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                // Not logged in card (unchanged)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = LavenderPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Not Logged In", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Please login to access your profile", fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (!loginToastShown) {
                                    loginToastShown = true
                                    Toast.makeText(context, "Please login from Login screen", Toast.LENGTH_SHORT).show()
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loginToastShown = false }, 2000)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
                        ) {
                            Text("Go to Login", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ========== HELPER FUNCTIONS (unchanged) ==========
@Composable
fun ProfileInfoCardItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color = LavenderPrimary,
    cardColor: Color = LavenderAccent.copy(alpha = 0.3f)
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, color = iconColor.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }
        }
    }
}