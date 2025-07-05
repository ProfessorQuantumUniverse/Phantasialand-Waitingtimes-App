package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import com.quantum_prof.phantalandwaittimes.ui.theme.*

@Composable
fun WaitTimeAlertDialog(
    attraction: AttractionWaitTime,
    currentAlert: WaitTimeAlert?,
    onDismiss: () -> Unit,
    onSetAlert: (Int) -> Unit,
    onRemoveAlert: () -> Unit
) {
    var targetTimeText by remember {
        mutableStateOf(currentAlert?.targetTime?.toString() ?: "15")
    }
    var isValidTime by remember { mutableStateOf(true) }

    // Moderner Material 3 AlertDialog
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Entfernen Button (nur wenn Alert existiert)
                if (currentAlert != null) {
                    TextButton(
                        onClick = {
                            onRemoveAlert()
                            onDismiss()
                        }
                    ) {
                        Text("Remove")
                    }
                }

                // Speichern Button
                TextButton(
                    onClick = {
                        val targetTime = targetTimeText.toIntOrNull()
                        if (targetTime != null && targetTime > 0 && targetTime <= 180) {
                            onSetAlert(targetTime)
                            onDismiss()
                        }
                    },
                    enabled = isValidTime && targetTimeText.isNotEmpty()
                ) {
                    Text(if (currentAlert != null) "Edit" else "Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abort")
            }
        },
        icon = {
            Icon(
                imageVector = if (currentAlert != null) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (currentAlert != null) "Edit Alert" else "Edit Queue-Time-Alert",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                // Attraktionsname
                Text(
                    text = "For: ${attraction.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Aktueller Alert Status
                if (currentAlert != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Current Alert: ≤ ${currentAlert.targetTime} Min",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Eingabefeld
                OutlinedTextField(
                    value = targetTimeText,
                    onValueChange = { newValue ->
                        targetTimeText = newValue
                        isValidTime = try {
                            val time = newValue.toIntOrNull()
                            time != null && time > 0 && time <= 180
                        } catch (e: Exception) {
                            false
                        }
                    },
                    label = { Text("Queue time: (Minuten)") },
                    placeholder = { Text("e.g. 15") },
                    suffix = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValidTime && targetTimeText.isNotEmpty(),
                    supportingText = {
                        if (!isValidTime && targetTimeText.isNotEmpty()) {
                            Text("Please enter a valid number between 1 and 180", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("You will be notified when the wait time is under this value.",)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}
