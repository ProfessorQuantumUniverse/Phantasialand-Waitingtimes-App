package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import com.quantum_prof.phantalandwaittimes.ui.waitTimeLabel

/**
 * Horizontal strip of the currently armed alerts.
 *
 * Alerts whose attraction is missing from the current data (filtered out, or dropped by the API)
 * are still shown — previously they vanished from this panel while remaining active, which made
 * them impossible to delete.
 */
@Composable
fun ActiveAlertsPanel(
    alerts: List<WaitTimeAlert>,
    waitTimes: List<AttractionWaitTime>,
    onEditAlert: (AttractionWaitTime) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    val byCode = waitTimes.associateBy { it.code }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.alerts_active_title, alerts.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = onCollapse, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.alerts_collapse),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(items = alerts, key = { it.attractionCode }) { alert ->
                    AlertChip(
                        alert = alert,
                        attraction = byCode[alert.attractionCode],
                        onEdit = { byCode[alert.attractionCode]?.let(onEditAlert) },
                        onRemove = { onRemoveAlert(alert.attractionCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertChip(
    alert: WaitTimeAlert,
    attraction: AttractionWaitTime?,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val isOpen = attraction?.isOpen == true
    val isTriggered = isOpen && attraction.displayWaitTime <= alert.targetMinutes

    val containerColor = when {
        isTriggered -> MaterialTheme.colorScheme.tertiaryContainer
        !isOpen -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isTriggered -> MaterialTheme.colorScheme.onTertiaryContainer
        !isOpen -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onEdit,
        // Nothing to edit when the attraction is not in the current data set.
        enabled = attraction != null,
        modifier = Modifier.width(168.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTriggered) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = attraction?.name.orEmpty().ifBlank { alert.attractionName },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.alerts_remove),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = attraction?.let { waitTimeLabel(it) } ?: stringResource(R.string.status_unknown),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.alerts_target_format, alert.targetMinutes),
                style = MaterialTheme.typography.labelSmall
            )

            if (isTriggered || !isOpen) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isTriggered) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Schedule
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isTriggered) {
                            stringResource(R.string.alerts_target_reached)
                        } else {
                            stringResource(R.string.status_closed)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
