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

fun NavGraphBuilder.mochanNavGraph(
    navController: NavHostController
) {
    // Add composable routes to the EXISTING navController
    composable(MochanScreen.DASHBOARD) {
        MochanDashboardScreen(navController)
    }

    composable(MochanScreen.CONSENT) {
        MochanConsentScreen(
            onAccept = {
                navController.navigate(MochanScreen.ASSESSMENT) {
                    popUpTo(MochanScreen.CONSENT) { inclusive = true }
                }
            },
            onDecline = { navController.popBackStack() }
        )
    }

    composable(MochanScreen.ASSESSMENT) {
        MochanAssessmentRoute(navController)
    }

    composable(
        route = MochanScreen.RESULT,
        arguments = listOf(navArgument("score") { type = NavType.IntType })
    ) { backStackEntry ->
        val score = backStackEntry.arguments?.getInt("score") ?: 0
        ResultScreen(
            navController = navController,
            score = score
        )
    }

    composable(MochanScreen.WELLNESS) {
        WellnessScreen(navController = navController)
    }

    composable(MochanScreen.BREATHING) {
        BreathingScreen(onBack = { navController.popBackStack() })
    }

    composable(MochanScreen.SOUNDS) {
        SoundScreen(onBack = { navController.popBackStack() })
    }

    composable(MochanScreen.JOURNAL) {
        JournalScreen(navController = navController)
    }

    composable(MochanScreen.MOOD_TRACKER) {
        MoodTrackerScreen(onBack = { navController.popBackStack() })
    }

    composable(MochanScreen.GROUNDING) {
        GroundingScreen(onBack = { navController.popBackStack() })
    }

    composable(MochanScreen.PROFILE) {
        ProfileScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable(MochanScreen.PRIVACY_DATA) {
        PrivacyDataScreen(navController = navController)
    }

    composable(MochanScreen.ASSESSMENT_HISTORY) {
        AssessmentHistoryScreen(navController = navController)
    }
}

@Composable
fun MochanAssessmentRoute(navController: NavHostController) {
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
        assessmentType = "depression"
    )
}

object MochanScreen {
    const val DASHBOARD = "mochan_dashboard"
    const val CONSENT = "mochan_consent"
    const val ASSESSMENT = "mochan_assessment"
    const val RESULT = "mochan_result/{score}"
    const val WELLNESS = "mochan_wellness"
    const val PROFILE = "mochan_profile"
    const val BREATHING = "mochan_breathing"
    const val SOUNDS = "mochan_sounds"
    const val JOURNAL = "mochan_journal"
    const val MOOD_TRACKER = "mochan_mood_tracker"
    const val GROUNDING = "mochan_grounding"
    const val PRIVACY_DATA = "mochan_privacy_data"
    const val ASSESSMENT_HISTORY = "mochan_assessment_history"
}