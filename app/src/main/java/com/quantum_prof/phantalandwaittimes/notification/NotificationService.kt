package com.quantum_prof.phantalandwaittimes.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quantum_prof.phantalandwaittimes.MainActivity
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** An alert whose target has been met, together with the wait time that met it. */
data class TriggeredAlert(
    val alert: WaitTimeAlert,
    val currentWaitMinutes: Int
)

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * A queue that just got short is only useful for a few minutes, so the channel is
     * high-importance and heads-up.
     *
     * An existing channel's importance cannot be raised programmatically, which is why this uses a
     * new channel id and removes the original one — otherwise everyone who already had the app
     * installed would stay on the old, silent channel forever.
     */
    fun createNotificationChannel() {
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)

        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(context.getString(R.string.notification_channel_description))
            .setVibrationEnabled(true)
            .build()

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Whether a notification would actually reach the user: the runtime permission (Android 13+)
     * must be granted *and* notifications must not be switched off for the app.
     */
    fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        return permissionGranted && notificationManager.areNotificationsEnabled()
    }

    /**
     * Posts one notification per [triggered] alert.
     *
     * When several fire in the same check they are bundled under a summary, so a group of friends'
     * worth of alerts does not arrive as a wall of separate notifications.
     */
    fun showAlertsReached(triggered: List<TriggeredAlert>) {
        if (triggered.isEmpty()) return
        if (!canPostNotifications()) {
            Log.i(TAG, "Notifications suppressed: not permitted")
            return
        }

        triggered.forEach { post(it) }

        if (triggered.size > 1) {
            postSummary(triggered)
        }
    }

    private fun post(triggered: TriggeredAlert) {
        val alert = triggered.alert
        val body = context.getString(
            R.string.notification_text,
            triggered.currentWaitMinutes,
            alert.targetMinutes
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title, alert.attractionName))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAttractionIntent(alert.attractionCode))
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()

        notify(alert.attractionCode.hashCode(), notification)
    }

    private fun postSummary(triggered: List<TriggeredAlert>) {
        val inbox = NotificationCompat.InboxStyle()
            .setSummaryText(
                context.resources.getQuantityString(
                    R.plurals.notification_summary_text,
                    triggered.size,
                    triggered.size
                )
            )
        triggered.forEach { inbox.addLine(it.alert.attractionName) }

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_summary_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notification_summary_text,
                    triggered.size,
                    triggered.size
                )
            )
            .setStyle(inbox)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAttractionIntent(attractionCode = null))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            // The permission can be revoked between the check above and this call.
            Log.w(TAG, "Notification rejected by the system", e)
        }
    }

    /**
     * Opens the list and, when [attractionCode] is set, scrolls straight to that attraction rather
     * than dropping the user at the top of an unfamiliar list.
     */
    private fun openAttractionIntent(attractionCode: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            attractionCode?.let { putExtra(EXTRA_ATTRACTION_CODE, it) }
        }

        return PendingIntent.getActivity(
            context,
            // A distinct request code per attraction, otherwise all notifications would share
            // (and overwrite) a single PendingIntent and every tap would open the same ride.
            attractionCode?.hashCode() ?: SUMMARY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_ATTRACTION_CODE = "extra_attraction_code"

        private const val CHANNEL_ID = "wait_time_alerts_high"

        /** Pre-2.2 channel, created with default importance. Deleted on first launch. */
        private const val LEGACY_CHANNEL_ID = "wait_time_alerts"

        private const val GROUP_KEY = "com.quantum_prof.phantalandwaittimes.WAIT_TIME_ALERTS"
        private const val SUMMARY_NOTIFICATION_ID = 1
        private const val TAG = "NotificationService"
    }
}
