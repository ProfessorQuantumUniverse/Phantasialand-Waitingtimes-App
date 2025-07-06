package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.getAttractionIconResId
import com.quantum_prof.phantalandwaittimes.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitTimeCard(
    attraction: AttractionWaitTime,
    isFavorite: Boolean,
    hasAlert: Boolean,
    onFavoriteToggle: () -> Unit,
    onAlertClick: () -> Unit, // Renamed from onAddAlert and onRemoveAlert to single callback
    currentAlert: com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert? = null,
    modifier: Modifier = Modifier
) {
    val isOpen = attraction.status.lowercase(Locale.GERMANY) == "opened"
    val waitTimeColor = getWaitTimeColor(attraction.waitTimeMinutes, isOpen)
    val waitTimeText = if (isOpen) "${attraction.waitTimeMinutes} min" else "Geschlossen"

    // Animation State
    var showSparkles by remember { mutableStateOf(false) }

    // Trigger sparkles when becoming favorite
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            showSparkles = true
            kotlinx.coroutines.delay(1000)
            showSparkles = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .favoriteAnimation(isFavorite)
            .favoriteGlowBorder(isFavorite)
            .sparkleOnFavorite(isFavorite, Color(0xFFFFD700)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFavorite) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFavorite) 8.dp else 2.dp
        )
    ) {
        // Sparkle Effect Overlay
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attraction Icon mit Animation
                Card(
                    modifier = Modifier
                        .size(44.dp)
                        .favoriteAnimation(isFavorite, 200),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = waitTimeColor.copy(alpha = if (isFavorite) 0.25f else 0.15f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = getAttractionIconResId(attraction.code)),
                            contentDescription = attraction.name,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = attraction.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = waitTimeColor.copy(alpha = if (isFavorite) 0.3f else 0.2f)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = if (isFavorite) Modifier.favoriteAnimation(true, 150) else Modifier
                        ) {
                            Text(
                                text = waitTimeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = waitTimeColor
                            )
                        }

                        if (hasAlert) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alert aktiv",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .pulsingGlow(MaterialTheme.colorScheme.primary, 1000)
                            )
                        }
                    }
                }

                // Action buttons mit Animationen
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Alert button - now always opens dialog with better icon
                    IconButton(
                        onClick = onAlertClick, // Always open dialog, regardless of hasAlert state
                        modifier = if (hasAlert) Modifier.pulsingGlow(MaterialTheme.colorScheme.primary, 2000) else Modifier
                    ) {
                        Icon(
                            imageVector = if (hasAlert) Icons.Default.EditNotifications else Icons.Default.AddAlert,
                            contentDescription = if (hasAlert) "Alert bearbeiten" else "Alert hinzufügen",
                            tint = if (hasAlert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Favorite button mit enhanced Animation
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.favoriteAnimation(isFavorite)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                            tint = if (isFavorite) {
                                FavoriteActive
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isFavorite) {
                                        Modifier.pulsingGlow(FavoriteActive, 1500)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }

                    // Quick Action: Show current wait time change trend with better icon
                    if (hasAlert && currentAlert != null) {
                        val isNearTarget = attraction.waitTimeMinutes <= currentAlert.targetTime + 5

                        if (isNearTarget) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Wartezeit sinkt",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .pulsingGlow(MaterialTheme.colorScheme.tertiary, 1000)
                            )
                        }
                    }
                }
            }

            // Sparkle Overlay
            if (showSparkles) {
                StarburstAnimation(
                    isActive = showSparkles,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun getWaitTimeColor(waitTime: Int, isOpen: Boolean): Color {
    return if (!isOpen) {
        WaitTimeClosed
    } else {
        when {
            waitTime <= 15 -> WaitTimeShort
            waitTime <= 30 -> WaitTimeMedium
            waitTime <= 60 -> WaitTimeLong
            else -> WaitTimeVeryLong
        }
    }
}
