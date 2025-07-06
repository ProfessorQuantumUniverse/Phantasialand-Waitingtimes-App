package com.quantum_prof.phantalandwaittimes.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaitTimeCheckService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun startPeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequestBuilder = PeriodicWorkRequest.Builder(
            WaitTimeCheckWorker::class.java,
            15L, TimeUnit.MINUTES
        )
        workRequestBuilder.setConstraints(constraints)
        val workRequest = workRequestBuilder.build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "wait_time_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    fun stopPeriodicChecks() {
        WorkManager.getInstance(context)
            .cancelUniqueWork("wait_time_check")
    }
}
