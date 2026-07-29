package com.quantum_prof.phantalandwaittimes.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Immutable
class NotificationPermissionState(
    /** True when a notification posted right now would actually reach the user. */
    val isEnabled: Boolean,
    /** Shows the system permission prompt; a no-op below Android 13. */
    val request: () -> Unit
)

/**
 * Tracks whether the app may post notifications.
 *
 * The permission is requested when the user first arms an alert rather than on every cold start:
 * a prompt that appears before the feature is used gets denied, and a denial on Android 13+ is
 * effectively permanent after the second dismissal.
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(context.canPostNotifications()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isEnabled = context.canPostNotifications() }

    // The user may flip the switch in system settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        isEnabled = context.canPostNotifications()
        onPauseOrDispose { }
    }

    return remember(isEnabled) {
        NotificationPermissionState(
            isEnabled = isEnabled,
            request = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }
}

private fun Context.canPostNotifications(): Boolean {
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    return permissionGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}
