package com.quantum_prof.phantalandwaittimes.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the periodic background check.
 *
 * The check exists purely to serve wait-time alerts, so it is enabled while at least one alert
 * exists and cancelled otherwise — previously it ran on every app start regardless, burning
 * battery and data for users who never created an alert.
 */
@Singleton
class WaitTimeCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun setEnabled(enabled: Boolean) {
        if (enabled) schedule() else cancel()
    }

    private fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<WaitTimeCheckWorker>(
            CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // KEEP so an already-running schedule is not restarted on every app launch.
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "wait_time_check"

        /** 15 minutes is the shortest interval WorkManager honours for periodic work. */
        const val CHECK_INTERVAL_MINUTES = 15L
    }
}
