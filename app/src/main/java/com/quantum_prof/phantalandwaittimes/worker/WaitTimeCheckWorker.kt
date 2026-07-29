package com.quantum_prof.phantalandwaittimes.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import com.quantum_prof.phantalandwaittimes.notification.NotificationService
import com.quantum_prof.phantalandwaittimes.notification.TriggeredAlert
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Periodically checks whether any attraction has dropped to its alert threshold and notifies.
 *
 * An alert fires at most once: it is removed as soon as its notification has been posted.
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
            val activeAlerts = alertRepository.refresh()
            if (activeAlerts.isEmpty()) {
                // Nothing left to watch — the scheduler will be torn down by the app as well.
                return Result.success()
            }

            if (!notificationService.canPostNotifications()) {
                // Without the runtime permission a check would only waste data.
                Log.i(TAG, "Notifications are disabled; skipping check")
                return Result.success()
            }

            // Always go to the network: a cache hit could be up to five minutes stale, which is a
            // third of the interval between two checks.
            val result = waitTimeRepository.getWaitTimes(forceRefresh = true)

            val snapshot = result.getOrElse { error ->
                Log.w(TAG, "Wait time check could not load data", error)
                return retryOrGiveUp()
            }

            val byCode = snapshot.waitTimes.associateBy { it.code }
            val triggered = activeAlerts.mapNotNull { alert ->
                val attraction = byCode[alert.attractionCode] ?: return@mapNotNull null
                if (attraction.isOpen && attraction.displayWaitTime <= alert.targetMinutes) {
                    TriggeredAlert(alert, attraction.displayWaitTime)
                } else {
                    null
                }
            }

            // Posted in one batch so several alerts firing in the same check arrive bundled
            // under a summary instead of as a wall of separate notifications.
            notificationService.showAlertsReached(triggered)
            triggered.forEach { alertRepository.removeAlert(it.alert.attractionCode) }

            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Wait time check failed", e)
            retryOrGiveUp()
        }
    }

    /**
     * Stops retrying after [MAX_ATTEMPTS]. Endless retries would keep waking the device up while
     * the next periodic run is only 15 minutes away anyway.
     */
    private fun retryOrGiveUp(): Result =
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()

    private companion object {
        const val TAG = "WaitTimeCheckWorker"
        const val MAX_ATTEMPTS = 3
    }
}
