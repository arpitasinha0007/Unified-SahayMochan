// ColorPalette.kt
package com.example.unifiedapp.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MochanColors {

    // ============ PRIMARY BRAND COLORS ============
    // Purple Family - Main brand color
    val PurplePrimary = Color(0xFF8B5CF6)      // Medium purple
    val PurpleSecondary = Color(0xFFA78BFA)    // Light purple
    val PurpleDark = Color(0xFF7C3AED)          // Dark purple
    val PurpleLight = Color(0xFFC4B5FD)         // Very light purple
    val PurpleVibrant = Color(0xFF7C3AED)       // Vibrant purple
    val PurpleMedium = Color(0xFF8B5CF6)        // Medium purple
    val PurpleSoft = Color(0xFFA78BFA)          // Soft purple



    // ============ GRADIENTS ============
    // Primary Purple Gradients
    val PrimaryPurpleGradient = Brush.linearGradient(
        colors = listOf(PurpleDark, PurpleMedium, PurpleSoft)
    )

    val GradientPurple = Brush.linearGradient(
        colors = listOf(PurplePrimary, PurpleSecondary)
    )

    val GradientPurpleDark = Brush.linearGradient(
        colors = listOf(PurpleDark, PurplePrimary)
    )

    val GradientPurpleLight = Brush.linearGradient(
        colors = listOf(PurpleSecondary, PurpleLight)
    )

    val PurpleLightGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFC084FC), Color(0xFFE9D5FF))
    )

    // Secondary Gradients - Wellness & Features
    val BlueCyanGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))
    )

    val OrangePinkGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFB923C), Color(0xFFF472B6))
    )

    val GreenTealGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
    )

    val GradientBlueCyan = Brush.linearGradient(
        colors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE))
    )

    val GradientPurplePink = Brush.linearGradient(
        colors = listOf(Color(0xFFA855F7), Color(0xFFEC4899))
    )

    val GradientGreenTeal = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
    )

    val GradientOrangeRed = Brush.linearGradient(
        colors = listOf(Color(0xFFF97316), Color(0xFFEF4444))
    )

    // Assessment Gradients
    val GradientStart = Color(0xFFFF385C)       // Bright pinkish-red
    val GradientMid = Color(0xFFFF5E3A)         // Red-orange
    val GradientEnd = Color(0xFFFF9345)         // Golden orange

    // ============ BACKGROUND COLORS ============
    // Purple Backgrounds
    val SoftPurpleBg = Color(0xFFF5F3FF)        // Very light purple background
    val SoftPurpleBorder = Color(0xFFE0E7FF)    // Light purple border
    val SoftPurpleHover = Color(0xFFEDE9FE)     // Slightly darker for hover

    // Other Backgrounds
    val SoftBlueBg = Color(0xFFEFF6FF)          // Light blue background
    val SoftGreenBg = Color(0xFFECFDF5)         // Light green background
    val SoftCyanBg = Color(0xFFE0F2FE)          // Soft cyan background
    val SoftPinkBg = Color(0xFFFFF1F2)           // Light pink background
    val SoftOrangeBg = Color(0xFFFFFBEB)         // Light orange background

    // Glass Effects
    val GlassWhite = Color.White.copy(alpha = 0.4f)
    val GlassBorder = Color.White.copy(alpha = 0.3f)
    val CardWhite = Color.White.copy(alpha = 0.95f)
    val MilkyWhite = Color.White.copy(alpha = 0.4f)

    // ============ TEXT COLORS ============
    val TextPrimary = Color(0xFF1D2335)             // Deep indigo/dark gray
    //val TextPrimary = Color(0xFF1F2937)          // Dark gray (Slate 800)
    val TextSecondary = Color(0xFF4B5563)        // Medium gray (Slate 600)
    val TextSoft = Color(0xFF4B5563)             // Medium gray
    val TextTertiary = Color(0xFF6B7280)         // Light gray (Slate 500)
    val TextLight = Color(0xFF9CA3AF)            // Very light gray (Slate 400)
    val TextMuted = Color(0xFF94A3B8)            // Muted gray (Slate 400)

    // Legacy text colors (for compatibility)
    val ColorTextPrimary = TextPrimary
    val ColorTextSecondary = TextSecondary
    val ColorTextTertiary = TextTertiary

    // ============ UI COLORS ============
    val ColorBorder = Color(0xFFE2E8F0)          // Light gray border (Slate 200)
    val ColorDivider = Color(0xFFE5E7EB)         // Divider color (Gray 200)
    val ColorCardBg = Color.White                  // Pure white for cards
    val SurfaceWhite = Color.White                 // White surface
    val ColorSurface = Color(0xFFF8FAFC)          // Slate 50

    // ============ STATUS/SEVERITY COLORS ============
    // Success/Positive
    val ColorSuccess = Color(0xFF10B981)          // Green
    val SuccessLight = Color(0xFFECFDF5)           // Light green background

    // Warning/Moderate
    val ColorWarning = Color(0xFFF59E0B)           // Amber/Orange
    val WarningLight = Color(0xFFFFFBEB)            // Light orange background

    // Error/Severe
    val ColorError = Color(0xFFEF4444)              // Red
    val ErrorLight = Color(0xFFFFF1F2)               // Light red background

    // Severity specific
    val MildColor = Color(0xFF10B981)                // Green
    val MildSecondary = Color(0xFF34D399)            // Light Green
    val MildLightColor = Color(0xFFECFDF5)            // Light Green

    val ModerateColor = Color(0xFFF59E0B)            // Orange
    val ModerateSecondary = Color(0xFFFBBF24)        // Light Orange
    val ModerateLightColor = Color(0xFFFFFBEB)       // Light Orange

    val SevereColor = Color(0xFFEF4444)              // Red
    val SevereSecondary = Color(0xFFF87171)          // Light Red
    val SevereLightColor = Color(0xFFFFF1F2)         // Light Red

    // ============ SEVERITY GRADIENTS ============
    val MildGradient = Brush.linearGradient(
        colors = listOf(MildColor, MildSecondary)
    )

    val ModerateGradient = Brush.linearGradient(
        colors = listOf(ModerateColor, ModerateSecondary)
    )

    val SevereGradient = Brush.linearGradient(
        colors = listOf(SevereColor, SevereSecondary)
    )

    // ============ MOOD TRACKER COLORS ============
    object MoodColors {
        val Happy = Color(0xFF4ADE80)               // Green
        val Neutral = Color(0xFF22D3EE)              // Cyan
        val Sad = Color(0xFFFB7185)                  // Pink/Red
        val Ecstatic = Color(0xFF8B5CF6)              // Purple
        val Content = Color(0xFF60A5FA)               // Blue
        val Tired = Color(0xFFA78BFA)                  // Light purple
        val Anxious = Color(0xFFFBBF24)                // Yellow
        val Angry = Color(0xFFF87171)                  // Light red
    }

    // ============ WELLNESS ICON GRADIENTS ============
    object WellnessGradients {
        val Breathing = Brush.linearGradient(
            colors = listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))
        )
        val Sounds = Brush.linearGradient(
            colors = listOf(Color(0xFFA855F7), Color(0xFFEC4899))
        )
        val Mood = Brush.linearGradient(
            colors = listOf(Color(0xFF34D399), Color(0xFF99F6E4))
        )
        val Journal = Brush.linearGradient(
            colors = listOf(Color(0xFFAB47BC), Color(0xFF7E57C2))
        )
        val Heart = Brush.linearGradient(
            colors = listOf(Color(0xFF29B6F6), Color(0xFF26C6DA))
        )
    }

    // ============ SOUND SCREEN COLORS ============
    object SoundColors {
        val Rain = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE))
        val Ocean = listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))
        val Forest = listOf(Color(0xFF4ADE80), Color(0xFF10B981))
        val Birds = listOf(Color(0xFFFACC15), Color(0xFFFB923C))
        val Thunder = listOf(Color(0xFFC084FC), Color(0xFFF472B6))
        val Piano = listOf(Color(0xFFF472B6), Color(0xFFFB923C))
    }

    // ============ JOURNAL SCREEN GRADIENTS ============
    val CreamGradient = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFF8F8FA))
    )

    val LavenderCreamGradient = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFF5E6FF))
    )

    val WarmWhiteGradient = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFFFFAF0))
    )

    // ============ CRISIS & EMERGENCY ============
    val CrisisRed = Color(0xFFEF4444)
    val CrisisLightBg = Color(0xFFFEF2F2)
    val CrisisBorder = Color(0xFFFECACA)

    // ============ UTILITY FUNCTIONS ============
    fun parseMoodColor(colorHex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Black
        }
    }
}