package com.quantum_prof.phantalandwaittimes.data

import android.content.SharedPreferences
import com.quantum_prof.phantalandwaittimes.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class WaitTimeResult(
    val waitTimes: List<AttractionWaitTime>,
    val isFromCache: Boolean
)

@Singleton
class WaitTimeRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences,
    private val json: Json
) {

    companion object {
        private const val CACHE_KEY = "cached_wait_times"
        private const val CACHE_TIMESTAMP_KEY = "cache_timestamp"
        private const val CACHE_VALIDITY_MINUTES = 5
    }

    suspend fun getPhantasialandWaitTimes(forceRefresh: Boolean = false): Result<WaitTimeResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Prüfe Cache wenn nicht forced refresh
                if (!forceRefresh) {
                    val cachedResult = getCachedData()
                    if (cachedResult != null) {
                        return@withContext Result.success(cachedResult)
                    }
                }

                // Lade von API
                val waitTimes = apiService.getWaitTimes()

                // Speichere in Cache
                saveCachedData(waitTimes)

                Result.success(WaitTimeResult(waitTimes, false))
            } catch (e: Exception) {
                // Bei Fehler versuche Cache zu laden
                val cachedResult = getCachedData(ignoreValidity = true)
                if (cachedResult != null) {
                    Result.success(cachedResult.copy(isFromCache = true))
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    private fun getCachedData(ignoreValidity: Boolean = false): WaitTimeResult? {
        val cachedJson = sharedPreferences.getString(CACHE_KEY, null) ?: return null
        val cacheTimestamp = sharedPreferences.getLong(CACHE_TIMESTAMP_KEY, 0)

        if (!ignoreValidity) {
            val currentTime = System.currentTimeMillis()
            val cacheAge = (currentTime - cacheTimestamp) / (1000 * 60) // in minutes
            if (cacheAge > CACHE_VALIDITY_MINUTES) {
                return null
            }
        }

        return try {
            val waitTimes = json.decodeFromString<List<AttractionWaitTime>>(cachedJson)
            WaitTimeResult(waitTimes, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCachedData(waitTimes: List<AttractionWaitTime>) {
        try {
            val jsonString = json.encodeToString(waitTimes)
            sharedPreferences.edit()
                .putString(CACHE_KEY, jsonString)
                .putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
