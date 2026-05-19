package com.example.unifiedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.unifiedapp.screens.*
import com.example.unifiedapp.ui.auth.UnifiedAuthScreen
import com.example.unifiedapp.theme.UnifiedAppTheme
import com.example.unifiedapp.data.UserSessionManager
import com.example.unifiedapp.navigation.sahayNavGraph
import com.example.unifiedapp.navigation.mochanNavGraph
import com.example.unifiedapp.navigation.SahayScreen
import com.example.unifiedapp.navigation.MochanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnifiedAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val sessionManager = UserSessionManager(applicationContext)

                    NavHost(
                        navController = navController,
                        startDestination = "launcher"
                    ) {
                        // Launcher Screen
                        composable("launcher") {
                            LauncherScreen(navController = navController)
                        }

                        // Auth Screen (Login/Signup)
                        composable("auth") {
                            UnifiedAuthScreen(
                                navController = navController,
                                onLoginSuccess = { userProfile ->
                                    sessionManager.saveUser(userProfile)
                                    navController.navigate("launcher") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Dashboard Screen
                        composable("dashboard") {
                            DashboardScreen(
                                navController = navController,
                                onLogout = {
                                    sessionManager.logout()
                                    navController.navigate("launcher") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ✅ ADD PROFILE SCREEN ROUTE
                        composable("profile") {
                            ProfileScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ✅ ADD PRIVACY DATA SCREEN ROUTE
                        composable("privacy_data") {
                            PrivacyDataScreen(
                                navController = navController
                            )
                        }

                        // ✅ ADD UNDERAGE RESULTS SCREEN ROUTE (FIXED)
                        composable(
                            route = "underage_results?score={score}&aiPrediction={aiPrediction}",
                            arguments = listOf(
                                navArgument("score") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("aiPrediction") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val score = backStackEntry.arguments?.getInt("score") ?: 0
                            val aiPrediction = backStackEntry.arguments?.getString("aiPrediction") ?: ""
                            UnderageResultScreen(
                                navController = navController,
                                score = score,
                                aiPrediction = aiPrediction,
                                onFinish = { navController.popBackStack("launcher", false) }
                            )
                        }

                        // Wellness Tools
                        composable("breathing") {
                            BreathingScreen(onBack = { navController.popBackStack() })
                        }
                        composable("sounds") {
                            SoundScreen(onBack = { navController.popBackStack() })
                        }
                        composable("journal") {
                            JournalScreen().JournalContent(navController = navController)
                        }
                        composable("mood_tracker") {
                            MoodTrackerScreen(onBack = { navController.popBackStack() })
                        }
                        composable("grounding") {
                            GroundingScreen(onBack = { navController.popBackStack() })
                        }
                        composable("assessment_history") {
                            AssessmentHistoryScreen(navController = navController)
                        }

                        // Sahay (Anxiety) Navigation Group
                        navigation(
                            startDestination = SahayScreen.DASHBOARD,
                            route = "sahay_graph"
                        ) {
                            sahayNavGraph(navController)
                        }

                        // Mochan (Depression) Navigation Group
                        navigation(
                            startDestination = MochanScreen.DASHBOARD,
                            route = "mochan_graph"
                        ) {
                            mochanNavGraph(navController)
                        }
                    }
                }
            }
        }
    }
}