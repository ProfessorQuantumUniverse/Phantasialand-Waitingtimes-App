package com.quantum_prof.phantalandwaittimes.worker

import android.content.Context
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service zum Überprüfen von Wartezeit-Alerts
 * Alternative Implementierung ohne WorkManager-Abhängigkeiten
 */
@Singleton
class WaitTimeCheckService @Inject constructor(
    private val waitTimeRepository: WaitTimeRepository,
    private val alertRepository: AlertRepository,
    private val notificationService: NotificationService
) {

    /**
     * Prüft alle aktiven Alerts und sendet Benachrichtigungen bei Bedarf
     * @return true wenn erfolgreich, false bei Fehlern
     */
    suspend fun checkAlerts(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Lade aktive Alerts
            val activeAlerts = alertRepository.getAlerts()
            if (activeAlerts.isEmpty()) {
                return@withContext true
            }

            // 2. Hole aktuelle Wartezeiten
            val waitTimesResult = waitTimeRepository.getPhantasialandWaitTimes()

            waitTimesResult.onSuccess { (waitTimes, _) ->
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

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Prüft einen spezifischen Alert
     */
    suspend fun checkSpecificAlert(attractionCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val alert = alertRepository.getAlerts().find { it.attractionCode == attractionCode }
                ?: return@withContext false

            val waitTimesResult = waitTimeRepository.getPhantasialandWaitTimes()

            waitTimesResult.onSuccess { (waitTimes, _) ->
                val currentAttraction = waitTimes.find { it.code == attractionCode }

                if (currentAttraction != null &&
                    currentAttraction.status.lowercase() == "opened" &&
                    currentAttraction.waitTimeMinutes <= alert.targetTime) {

                    notificationService.showNotification(alert, currentAttraction.waitTimeMinutes)
                    alertRepository.removeAlert(attractionCode)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
