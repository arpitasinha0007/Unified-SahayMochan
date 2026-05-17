package com.example.unifiedapp.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Assessment : Screen("assessment")
    object Results : Screen("results")
    object Relax : Screen("relax")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object Menu : Screen("menu")
    object AssessmentHistory : Screen("assessment_history")
}
