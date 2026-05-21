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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.R
import com.example.unifiedapp.data.UserProfile
import com.example.unifiedapp.data.UserSessionManager
import com.example.unifiedapp.navigation.Screen  // ✅ ADD THIS IMPORT
import com.example.unifiedapp.utils.TrialHelper
import kotlinx.coroutines.launch

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

    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

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
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
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
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
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
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
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
                        navController.navigate(Screen.LAUNCHER) {
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

// The rest of the file (UnifiedHomeContent, UnifiedWellnessContent, UnifiedProfileContent) remains unchanged
// except that we now use Screen constants in navigation calls.

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
    val coroutineScope = rememberCoroutineScope()
    var trialsRemaining by remember { mutableStateOf(remainingTrials) }

    LaunchedEffect(remainingTrials) {
        trialsRemaining = remainingTrials
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = Color(0xFFFFFDF9).copy(alpha = 0.75f),
            shadowElevation = 1.dp
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
                        "Unified Mental Health",
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
            // Sahay Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLoggedIn) navController.navigate(Screen.SAHAY_CONSENT)
                        else onLoginClick()
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
                        Spacer(Modifier.height(16.dp))
                        Text("Takes 3–4 minutes • Camera required", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                    }
                }
            }

            // Mochan Card
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
                        when {
                            isLoggedIn && !isLoadingTrials && trialsRemaining != null -> {
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
                                        text = when (trialsRemaining) {
                                            0 -> "No assessments remaining"
                                            1 -> "1 assessment remaining"
                                            else -> "$trialsRemaining assessments remaining"
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
                                    Text("Checking trials...", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = Color(0xFFC62828))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Takes 2–3 minutes • Private & secure", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedWellnessContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Wellness Tools", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))

        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.BREATHING) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Breathing Exercises", fontWeight = FontWeight.Bold)
                    Text("Guided breathing for relaxation", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.SOUNDS) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Calming Sounds", fontWeight = FontWeight.Bold)
                    Text("Soothing sounds for relaxation", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.MOOD_TRACKER) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Mood Tracker", fontWeight = FontWeight.Bold)
                    Text("Track your daily mood", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.JOURNAL) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Journal", fontWeight = FontWeight.Bold)
                    Text("Write down your thoughts", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.GROUNDING) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nature, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Grounding Exercises", fontWeight = FontWeight.Bold)
                    Text("5-4-3-2-1 grounding technique", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3E8FF),
                        Color(0xFFE9D5FF),
                        Color(0xFFD8B4FE)
                    )
                )
            )
    ) {
        if (isLoggedIn && userData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFFA78BFA),
                                Color(0xFFC084FC)
                            )
                        )
                    )
                    .height(240.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = userName.take(1).uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = userData.email, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.ASSESSMENT_HISTORY) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Assessment History", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                            Text("View your past assessment results", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.PRIVACY_DATA) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Privacy", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy & Data", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                            Text("Manage your data and privacy settings", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFFA78BFA).copy(alpha = 0.1f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF8B5CF6), modifier = Modifier.size(50.dp))
                    }
                    Text("Not Logged In", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Text("Please login to access your profile", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Button(
                        onClick = {
                            if (!loginToastShown) {
                                loginToastShown = true
                                Toast.makeText(context, "Please login from Login screen", Toast.LENGTH_SHORT).show()
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ loginToastShown = false }, 2000)
                            }
                        },
                        modifier = Modifier.width(200.dp).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Go to Login", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}