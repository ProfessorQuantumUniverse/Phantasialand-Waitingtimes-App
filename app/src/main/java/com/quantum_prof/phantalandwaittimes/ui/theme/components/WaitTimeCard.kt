package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.attractionIconResId
import com.quantum_prof.phantalandwaittimes.ui.theme.appAccents
import com.quantum_prof.phantalandwaittimes.ui.waitTimeColor
import com.quantum_prof.phantalandwaittimes.ui.waitTimeLabel

/**
 * One attraction row: icon, name, wait-time badge and the favourite / alert actions.
 *
 * Favourites are marked with a static accent border plus a one-shot scale rather than a
 * permanently animating glow, so a list full of favourites no longer animates indefinitely.
 */
@Composable
fun WaitTimeCard(
    attraction: AttractionWaitTime,
    isFavorite: Boolean,
    hasAlert: Boolean,
    onFavoriteToggle: () -> Unit,
    onAlertClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Set briefly after arriving here from a notification, to point out the right row. */
    isHighlighted: Boolean = false
) {
    val accentColor = waitTimeColor(attraction)
    val favoriteColor = appAccents.favorite
    val highlightColor = MaterialTheme.colorScheme.primary

    // The highlight fades in and out rather than snapping, so a deep link reads as a gesture
    // towards the row instead of a flash.
    val highlightStrength by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "deepLinkHighlight"
    )

    val borderColor = when {
        highlightStrength > 0f -> highlightColor.copy(alpha = highlightStrength)
        isFavorite -> favoriteColor.copy(alpha = 0.7f)
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .favoriteEmphasis(isFavorite),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = lerp(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                highlightColor.copy(alpha = 0.20f),
                highlightStrength
            )
        ),
        border = borderColor?.let { BorderStroke(1.5.dp, it) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFavorite || highlightStrength > 0f) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttractionIcon(code = attraction.code, tint = accentColor)

            Spacer(Modifier.width(12.dp))

            Text(
                text = attraction.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            WaitTimeBadge(attraction = attraction, color = accentColor)

            IconButton(onClick = onAlertClick) {
                Icon(
                    imageVector = if (hasAlert) {
                        Icons.Default.EditNotifications
                    } else {
                        Icons.Default.AddAlert
                    },
                    contentDescription = stringResource(
                        if (hasAlert) R.string.alert_edit else R.string.alert_add
                    ),
                    tint = if (hasAlert) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(20.dp)
                        // The only continuously animating element, and only when an alert is armed.
                        .then(
                            if (hasAlert) {
                                Modifier.pulsingGlow(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                    ),
                    tint = if (isFavorite) {
                        favoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AttractionIcon(
    code: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        // Drawn as an Icon rather than an Image so the artwork is tinted to the theme: the raw
        // vectors are near-white and were effectively invisible on light surfaces.
        Icon(
            painter = painterResource(id = attractionIconResId(code)),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun WaitTimeBadge(
    attraction: AttractionWaitTime,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = waitTimeLabel(attraction),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = color
        )
    }
}
