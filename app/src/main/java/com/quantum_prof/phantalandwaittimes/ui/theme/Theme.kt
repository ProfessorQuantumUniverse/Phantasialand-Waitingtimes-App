package com.quantum_prof.phantalandwaittimes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enhanced Glasmorphism Color Schemes with Material 3
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = OnPrimary,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = OnSecondary,

    tertiary = Tertiary,
    onTertiary = OnPrimary,
    tertiaryContainer = Color(0xFF7C2D92),
    onTertiaryContainer = Color(0xFFFFD6FF),

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Primary,

    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    inversePrimary = Color(0xFFDDD6FE),

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),

    error = Color(0xFFEF4444),
    onError = OnPrimary,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECDD3),

    scrim = Color(0x80000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFE4D9FF),
    onPrimaryContainer = Color(0xFF3B0764),

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),

    tertiary = Tertiary,
    onTertiary = OnPrimary,
    tertiaryContainer = Color(0xFFFFE1F1),
    onTertiaryContainer = Color(0xFF831843),

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Primary,

    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Color(0xFFDDD6FE),

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),

    error = Color(0xFFDC2626),
    onError = OnPrimary,
    errorContainer = Color(0xFFFECDD3),
    onErrorContainer = Color(0xFF7F1D1D),

    scrim = Color(0x80000000)
)

@Composable
fun PhantasialandWaitTimesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false for consistent glassmorphism design
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status and navigation bars transparent for full glassmorphism effect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Set appropriate content colors
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}