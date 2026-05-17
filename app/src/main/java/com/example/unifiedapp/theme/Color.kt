package com.example.unifiedapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ============================================================================
// PRIMARY COLOR PALETTE - Single source of truth for all colors
// ============================================================================

// Primary Purple
val PurplePrimary = Color(0xFF8B5CF6)
val PurpleSecondary = Color(0xFFA78BFA)
val PurpleDark = Color(0xFF7C3AED)
val PurpleLight = Color(0xFFC4B5FD)
val PurpleUltraLight = Color(0xFFF5F3FF)

// Gradients using primary purple
val GradientPurple = Brush.linearGradient(colors = listOf(PurplePrimary, PurpleSecondary))
val GradientPurpleDark = Brush.linearGradient(colors = listOf(PurpleDark, PurplePrimary))
val GradientPurpleLight = Brush.linearGradient(colors = listOf(PurpleSecondary, PurpleLight))

// ============================================================================
// ACCENT COLORS
// ============================================================================

// Blue family
val BlueBright = Color(0xFF60A5FA)
val BlueCyan = Color(0xFF22D3EE)
val GradientBlueCyan = Brush.linearGradient(colors = listOf(BlueBright, BlueCyan))

// Pink/Red family
val PinkBright = Color(0xFFEC4899)
val CoralStart = Color(0xFFFF385C)
val CoralMid = Color(0xFFFF5E3A)
val CoralEnd = Color(0xFFFF9345)
val GradientCoral = Brush.linearGradient(colors = listOf(CoralStart, CoralMid, CoralEnd))
val GradientPurplePink = Brush.linearGradient(colors = listOf(PurplePrimary, PinkBright))

// Green family
val GreenMint = Color(0xFF10B981)
val GreenTeal = Color(0xFF14B8A6)
val GradientGreenTeal = Brush.linearGradient(colors = listOf(GreenMint, GreenTeal))

// Orange family
val OrangeWarm = Color(0xFFF97316)
val OrangeRed = Color(0xFFEF4444)
val GradientOrangeRed = Brush.linearGradient(colors = listOf(OrangeWarm, OrangeRed))

// Sunset Gradient
val GradientSunset = Brush.linearGradient(colors = listOf(Color(0xFFFF8A80), Color(0xFFFFB74D)))

// ============================================================================
// SEVERITY COLORS
// ============================================================================

val MildColor = GreenMint
val MildLightColor = Color(0xFFECFDF5)

val ModerateColor = Color(0xFFF59E0B)
val ModerateLightColor = Color(0xFFFFFBEB)

val SevereColor = OrangeRed
val SevereLightColor = Color(0xFFFFF1F2)

// ============================================================================
// TEXT COLORS
// ============================================================================

val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF4B5563)
val TextTertiary = Color(0xFF6B7280)
val TextMuted = Color(0xFF9CA3AF)

// ============================================================================
// BACKGROUND & SURFACE COLORS
// ============================================================================

val SurfaceWhite = Color.White
val SurfaceOffWhite = Color(0xFFF8FAFC)
val SurfaceCard = Color.White
val GlassWhite = Color.White.copy(alpha = 0.4f)
val GlassWhiteHeavy = Color.White.copy(alpha = 0.75f)

// ============================================================================
// BORDER & DIVIDER COLORS
// ============================================================================

val BorderLight = Color(0xFFE5E7EB)

// ============================================================================
// UI COLORS (Legacy compatibility)
// ============================================================================

val SoftPurpleBg = Color(0xFFF5F3FF)
val SoftPurpleBorder = Color(0xFFE0E7FF)
val ColorTextPrimary = TextPrimary
val ColorTextSecondary = TextSecondary
val ColorTextTertiary = TextTertiary
val ColorBorder = BorderLight
val ColorCardBg = SurfaceCard
val ColorSuccess = GreenMint
val ColorError = SevereColor

// ============================================================================
// GRADIENTS FOR WELLNESS, SOUND, MOOD TRACKER
// ============================================================================

val MoodHappyGradient = Brush.linearGradient(colors = listOf(Color(0xFF34D399), Color(0xFF99F6E4)))
val MoodNeutralGradient = Brush.linearGradient(colors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE)))
val MoodSadGradient = Brush.linearGradient(colors = listOf(Color(0xFFFB7185), Color(0xFFF87171)))

val WellnessBlueGradient = Brush.linearGradient(colors = listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF)))
val WellnessPurplePinkGradient = Brush.linearGradient(colors = listOf(Color(0xFFA855F7), Color(0xFFEC4899)))
val WellnessGreenGradient = Brush.linearGradient(colors = listOf(Color(0xFF34D399), Color(0xFF99F6E4)))

val SoundRainGradient = Brush.linearGradient(colors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE)))
val SoundOceanGradient = Brush.linearGradient(colors = listOf(Color(0xFF22D3EE), Color(0xFF3B82F6)))
val SoundForestGradient = Brush.linearGradient(colors = listOf(Color(0xFF4ADE80), Color(0xFF10B981)))
val SoundBirdsGradient = Brush.linearGradient(colors = listOf(Color(0xFFFACC15), Color(0xFFFB923C)))
val SoundThunderGradient = Brush.linearGradient(colors = listOf(Color(0xFFC084FC), Color(0xFFF472B6)))
val SoundPianoGradient = Brush.linearGradient(colors = listOf(Color(0xFFF472B6), Color(0xFFFB923C)))

val BreathingGradient = Brush.linearGradient(colors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE)))
val BreathingCircleBg = Color(0xFFDBEAFE)

val JournalPurpleGradient = Brush.linearGradient(colors = listOf(Color(0xFFAB47BC), Color(0xFF7E57C2)))
val JournalLightPurple = Color(0xFFF3E8FF)
val JournalLavender = Color(0xFFF5E6FF)

val TermsGradient = Brush.horizontalGradient(colors = listOf(CoralStart, CoralMid, CoralEnd))

val GradientStart = CoralStart
val GradientMid = CoralMid
val GradientEnd = CoralEnd

// Legacy Material colors (keep if needed)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)


val AppSageBackground = SoftPurpleBg
val ButtonNextGreen = PurplePrimary
val CardCream = SurfaceWhite
val TextDark = TextPrimary
val TextWhite = Color.White

// Sahay color aliases for compatibility
val BackgroundSage = SoftPurpleBg
val SoftApricot = Color(0xFFFFE0C0)  // Soft peach color
val MistyBlue = Color(0xFFD4E4F0)     // Soft blue
val SageMint = Color(0xFFD0ECD0)      // Light mint
val PaleMint = Color(0xFFE0F0E0)      // Pale mint

// Also add these if missing
val SageLight = PurpleUltraLight
val SageDark = TextPrimary
val SagePrimary = PurplePrimary
val SageAccent = PurpleSecondary
val SageMedium = PurpleLight
val WhiteSoft = SurfaceWhite
val SoftCoral = Color(0xFFFFB5A0)


// ============================================================================
// ADDITIONAL COLORS FOR PRIVACY POLICY, TERMS, AND WEALTH SCREEN
// ============================================================================

// Colors for PrivacyPolicyPopup
val SoftRose = Color(0xFFF7D9E0)      // Soft pink/rose
val SoftLilac = Color(0xFFE6E0F0)     // Soft lilac/purple
val BlushPink = Color(0xFFFADADD)     // Blush pink
val CloudBlue = Color(0xFFD8ECF0)     // Soft cloud blue

// Colors for TermsAndConditionsPopup
val Seafoam = Color(0xFFD8ECE0)       // Soft seafoam green
val PowderBlue = Color(0xFFDAEAF0)    // Powder blue
val LavenderMist = Color(0xFFE2E0F0)  // Lavender mist
val DustyRose = Color(0xFFE9D4D0)     // Dusty rose
val Butter = Color(0xFFF9F0D6)        // Soft butter yellow
val PeachSorbet = Color(0xFFFFE5D9)   // Peach sorbet

// Colors for WealthScreen
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        PurpleUltraLight,
        SurfaceWhite
    )
)

// Also add these if they're used elsewhere
val SoftPeach = Color(0xFFFFE5D9)
val SageGradient = GradientPurple



// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

fun parseColor(hexColor: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        TextPrimary
    }
}

fun gradientFromColors(color1: Color, color2: Color): Brush {
    return Brush.linearGradient(colors = listOf(color1, color2))
}

fun radialGlow(color: Color, alpha: Float = 0.4f): Brush {
    return Brush.radialGradient(
        colors = listOf(color.copy(alpha = alpha), Color.Transparent)
    )
}

fun getSeverityColor(score: Int): Color {
    return when {
        score <= 9 -> MildColor
        score <= 14 -> ModerateColor
        else -> SevereColor
    }
}

fun getSeverityBackgroundColor(score: Int): Color {
    return when {
        score <= 9 -> MildLightColor
        score <= 14 -> ModerateLightColor
        else -> SevereLightColor
    }
}