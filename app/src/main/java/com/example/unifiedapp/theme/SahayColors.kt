package com.example.unifiedapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sahay Color Theme (Sage Green) - SINGLE SOURCE OF TRUTH
val SahaySageLight = Color(0xFFF1F7F3)
val SahaySageMedium = Color(0xFFD3E4D6)
val SahaySageAccent = Color(0xFF6B9071)
val SahayCharcoal = Color(0xFF3E4E42)
val SahayWhiteSoft = Color(0xFFFAFAFA)
val SahayMutedSlate = Color(0xFF5D6D66)

val SahayGradient = Brush.verticalGradient(
    colors = listOf(SahaySageLight, SahayWhiteSoft)
)