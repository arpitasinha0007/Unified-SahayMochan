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
import com.example.unifiedapp.ui.auth.ClinicianRegisterScreen
import com.example.unifiedapp.ui.auth.UnifiedAuthScreen
import com.example.unifiedapp.ui.auth.UserProfile
import com.example.unifiedapp.ui.clinical.ClinicianDashboardScreen
import com.example.unifiedapp.ui.clinical.HamAQuestionnaireScreen
import com.example.unifiedapp.ui.clinical.HdrSQuestionnaireScreen
import com.example.unifiedapp.ui.clinical.ClinicalResultScreen
import com.example.unifiedapp.ui.views.CameraViewModel

object Screen {
    // Core navigation
    const val LAUNCHER = "launcher"
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"

    // Sahay flow
    const val SAHAY_CONSENT = "sahay_consent"
    const val SAHAY_ASSESSMENT = "sahay_assessment"
    const val SAHAY_RESULT = "sahay_result/{score}"

    // Mochan flow
    const val MOCHAN_CONSENT = "mochan_consent"
    const val MOCHAN_ASSESSMENT = "mochan_assessment"
    const val MOCHAN_RESULT = "mochan_result/{score}"

    // Underage result
    const val UNDERAGE_RESULT = "underage_result"

    // Profile & data
    const val PROFILE = "profile"
    const val PRIVACY_DATA = "privacy_data"
    const val ASSESSMENT_HISTORY = "assessment_history"

    // Wellness tools
    const val WELLNESS = "wellness"
    const val BREATHING = "breathing"
    const val SOUNDS = "sounds"
    const val JOURNAL = "journal"
    const val MOOD_TRACKER = "mood_tracker"
    const val GROUNDING = "grounding"

    // Popups
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"

    // Clinician routes
    const val CLINICIAN_DASHBOARD = "clinician_dashboard"
    const val CLINICIAN_REGISTER = "clinician_register"   // ✅ Added
    const val HAM_A_ASSESSMENT = "ham_a_assessment/{patientId}/{patientName}"
    const val HDRS_ASSESSMENT = "hdrs_assessment/{patientId}/{patientName}"
    const val CLINICAL_RESULT = "clinical_result/{score}/{severity}/{type}"
}

@Composable
fun UnifiedNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.AUTH
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
        // Launcher
        composable(Screen.LAUNCHER) {
            LauncherScreen(navController = navController)
        }

        // Authentication (login/signup)
        composable(Screen.AUTH) {
            UnifiedAuthScreen(
                navController = navController,
                onLoginSuccess = { userProfile: UserProfile ->
                    navController.navigate(Screen.DASHBOARD) {
                        popUpTo(Screen.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // ✅ Clinician Registration
        composable(Screen.CLINICIAN_REGISTER) {
            ClinicianRegisterScreen(navController = navController)
        }

        // Patient dashboard
        composable(Screen.DASHBOARD) {
            DashboardScreen(navController = navController, onLogout = { })
        }

        // ========== SAHAY (Anxiety) Flow ==========
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

        // ========== MOCHAN (Depression) Flow ==========
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

        // ========== Underage Result ==========
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

        // ========== Profile & Data Management ==========
        composable(Screen.PROFILE) {
            ProfileScreen(navController = navController, onBack = { navController.popBackStack() })
        }
        composable(Screen.PRIVACY_DATA) {
            PrivacyDataScreen(navController = navController)
        }
        composable(Screen.ASSESSMENT_HISTORY) {
            AssessmentHistoryScreen(navController = navController)
        }

        // ========== Wellness Tools ==========
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

        // ========== Legal Popups ==========
        composable(Screen.PRIVACY_POLICY) {
            PrivacyPolicyPopup(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.TERMS_CONDITIONS) {
            TermsAndConditionsPopup(onDismiss = { navController.popBackStack() })
        }

        // ========== CLINICIAN ROUTES ==========
        composable(Screen.CLINICIAN_DASHBOARD) {
            ClinicianDashboardScreen(navController = navController)
        }

        composable(
            route = Screen.HAM_A_ASSESSMENT,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: ""
            HamAQuestionnaireScreen(
                navController = navController,
                patientId = patientId,
                patientName = patientName
            )
        }

        composable(
            route = Screen.HDRS_ASSESSMENT,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: ""
            HdrSQuestionnaireScreen(
                navController = navController,
                patientId = patientId,
                patientName = patientName
            )
        }

        composable(
            route = Screen.CLINICAL_RESULT,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("severity") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val severity = backStackEntry.arguments?.getString("severity") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: ""
            ClinicalResultScreen(
                navController = navController,
                score = score,
                severity = severity,
                type = type
            )
        }
    }
}