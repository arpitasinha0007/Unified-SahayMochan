package com.example.unifiedapp.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
// Import colors from your Color.kt (top‑level in the same package)
import com.example.unifiedapp.theme.PurplePrimary
import com.example.unifiedapp.theme.PurpleSecondary
import com.example.unifiedapp.theme.PinkBright
import com.example.unifiedapp.theme.TextDark
import com.example.unifiedapp.theme.SurfaceWhite
import com.example.unifiedapp.theme.ColorTextPrimary
import com.example.unifiedapp.theme.TextTertiary
import com.example.unifiedapp.theme.SoftPurpleBg
import com.example.unifiedapp.theme.TextPrimary

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = PinkBright,
    background = TextDark,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onBackground = TextTertiary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = PinkBright,
    background = SoftPurpleBg,   // This replaces the black background with soft purple
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onBackground = ColorTextPrimary,
    onSurface = ColorTextPrimary
)

@Composable
fun UnifiedAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}