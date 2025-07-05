package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.theme.*

@Composable
fun WaitTimeCard(
    attraction: AttractionWaitTime,
    isFavorite: Boolean,
    hasAlert: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddAlert: () -> Unit,
    onRemoveAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen = attraction.status == "opened"

    val waitTimeColor = when {
        !isOpen -> WaitTimeClosed
        attraction.waitTimeMinutes <= 15 -> WaitTimeShort
        attraction.waitTimeMinutes <= 30 -> WaitTimeMedium
        attraction.waitTimeMinutes <= 60 -> WaitTimeLong
        else -> WaitTimeVeryLong
    }

    // Schlichte Material 3 Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hasAlert) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Einfacher Status-Indikator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(waitTimeColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Schlichter Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isOpen) {
                        when (attraction.waitTimeMinutes) {
                            0 -> "No Data"
                            else -> "${attraction.waitTimeMinutes} Min"
                        }
                    } else "Closed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = waitTimeColor
                )
            }

            // Material 3 IconButtons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { if (hasAlert) onRemoveAlert() else onAddAlert() }
                ) {
                    Icon(
                        imageVector = if (hasAlert) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (hasAlert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WaitTimeCompactCard(
    attraction: AttractionWaitTime,
    isFavorite: Boolean,
    hasAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val isOpen = attraction.status == "opened"

    val waitTimeColor = when {
        !isOpen -> WaitTimeClosed
        attraction.waitTimeMinutes <= 15 -> WaitTimeShort
        attraction.waitTimeMinutes <= 30 -> WaitTimeMedium
        attraction.waitTimeMinutes <= 60 -> WaitTimeLong
        else -> WaitTimeVeryLong
    }

    GlassMorphismCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
        elevated = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(waitTimeColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = attraction.name,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Indicators
            if (hasAlert) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Has Alert",
                    tint = AlertBorder,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = FavoriteActive,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (isOpen) {
                    when (attraction.waitTimeMinutes) {
                        0 -> "0"
                        else -> "${attraction.waitTimeMinutes}"
                    }
                } else "X",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = waitTimeColor
            )
        }
    }
}
