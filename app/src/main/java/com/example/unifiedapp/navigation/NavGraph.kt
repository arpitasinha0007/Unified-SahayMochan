package com.example.unifiedapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.unifiedapp.screens.*
import com.example.unifiedapp.ui.auth.UnifiedAuthScreen
import com.example.unifiedapp.ui.auth.UserProfile
import com.example.unifiedapp.ui.views.CameraViewModel

object Screen {
    const val LAUNCHER = "launcher"
    const val DASHBOARD = "dashboard"
    const val PROFILE = "profile"
    const val WELLNESS = "wellness"
    const val PRIVACY_DATA = "privacy_data"
    const val ASSESSMENT_HISTORY = "assessment_history"
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONSENT = "consent"
    const val ASSESSMENT = "assessment"
    const val RESULT = "result/{score}"
    const val UNDERAGE_RESULT = "underage_result"
    const val SAHAY_CONSENT = "sahay_consent"
    const val SAHAY_ASSESSMENT = "sahay_assessment"
    const val SAHAY_RESULT = "sahay_result/{score}"
    const val MOCHAN_CONSENT = "mochan_consent"
    const val MOCHAN_ASSESSMENT = "mochan_assessment"
    const val MOCHAN_RESULT = "mochan_result/{score}"
    const val BREATHING = "breathing"
    const val SOUNDS = "sounds"
    const val JOURNAL = "journal"
    const val MOOD_TRACKER = "mood_tracker"
    const val GROUNDING = "grounding"
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"
}

@Composable
fun UnifiedNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.LAUNCHER
) {
    val context = LocalContext.current

    val cameraViewModel = remember { CameraViewModel() }
    LaunchedEffect(Unit) {
        cameraViewModel.init(context.applicationContext)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LAUNCHER) {
            LauncherScreen(navController = navController)
        }

        composable(Screen.AUTH) {
            UnifiedAuthScreen(
                navController = navController,
                onLoginSuccess = { userProfile: UserProfile ->
                    navController.navigate(Screen.DASHBOARD) {
                        popUpTo(Screen.LAUNCHER) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                onLogout = { }
            )
        }

        // Sahay Assessment Flow
        composable(Screen.SAHAY_CONSENT) {
            ConsentScreen(
                navController = navController,   // ✅ FIXED
                onAccept = {
                    navController.navigate(Screen.SAHAY_ASSESSMENT) {
                        popUpTo(Screen.SAHAY_CONSENT) { inclusive = true }
                    }
                },
                onDecline = { navController.popBackStack() }
            )
        }
        composable(Screen.SAHAY_ASSESSMENT) {
            AssessmentQuestionnairesScreen(
                navController = navController,
                cameraViewModel = cameraViewModel,
                assessmentType = "anxiety"
            )
        }
        composable(
            route = Screen.SAHAY_RESULT,
            arguments = listOf(navArgument("score") { type = NavType.IntType })
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            ResultScreen(
                navController = navController,
                score = score,
                assessmentType = "anxiety"
            )
        }

        // Mochan Assessment Flow
        composable(Screen.MOCHAN_CONSENT) {
            ConsentScreen(
                navController = navController,   // ✅ FIXED
                onAccept = {
                    navController.navigate(Screen.MOCHAN_ASSESSMENT) {
                        popUpTo(Screen.MOCHAN_CONSENT) { inclusive = true }
                    }
                },
                onDecline = { navController.popBackStack() }
            )
        }
        composable(Screen.MOCHAN_ASSESSMENT) {
            AssessmentQuestionnairesScreen(
                navController = navController,
                cameraViewModel = cameraViewModel,
                assessmentType = "depression"
            )
        }
        composable(
            route = Screen.MOCHAN_RESULT,
            arguments = listOf(navArgument("score") { type = NavType.IntType })
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            ResultScreen(
                navController = navController,
                score = score,
                assessmentType = "depression"
            )
        }

        // Underage Result Screen
        composable(
            route = Screen.UNDERAGE_RESULT,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType; defaultValue = 0 },
                navArgument("aiPrediction") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val aiPrediction = backStackEntry.arguments?.getString("aiPrediction") ?: ""
            UnderageResultScreen(
                navController = navController,
                score = score,
                aiPrediction = aiPrediction,
                onFinish = { navController.popBackStack(Screen.LAUNCHER, false) }
            )
        }

        // Profile & Privacy
        composable(Screen.PROFILE) {
            ProfileScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PRIVACY_DATA) {
            PrivacyDataScreen(navController = navController)
        }
        composable(Screen.ASSESSMENT_HISTORY) {
            AssessmentHistoryScreen(navController = navController)
        }

        // Wellness Tools
        composable(Screen.WELLNESS) {
            WellnessScreen(navController = navController)
        }
        composable(Screen.BREATHING) {
            BreathingScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SOUNDS) {
            SoundScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.JOURNAL) {
            JournalScreen().JournalContent(navController = navController)
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