package com.quantum_prof.phantalandwaittimes.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.quantum_prof.phantalandwaittimes.data.network.ApiLanguage
import com.quantum_prof.phantalandwaittimes.data.network.ApiService
import com.quantum_prof.phantalandwaittimes.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * A consistent view of the wait times plus where they came from.
 *
 * [fetchedAt] is always the moment the data actually left the API — for a cache hit it is the
 * timestamp of the original network call, not of the cache read. That way the UI can honestly
 * report how old the numbers are.
 */
data class WaitTimeSnapshot(
    val waitTimes: List<AttractionWaitTime>,
    val isFromCache: Boolean,
    val fetchedAt: Long
)

@Singleton
class WaitTimeRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Serialises refreshes so that the UI and the background worker cannot fire two
     * simultaneous requests (and cannot interleave their cache writes).
     */
    private val refreshMutex = Mutex()

    /**
     * Loads the wait times, preferring a fresh network response.
     *
     * - Without [forceRefresh] a cache entry younger than [CACHE_VALIDITY_MILLIS] is returned as is.
     * - If the network call fails, any cached data is served instead (flagged as cached) so the
     *   app stays useful offline; only a failure with no cache at all produces [Result.failure].
     */
    suspend fun getWaitTimes(forceRefresh: Boolean = false): Result<WaitTimeSnapshot> =
        withContext(ioDispatcher) {
            refreshMutex.withLock {
                val language = ApiLanguage.forCurrentLocale()

                if (!forceRefresh) {
                    readCache(maxAgeMillis = CACHE_VALIDITY_MILLIS, requiredLanguage = language)
                        ?.let { return@withLock Result.success(it) }
                }

                try {
                    val fresh = apiService.getWaitTimes(language = language.header).sanitized()
                    val fetchedAt = System.currentTimeMillis()
                    writeCache(fresh, fetchedAt, language)
                    Result.success(WaitTimeSnapshot(fresh, isFromCache = false, fetchedAt = fetchedAt))
                } catch (cancellation: CancellationException) {
                    // Never swallow cancellation — it must propagate to unwind the caller's scope.
                    throw cancellation
                } catch (e: Exception) {
                    Log.w(TAG, "Wait time refresh failed, falling back to cache", e)
                    // The offline fallback deliberately accepts a cache written in another
                    // language: slightly stale labels beat an empty screen.
                    readCache(maxAgeMillis = null, requiredLanguage = null)
                        ?.let { Result.success(it) }
                        ?: Result.failure(e)
                }
            }
        }

    /** Drops the cached response, e.g. when it can no longer be parsed. */
    suspend fun clearCache() = withContext(ioDispatcher) {
        dropCache()
    }

    private fun dropCache() {
        sharedPreferences.edit {
            remove(KEY_CACHE)
            remove(KEY_CACHE_TIMESTAMP)
            remove(KEY_CACHE_LANGUAGE)
        }
    }

    /**
     * Removes entries the UI cannot render and collapses duplicate codes.
     *
     * Duplicates matter: `code` is used as the `LazyColumn` item key, and a repeated key
     * crashes Compose at runtime.
     */
    private fun List<AttractionWaitTime>.sanitized(): List<AttractionWaitTime> =
        filter { it.isUsable }.distinctBy { it.code }

    /**
     * @param maxAgeMillis how old the entry may be, or null to accept it at any age.
     * @param requiredLanguage the language the entry must have been fetched in, or null to accept
     *   any. A mismatch is a miss so that switching the device language re-fetches the data.
     */
    private fun readCache(maxAgeMillis: Long?, requiredLanguage: ApiLanguage?): WaitTimeSnapshot? {
        val cachedJson = sharedPreferences.getString(KEY_CACHE, null) ?: return null
        val cachedAt = sharedPreferences.getLong(KEY_CACHE_TIMESTAMP, 0L)
        if (cachedAt <= 0L) return null

        if (requiredLanguage != null) {
            val cachedLanguage = sharedPreferences.getString(KEY_CACHE_LANGUAGE, null)
            if (cachedLanguage != requiredLanguage.header) return null
        }

        if (maxAgeMillis != null) {
            val age = System.currentTimeMillis() - cachedAt
            // A negative age means the device clock moved backwards; treat it as stale.
            if (age < 0L || age > maxAgeMillis) return null
        }

        return try {
            val waitTimes = json.decodeFromString<List<AttractionWaitTime>>(cachedJson).sanitized()
            if (waitTimes.isEmpty()) null
            else WaitTimeSnapshot(waitTimes, isFromCache = true, fetchedAt = cachedAt)
        } catch (e: Exception) {
            Log.w(TAG, "Discarding unreadable wait time cache", e)
            dropCache()
            null
        }
    }

    private fun writeCache(
        waitTimes: List<AttractionWaitTime>,
        fetchedAt: Long,
        language: ApiLanguage
    ) {
        try {
            sharedPreferences.edit {
                putString(KEY_CACHE, json.encodeToString(waitTimes))
                putLong(KEY_CACHE_TIMESTAMP, fetchedAt)
                putString(KEY_CACHE_LANGUAGE, language.header)
            }
        } catch (e: Exception) {
            // A failed cache write must never fail the refresh itself.
            Log.w(TAG, "Could not persist wait time cache", e)
        }
    }

    private companion object {
        const val TAG = "WaitTimeRepository"
        const val KEY_CACHE = "cached_wait_times"
        const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        const val KEY_CACHE_LANGUAGE = "cache_language"
        val CACHE_VALIDITY_MILLIS = 5.minutes.inWholeMilliseconds
    }
}
