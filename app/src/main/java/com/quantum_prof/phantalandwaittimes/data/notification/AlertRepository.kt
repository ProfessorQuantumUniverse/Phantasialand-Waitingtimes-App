package com.quantum_prof.phantalandwaittimes.data.notification

import android.content.SharedPreferences
import com.quantum_prof.phantalandwaittimes.di.StorageModule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val json: Json
) {

    fun getAlerts(): List<WaitTimeAlert> {
        val alertStrings = sharedPreferences.getStringSet(StorageModule.KEY_WAIT_TIME_ALERTS, emptySet()) ?: emptySet()
        return alertStrings.mapNotNull {
            try {
                json.decodeFromString<WaitTimeAlert>(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun addAlert(alert: WaitTimeAlert) {
        val currentAlerts = getAlerts().toMutableList()
        currentAlerts.removeAll { it.attractionCode == alert.attractionCode }
        currentAlerts.add(alert)
        saveAlerts(currentAlerts)
    }

    fun removeAlert(attractionCode: String) {
        val currentAlerts = getAlerts().toMutableList()
        currentAlerts.removeAll { it.attractionCode == attractionCode }
        saveAlerts(currentAlerts)
    }

    fun getAlertFor(attractionCode: String): WaitTimeAlert? {
        return getAlerts().find { it.attractionCode == attractionCode }
    }

    private fun saveAlerts(alerts: List<WaitTimeAlert>) {
        val alertStrings = alerts.map { json.encodeToString(it) }.toSet()
        sharedPreferences.edit()
            .putStringSet(StorageModule.KEY_WAIT_TIME_ALERTS, alertStrings)
            .apply()
    }
}

