package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitTimeAlertDialog(
    attraction: AttractionWaitTime,
    currentAlert: WaitTimeAlert?,
    onDismiss: () -> Unit,
    onSetAlert: (Int) -> Unit,
    onRemoveAlert: () -> Unit
) {
    var targetTimeText by remember {
        mutableStateOf(currentAlert?.targetTime?.toString() ?: "30")
    }
    val isEditing = currentAlert != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Schöneres Alert Icon mit gestapelten Elementen
                    Box {
                        Icon(
                            imageVector = Icons.Default.NotificationAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .pulsingGlow(MaterialTheme.colorScheme.primary, 2000)
                        )

                        // Kleiner animierter Punkt
                        if (!isEditing) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .offset(x = 20.dp, y = 0.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
                                    .pulsingGlow(MaterialTheme.colorScheme.tertiary, 1000)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) "Alert bearbeiten" else "Alert erstellen",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = attraction.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current wait time info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Aktuelle Wartezeit:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${attraction.waitTimeMinutes} min",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target time input with quick preset buttons
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Benachrichtigen wenn Wartezeit unter:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Quick preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 20, 30, 45).forEach { preset ->
                            FilterChip(
                                onClick = { targetTimeText = preset.toString() },
                                label = {
                                    Text(
                                        "${preset}min",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                selected = targetTimeText == preset.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetTimeText,
                        onValueChange = { targetTimeText = it },
                        label = { Text("Minuten") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("min") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                if (isEditing) {
                    // Editing layout: Delete button on top, action buttons below
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onRemoveAlert()
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alert löschen")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Abbrechen")
                            }

                            Button(
                                onClick = {
                                    val targetTime = targetTimeText.toIntOrNull()
                                    if (targetTime != null && targetTime > 0) {
                                        onSetAlert(targetTime)
                                        onDismiss()
                                    }
                                },
                                enabled = targetTimeText.toIntOrNull() != null && targetTimeText.toIntOrNull()!! > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aktualisieren")
                            }
                        }
                    }
                } else {
                    // New alert layout: just the action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abbrechen")
                        }

                        Button(
                            onClick = {
                                val targetTime = targetTimeText.toIntOrNull()
                                if (targetTime != null && targetTime > 0) {
                                    onSetAlert(targetTime)
                                    onDismiss()
                                }
                            },
                            enabled = targetTimeText.toIntOrNull() != null && targetTimeText.toIntOrNull()!! > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Erstellen")
                        }
                    }
                }
            }
        }
    }
}
