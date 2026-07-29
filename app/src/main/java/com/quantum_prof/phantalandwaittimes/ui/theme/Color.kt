package com.quantum_prof.phantalandwaittimes.ui.theme

import androidx.compose.ui.graphics.Color

// -----------------------------------------------------------------------------------------------
// Brand palette
//
// Built around the park's green with an amber accent. Light and dark get their own tones instead
// of sharing one set — previously both schemes reused the same values, which left the light theme
// with dark-theme surfaces and poor contrast.
// -----------------------------------------------------------------------------------------------

// Light scheme
val LightPrimary = Color(0xFF2E7D46)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB6F0C4)
val LightOnPrimaryContainer = Color(0xFF00210E)

val LightSecondary = Color(0xFF0E7490)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFC5EDFB)
val LightOnSecondaryContainer = Color(0xFF002632)

val LightTertiary = Color(0xFF8A5A00)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDEA6)
val LightOnTertiaryContainer = Color(0xFF2B1700)

val LightBackground = Color(0xFFF6F7FB)
val LightOnBackground = Color(0xFF191C1A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF191C1A)
val LightSurfaceVariant = Color(0xFFE6EBE6)
val LightOnSurfaceVariant = Color(0xFF414942)
val LightOutline = Color(0xFF717972)
val LightOutlineVariant = Color(0xFFC1C9C1)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

// Dark scheme
val DarkPrimary = Color(0xFF7BD79A)
val DarkOnPrimary = Color(0xFF00391C)
val DarkPrimaryContainer = Color(0xFF19532F)
val DarkOnPrimaryContainer = Color(0xFFB6F0C4)

val DarkSecondary = Color(0xFF74D3ED)
val DarkOnSecondary = Color(0xFF003543)
val DarkSecondaryContainer = Color(0xFF004E60)
val DarkOnSecondaryContainer = Color(0xFFC5EDFB)

val DarkTertiary = Color(0xFFF6C01E)
val DarkOnTertiary = Color(0xFF3F2E00)
val DarkTertiaryContainer = Color(0xFF684300)
val DarkOnTertiaryContainer = Color(0xFFFFDEA6)

val DarkBackground = Color(0xFF0E1116)
val DarkOnBackground = Color(0xFFE1E3DF)
val DarkSurface = Color(0xFF171B21)
val DarkOnSurface = Color(0xFFE1E3DF)
val DarkSurfaceVariant = Color(0xFF262C33)
val DarkOnSurfaceVariant = Color(0xFFC0C9C2)
val DarkOutline = Color(0xFF8B938C)
val DarkOutlineVariant = Color(0xFF414942)

val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)

// -----------------------------------------------------------------------------------------------
// Semantic colours that are not part of the Material scheme
// -----------------------------------------------------------------------------------------------

/** Wait-time buckets. Each has a light and a dark variant so contrast holds in both themes. */
val WaitShortLight = Color(0xFF1B7F4B)
val WaitShortDark = Color(0xFF57D98D)
val WaitMediumLight = Color(0xFF9A6400)
val WaitMediumDark = Color(0xFFF0B429)
val WaitLongLight = Color(0xFFC2410C)
val WaitLongDark = Color(0xFFFB923C)
val WaitVeryLongLight = Color(0xFFB3261E)
val WaitVeryLongDark = Color(0xFFF87171)
val WaitClosedLight = Color(0xFF6B7280)
val WaitClosedDark = Color(0xFF9CA3AF)

/** Favourite indicator. */
val FavoriteLight = Color(0xFFC98A00)
val FavoriteDark = Color(0xFFFBBF24)

/** Scrim drawn over the park photo so cards and text stay legible. */
val ScrimLight = Color(0xFFF6F7FB)
val ScrimDark = Color(0xFF0E1116)
