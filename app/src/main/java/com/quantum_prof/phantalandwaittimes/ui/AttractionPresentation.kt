package com.quantum_prof.phantalandwaittimes.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.AttractionStatus
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.theme.appAccents

/**
 * Presentation helpers shared by the list and the alert views, kept in one place so the icon,
 * colour and label for an attraction cannot drift apart between screens.
 */

private val COASTER_CODES = setOf("3136", "3137", "3532", "3235", "3630", "3539", "3733")
private val WATER_RIDE_CODES = setOf("3238", "3139", "3735")
private val FAMILY_RIDE_CODES = setOf("34", "3431", "3432")
private val CHILD_RIDE_CODES = setOf(
    "31", "32", "33", "35", "3632", "3633", "3634", "3635", "3638", "3730", "3731", "3732"
)

@DrawableRes
fun attractionIconResId(code: String): Int = when (code) {
    in COASTER_CODES -> R.drawable.ic_coaster
    in WATER_RIDE_CODES -> R.drawable.ic_waterride
    in CHILD_RIDE_CODES -> R.drawable.ic_childride
    in FAMILY_RIDE_CODES -> R.drawable.ic_default_ride
    else -> R.drawable.ic_default_ride
}

/**
 * Colour bucket for a wait time. Closed attractions are always neutral — their reported wait
 * time carries no meaning.
 */
@Composable
@ReadOnlyComposable
fun waitTimeColor(attraction: AttractionWaitTime): Color {
    val accents = appAccents
    if (!attraction.isOpen) return accents.waitClosed
    return when (attraction.displayWaitTime) {
        in 0..15 -> accents.waitShort
        in 16..30 -> accents.waitMedium
        in 31..60 -> accents.waitLong
        else -> accents.waitVeryLong
    }
}

/** The badge text: the wait in minutes when open, the reason it is unavailable otherwise. */
@Composable
@ReadOnlyComposable
fun waitTimeLabel(attraction: AttractionWaitTime): String = when (attraction.attractionStatus) {
    AttractionStatus.OPENED,
    AttractionStatus.VIRTUAL_QUEUE ->
        stringResource(R.string.wait_minutes, attraction.displayWaitTime)

    AttractionStatus.CLOSED -> stringResource(R.string.status_closed)
    AttractionStatus.MAINTENANCE -> stringResource(R.string.status_maintenance)
    AttractionStatus.UNKNOWN -> stringResource(R.string.status_unknown)
}
