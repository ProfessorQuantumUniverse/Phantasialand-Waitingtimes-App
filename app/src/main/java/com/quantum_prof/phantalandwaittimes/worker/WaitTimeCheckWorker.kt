package com.quantum_prof.phantalandwaittimes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker zum Überprüfen von Wartezeit-Alerts
 */
@HiltWorker
class WaitTimeCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val waitTimeRepository: WaitTimeRepository,
    private val alertRepository: AlertRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Lade aktive Alerts
            val activeAlerts = alertRepository.getAlerts()
            if (activeAlerts.isEmpty()) {
                return Result.success()
            }

            // 2. Hole aktuelle Wartezeiten
            val waitTimesResult = waitTimeRepository.getPhantasialandWaitTimes()

            waitTimesResult.onSuccess { result ->
                val (waitTimes, _) = result
                // 3. Prüfe jeden Alert
                activeAlerts.forEach { alert ->
                    val currentAttraction = waitTimes.find { it.code == alert.attractionCode }

                    if (currentAttraction != null &&
                        currentAttraction.status.lowercase() == "opened" &&
                        currentAttraction.waitTimeMinutes <= alert.targetTime) {

                        // 4. Sende Benachrichtigung
                        notificationService.showNotification(alert, currentAttraction.waitTimeMinutes)

                        // 5. Entferne Alert nach Benachrichtigung (einmalig)
                        alertRepository.removeAlert(alert.attractionCode)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
