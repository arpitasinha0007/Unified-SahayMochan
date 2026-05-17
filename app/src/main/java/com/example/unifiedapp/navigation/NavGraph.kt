package com.example.unifiedapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.unifiedapp.screens.AssessmentHistoryScreen
import com.example.unifiedapp.screens.AssessmentQuestionnairesScreen
import com.example.unifiedapp.screens.BreathingScreen
import com.example.unifiedapp.screens.ConsentScreen
import com.example.unifiedapp.screens.DashboardScreen
import com.example.unifiedapp.screens.GroundingScreen
import com.example.unifiedapp.screens.JournalScreen
import com.example.unifiedapp.screens.MoodTrackerScreen
import com.example.unifiedapp.screens.PrivacyDataScreen
import com.example.unifiedapp.screens.PrivacyPolicyPopup
import com.example.unifiedapp.screens.ProfileScreen
import com.example.unifiedapp.screens.ResultScreen
import com.example.unifiedapp.screens.SoundScreen
import com.example.unifiedapp.screens.TermsAndConditionsPopup
import com.example.unifiedapp.screens.UnderageResultScreen
import com.example.unifiedapp.screens.WellnessScreen
import com.example.unifiedapp.ui.Controller.CameraController
import com.example.unifiedapp.ui.views.UserPreferences
import com.example.unifiedapp.ui.views.CameraViewModel  // ✅ ADD THIS IMPORT

object Screen {
    const val DASHBOARD = "dashboard"
    const val PROFILE = "profile"
    const val WELLNESS = "wellness"
    const val PRIVACY_DATA = "privacy_data"
    const val ASSESSMENT_HISTORY = "assessment_history"

    // Auth routes
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Consent and Assessment routes
    const val CONSENT = "consent"
    const val ASSESSMENT = "assessment"
    const val RESULT = "result/{score}"
    const val UNDERAGE_RESULT = "underage_result"

    // Sahay (Anxiety) specific routes
    const val SAHAY_CONSENT = "sahay_consent"
    const val SAHAY_ASSESSMENT = "sahay_assessment"
    const val SAHAY_RESULT = "sahay_result/{score}"

    // Mochan (Depression) specific routes
    const val MOCHAN_CONSENT = "mochan_consent"
    const val MOCHAN_ASSESSMENT = "mochan_assessment"
    const val MOCHAN_RESULT = "mochan_result/{score}"

    // Wellness Tools
    const val BREATHING = "breathing"
    const val SOUNDS = "sounds"
    const val JOURNAL = "journal"
    const val MOOD_TRACKER = "mood_tracker"
    const val GROUNDING = "grounding"

    // Popups
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"
}

@Composable
fun UnifiedNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.DASHBOARD
) {
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)

    // ✅ FIXED: Create CameraViewModel with proper initialization
    val cameraViewModel: CameraViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CameraViewModel().apply {
                    init(context.applicationContext)
                } as T
            }
        }
    )

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Dashboard
        composable(Screen.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                onLogout = { }
            )
        }

        // Auth screens (add if you have them)
        composable(Screen.AUTH) {
            androidx.compose.material3.Text("Auth Screen - Create this")
        }

        // Update the composable calls to pass assessmentType
        composable(Screen.SAHAY_ASSESSMENT) {
            AssessmentQuestionnairesScreen(
                navController = navController,
                cameraViewModel = cameraViewModel,
                assessmentType = "anxiety"  // Pass anxiety type for Sahay
            )
        }

        composable(Screen.MOCHAN_ASSESSMENT) {
            AssessmentQuestionnairesScreen(
                navController = navController,
                cameraViewModel = cameraViewModel,
                assessmentType = "depression"  // Pass depression type for Mochan
            )
        }

        // Profile
        composable(Screen.PROFILE) {
            ProfileScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        // Wellness
        composable(Screen.WELLNESS) {
            WellnessScreen(navController = navController)
        }

        // Privacy & Data
        composable(Screen.PRIVACY_DATA) {
            PrivacyDataScreen(navController = navController)
        }

        // Assessment History
        composable(Screen.ASSESSMENT_HISTORY) {
            AssessmentHistoryScreen(navController = navController)
        }

        // ========== SAHAY (ANXIETY) FLOW ==========
        composable(Screen.SAHAY_CONSENT) {
            ConsentScreen(
                navController = navController,
                onAccept = {
                    navController.navigate(Screen.SAHAY_ASSESSMENT) {
                        popUpTo(Screen.SAHAY_CONSENT) { inclusive = true }
                    }
                },
                onDecline = { navController.popBackStack() }
            )
        }

        // ========== MOCHAN (DEPRESSION) FLOW ==========
        composable(Screen.MOCHAN_CONSENT) {
            ConsentScreen(
                navController = navController,
                onAccept = {
                    navController.navigate(Screen.MOCHAN_ASSESSMENT) {
                        popUpTo(Screen.MOCHAN_CONSENT) { inclusive = true }
                    }
                },
                onDecline = { navController.popBackStack() }
            )
        }

        // Result Screens
        composable(
            route = Screen.RESULT,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            ResultScreen(
                navController = navController,
                score = score
            )
        }

        composable(Screen.UNDERAGE_RESULT) {
            UnderageResultScreen(
                navController = navController,
                score = 0,
                aiPrediction = "",
                onFinish = {
                    navController.navigate(Screen.DASHBOARD) {
                        popUpTo(Screen.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        // Wellness Tools
        composable(Screen.BREATHING) {
            BreathingScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SOUNDS) {
            SoundScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.JOURNAL) {
            JournalScreen(navController = navController)
        }

        composable(Screen.MOOD_TRACKER) {
            MoodTrackerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.GROUNDING) {
            GroundingScreen(onBack = { navController.popBackStack() })
        }

        // Popups
        composable(Screen.PRIVACY_POLICY) {
            PrivacyPolicyPopup(onDismiss = { navController.popBackStack() })
        }

        composable(Screen.TERMS_CONDITIONS) {
            TermsAndConditionsPopup(onDismiss = { navController.popBackStack() })
        }
    }
}