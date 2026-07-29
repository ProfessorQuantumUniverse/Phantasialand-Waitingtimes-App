package com.quantum_prof.phantalandwaittimes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

/**
 * Colours that are meaningful to this app but have no slot in [MaterialTheme.colorScheme].
 * Provided through a composition local so composables can read them the same way.
 */
data class AppAccentColors(
    val waitShort: Color,
    val waitMedium: Color,
    val waitLong: Color,
    val waitVeryLong: Color,
    val waitClosed: Color,
    val favorite: Color,
    val scrim: Color
)

private val LightAccents = AppAccentColors(
    waitShort = WaitShortLight,
    waitMedium = WaitMediumLight,
    waitLong = WaitLongLight,
    waitVeryLong = WaitVeryLongLight,
    waitClosed = WaitClosedLight,
    favorite = FavoriteLight,
    scrim = ScrimLight
)

private val DarkAccents = AppAccentColors(
    waitShort = WaitShortDark,
    waitMedium = WaitMediumDark,
    waitLong = WaitLongDark,
    waitVeryLong = WaitVeryLongDark,
    waitClosed = WaitClosedDark,
    favorite = FavoriteDark,
    scrim = ScrimDark
)

val LocalAppAccentColors: ProvidableCompositionLocal<AppAccentColors> =
    staticCompositionLocalOf { LightAccents }

/** Shorthand for the app-specific accent colours. */
val appAccents: AppAccentColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppAccentColors.current

@Composable
fun PhantasialandWaitTimesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Material You. Off by default so the wait-time colour coding stays predictable. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    // System bar handling lives in MainActivity via enableEdgeToEdge(); the Window#statusBarColor
    // and #navigationBarColor properties this used to set are deprecated and ignored on API 35+.
    CompositionLocalProvider(
        LocalAppAccentColors provides if (darkTheme) DarkAccents else LightAccents
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
