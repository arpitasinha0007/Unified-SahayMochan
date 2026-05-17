package com.example.unifiedapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mochan Color Theme (Purple) - SINGLE SOURCE OF TRUTH
val MochanPurplePrimary = Color(0xFF8B5CF6)
val MochanPurpleSecondary = Color(0xFFA78BFA)
val MochanPurpleDark = Color(0xFF7C3AED)
val MochanPurpleLight = Color(0xFFC4B5FD)
val MochanPurpleUltraLight = Color(0xFFF5F3FF)
val MochanTextPrimary = Color(0xFF1F2937)
val MochanTextSecondary = Color(0xFF6B7280)

val MochanGradient = Brush.verticalGradient(
    colors = listOf(MochanPurpleUltraLight, Color.White)
)