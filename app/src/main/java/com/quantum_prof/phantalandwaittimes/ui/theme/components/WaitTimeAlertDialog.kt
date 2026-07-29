package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import com.quantum_prof.phantalandwaittimes.ui.waitTimeLabel

private val PRESET_MINUTES = listOf(10, 20, 30, 45)

/**
 * Create or edit the alert threshold for one attraction.
 *
 * Built on [AlertDialog] so it inherits Material's sizing, insets and predictive-back handling
 * instead of hand-rolling a `Dialog` + `Card`. Input is validated inline rather than only by
 * disabling the confirm button, so a typo explains itself.
 */
@Composable
fun WaitTimeAlertDialog(
    attraction: AttractionWaitTime,
    currentAlert: WaitTimeAlert?,
    notificationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSetAlert: (Int) -> Unit,
    onRemoveAlert: () -> Unit
) {
    val isEditing = currentAlert != null

    var targetText by rememberSaveable(currentAlert?.targetMinutes) {
        mutableStateOf(
            (currentAlert?.targetMinutes ?: WaitTimeAlert.DEFAULT_TARGET_MINUTES).toString()
        )
    }

    val parsedTarget = remember(targetText) {
        targetText.trim().toIntOrNull()
            ?.takeIf { it in WaitTimeAlert.MIN_TARGET_MINUTES..WaitTimeAlert.MAX_TARGET_MINUTES }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isEditing) R.string.alert_dialog_edit_title else R.string.alert_dialog_create_title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.alert_dialog_current_wait),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = waitTimeLabel(attraction),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.alert_dialog_notify_when),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PRESET_MINUTES.forEach { preset ->
                        FilterChip(
                            selected = parsedTarget == preset,
                            onClick = { targetText = preset.toString() },
                            label = {
                                Text(
                                    text = stringResource(R.string.wait_minutes, preset),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input ->
                        // Only digits can ever reach the state, so the field cannot hold junk.
                        targetText = input.filter(Char::isDigit).take(3)
                    },
                    label = { Text(stringResource(R.string.alert_dialog_minutes_label)) },
                    suffix = { Text(stringResource(R.string.alert_dialog_minutes_suffix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = targetText.isNotEmpty() && parsedTarget == null,
                    supportingText = {
                        if (targetText.isNotEmpty() && parsedTarget == null) {
                            Text(
                                stringResource(
                                    R.string.alert_dialog_invalid_input,
                                    WaitTimeAlert.MIN_TARGET_MINUTES,
                                    WaitTimeAlert.MAX_TARGET_MINUTES
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!notificationsEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.alert_dialog_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (isEditing) {
                    Spacer(Modifier.height(8.dp))
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
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.alert_dialog_delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedTarget?.let {
                        onSetAlert(it)
                        onDismiss()
                    }
                },
                enabled = parsedTarget != null
            ) {
                Text(
                    stringResource(
                        if (isEditing) R.string.alert_dialog_save else R.string.alert_dialog_create
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alert_dialog_cancel))
            }
        }
    )
}
