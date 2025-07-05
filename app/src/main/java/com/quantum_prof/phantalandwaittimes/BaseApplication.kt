package com.quantum_prof.phantalandwaittimes

import android.app.Application
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import com.quantum_prof.phantalandwaittimes.worker.WaitTimeCheckService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application() {

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var waitTimeCheckService: WaitTimeCheckService

    override fun onCreate() {
        super.onCreate()

        // Erstelle Notification Channel
        notificationService.createNotificationChannel()

        // Initialisiere Background Services
        initializeBackgroundServices()
    }

    private fun initializeBackgroundServices() {
        // Der WaitTimeCheckService wird über Dependency Injection bereitgestellt
        // und kann bei Bedarf von anderen Komponenten verwendet werden
        // Für periodische Checks könnte hier ein Timer oder AlarmManager verwendet werden

    }
}
