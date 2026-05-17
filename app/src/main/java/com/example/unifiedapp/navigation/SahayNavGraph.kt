package com.example.unifiedapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.unifiedapp.screens.*
import com.example.unifiedapp.ui.views.CameraViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

fun NavGraphBuilder.sahayNavGraph(
    navController: NavHostController
) {
    // Add composable routes to the EXISTING navController
    composable(SahayScreen.DASHBOARD) {
        SahayDashboardScreen(navController)
    }

    composable(SahayScreen.CONSENT) {
        SahayConsentScreen(
            onAccept = {
                navController.navigate(SahayScreen.ASSESSMENT) {
                    popUpTo(SahayScreen.CONSENT) { inclusive = true }
                }
            },
            onDecline = { navController.popBackStack() }
        )
    }

    composable(SahayScreen.ASSESSMENT) {
        SahayAssessmentRoute(navController)
    }

    composable(
        route = SahayScreen.RESULT,
        arguments = listOf(navArgument("score") { type = NavType.IntType })
    ) { backStackEntry ->
        val score = backStackEntry.arguments?.getInt("score") ?: 0
        ResultScreen(
            navController = navController,
            score = score
        )
    }

    composable(SahayScreen.WELLNESS) {
        WellnessScreen(navController = navController)
    }

    composable(SahayScreen.BREATHING) {
        BreathingScreen(onBack = { navController.popBackStack() })
    }

    composable(SahayScreen.SOUNDS) {
        SoundScreen(onBack = { navController.popBackStack() })
    }

    composable(SahayScreen.JOURNAL) {
        JournalScreen(navController = navController)
    }

    composable(SahayScreen.MOOD_TRACKER) {
        MoodTrackerScreen(onBack = { navController.popBackStack() })
    }

    composable(SahayScreen.GROUNDING) {
        GroundingScreen(onBack = { navController.popBackStack() })
    }

    composable(SahayScreen.PROFILE) {
        ProfileScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable(SahayScreen.PRIVACY_DATA) {
        PrivacyDataScreen(navController = navController)
    }

    composable(SahayScreen.ASSESSMENT_HISTORY) {
        AssessmentHistoryScreen(navController = navController)
    }
}

@Composable
fun SahayAssessmentRoute(navController: NavHostController) {
    val context = LocalContext.current
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
    AssessmentQuestionnairesScreen(
        navController = navController,
        cameraViewModel = cameraViewModel,
        assessmentType = "anxiety"
    )
}

object SahayScreen {
    const val DASHBOARD = "sahay_dashboard"
    const val CONSENT = "sahay_consent"
    const val ASSESSMENT = "sahay_assessment"
    const val RESULT = "sahay_result/{score}"
    const val WELLNESS = "sahay_wellness"
    const val PROFILE = "sahay_profile"
    const val BREATHING = "sahay_breathing"
    const val SOUNDS = "sahay_sounds"
    const val JOURNAL = "sahay_journal"
    const val MOOD_TRACKER = "sahay_mood_tracker"
    const val GROUNDING = "sahay_grounding"
    const val PRIVACY_DATA = "sahay_privacy_data"
    const val ASSESSMENT_HISTORY = "sahay_assessment_history"
}