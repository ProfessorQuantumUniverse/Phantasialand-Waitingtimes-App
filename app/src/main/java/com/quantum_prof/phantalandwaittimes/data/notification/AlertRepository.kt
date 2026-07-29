package com.quantum_prof.phantalandwaittimes.data.notification

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.quantum_prof.phantalandwaittimes.di.ApplicationScope
import com.quantum_prof.phantalandwaittimes.di.IoDispatcher
import com.quantum_prof.phantalandwaittimes.di.StorageModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's wait-time alerts and publishes them as a [StateFlow].
 *
 * Storage format note: alerts used to be kept as a `Set<String>` of individual JSON objects.
 * That silently collapsed two alerts whose JSON was byte-identical and had no defined order,
 * so they are now stored as a single JSON array under [StorageModule.KEY_WAIT_TIME_ALERTS_V2].
 * Data written by older versions is migrated on first read.
 */
@Singleton
class AlertRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope appScope: CoroutineScope
) {

    /** Guards every read-modify-write cycle against concurrent UI and worker access. */
    private val mutex = Mutex()

    private val _alerts = MutableStateFlow<List<WaitTimeAlert>>(emptyList())

    /** The current alerts. Emits again whenever an alert is added, updated or removed. */
    val alerts: StateFlow<List<WaitTimeAlert>> = _alerts.asStateFlow()

    init {
        // Load off the main thread; the flow starts empty and fills in shortly after.
        appScope.launch { refresh() }
    }

    /** Re-reads from disk and republishes. Returns the authoritative list. */
    suspend fun refresh(): List<WaitTimeAlert> = withContext(ioDispatcher) {
        mutex.withLock {
            readFromDisk().also { _alerts.value = it }
        }
    }

    /**
     * Adds [alert], replacing any existing alert for the same attraction.
     * Invalid alerts are ignored rather than persisted.
     */
    suspend fun addAlert(alert: WaitTimeAlert) {
        if (!alert.isValid) {
            Log.w(TAG, "Ignoring invalid alert for code='${alert.attractionCode}'")
            return
        }
        mutate { current ->
            current.filterNot { it.attractionCode == alert.attractionCode } + alert
        }
    }

    suspend fun removeAlert(attractionCode: String) {
        mutate { current -> current.filterNot { it.attractionCode == attractionCode } }
    }

    suspend fun getAlertFor(attractionCode: String): WaitTimeAlert? =
        refresh().firstOrNull { it.attractionCode == attractionCode }

    private suspend fun mutate(transform: (List<WaitTimeAlert>) -> List<WaitTimeAlert>) {
        withContext(ioDispatcher) {
            mutex.withLock {
                val updated = transform(readFromDisk()).sortedBy { it.createdAt }
                writeToDisk(updated)
                _alerts.value = updated
            }
        }
    }

    private fun readFromDisk(): List<WaitTimeAlert> {
        sharedPreferences.getString(StorageModule.KEY_WAIT_TIME_ALERTS_V2, null)?.let { raw ->
            return try {
                json.decodeFromString<List<WaitTimeAlert>>(raw).filter { it.isValid }
            } catch (e: Exception) {
                Log.w(TAG, "Discarding unreadable alert store", e)
                sharedPreferences.edit { remove(StorageModule.KEY_WAIT_TIME_ALERTS_V2) }
                emptyList()
            }
        }
        return migrateLegacyAlerts()
    }

    /** One-off conversion of the pre-2.2 `Set<String>` storage into the JSON array format. */
    private fun migrateLegacyAlerts(): List<WaitTimeAlert> {
        val legacy = try {
            sharedPreferences.getStringSet(StorageModule.KEY_WAIT_TIME_ALERTS_LEGACY, null)
        } catch (e: ClassCastException) {
            Log.w(TAG, "Legacy alert key held an unexpected type", e)
            null
        } ?: return emptyList()

        val migrated = legacy.mapNotNull { entry ->
            try {
                json.decodeFromString<WaitTimeAlert>(entry)
            } catch (e: Exception) {
                Log.w(TAG, "Skipping unreadable legacy alert", e)
                null
            }
        }.filter { it.isValid }

        writeToDisk(migrated)
        sharedPreferences.edit { remove(StorageModule.KEY_WAIT_TIME_ALERTS_LEGACY) }
        Log.i(TAG, "Migrated ${migrated.size} alert(s) to the new storage format")
        return migrated
    }

    private fun writeToDisk(alerts: List<WaitTimeAlert>) {
        try {
            sharedPreferences.edit {
                putString(StorageModule.KEY_WAIT_TIME_ALERTS_V2, json.encodeToString(alerts))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not persist alerts", e)
        }
    }

    private companion object {
        const val TAG = "AlertRepository"
    }
}
