package com.quantum_prof.phantalandwaittimes

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import com.quantum_prof.phantalandwaittimes.di.ApplicationScope
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import com.quantum_prof.phantalandwaittimes.worker.WaitTimeCheckScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var alertRepository: AlertRepository

    @Inject
    lateinit var checkScheduler: WaitTimeCheckScheduler

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationService.createNotificationChannel()
        keepBackgroundChecksInSyncWithAlerts()
    }

    /**
     * Single owner of the background check schedule: the periodic worker runs exactly while at
     * least one alert exists. Driving this from the process scope rather than a ViewModel keeps
     * the schedule correct even when the user never opens the list screen.
     */
    private fun keepBackgroundChecksInSyncWithAlerts() {
        appScope.launch {
            alertRepository.alerts
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { hasAlerts -> checkScheduler.setEnabled(hasAlerts) }
        }
    }
}
