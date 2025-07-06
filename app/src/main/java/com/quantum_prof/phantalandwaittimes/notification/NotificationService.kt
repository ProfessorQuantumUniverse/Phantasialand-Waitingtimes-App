package com.quantum_prof.phantalandwaittimes.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.quantum_prof.phantalandwaittimes.R
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "wait_time_alerts"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Wait Time Alerts"
            val descriptionText = "Notifications for when attraction wait times drop"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(alert: WaitTimeAlert, currentWaitTime: Int) {
        val notificationId = alert.attractionCode.hashCode()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_coaster) // Ersetze dies mit einem passenden Icon
            .setContentTitle("Wait time for ${alert.attractionName} is low!")
            .setContentText("Current wait time is $currentWaitTime minutes (your target was under ${alert.targetTime} minutes).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

