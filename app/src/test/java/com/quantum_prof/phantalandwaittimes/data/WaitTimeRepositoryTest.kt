package com.quantum_prof.phantalandwaittimes.data

import com.quantum_prof.phantalandwaittimes.data.network.ApiLanguage
import com.quantum_prof.phantalandwaittimes.data.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Locale

class WaitTimeRepositoryTest {

    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun setUpLocale() {
        // Most tests do not care about the language; pin one so they are deterministic.
        Locale.setDefault(Locale.GERMANY)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private class FakeApiService : ApiService {
        var response: List<AttractionWaitTime> = emptyList()
        var failure: Throwable? = null
        var callCount = 0
        var lastLanguage: String? = null

        override suspend fun getWaitTimes(parkId: String, language: String): List<AttractionWaitTime> {
            callCount++
            lastLanguage = language
            failure?.let { throw it }
            return response
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val api = FakeApiService()
    private val prefs = FakeSharedPreferences()

    /**
     * The repository's I/O dispatcher must share the scheduler `runTest` drives, otherwise
     * `withContext` dispatches onto a scheduler nothing ever advances.
     */
    private fun TestScope.repository() = WaitTimeRepository(
        apiService = api,
        sharedPreferences = prefs,
        json = json,
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun attraction(code: String, name: String = "Ride $code", wait: Int = 10) =
        AttractionWaitTime(code = code, name = name, waitTimeMinutes = wait, status = "opened")

    @Test
    fun `a successful fetch is reported as fresh`() = runTest {
        api.response = listOf(attraction("1"), attraction("2"))

        val snapshot = repository().getWaitTimes().getOrThrow()

        assertEquals(2, snapshot.waitTimes.size)
        assertFalse(snapshot.isFromCache)
        assertTrue(snapshot.fetchedAt > 0L)
    }

    @Test
    fun `a second call within the cache window does not hit the network`() = runTest {
        api.response = listOf(attraction("1"))
        val repository = repository()

        repository.getWaitTimes().getOrThrow()
        val second = repository.getWaitTimes().getOrThrow()

        assertEquals(1, api.callCount)
        assertTrue(second.isFromCache)
    }

    @Test
    fun `forceRefresh bypasses a valid cache`() = runTest {
        api.response = listOf(attraction("1"))
        val repository = repository()

        repository.getWaitTimes().getOrThrow()
        val second = repository.getWaitTimes(forceRefresh = true).getOrThrow()

        assertEquals(2, api.callCount)
        assertFalse(second.isFromCache)
    }

    @Test
    fun `a network failure falls back to cached data and keeps the original timestamp`() = runTest {
        api.response = listOf(attraction("1", name = "Taron"))
        val repository = repository()
        val fresh = repository.getWaitTimes().getOrThrow()

        api.failure = IOException("offline")
        val fallback = repository.getWaitTimes(forceRefresh = true).getOrThrow()

        assertTrue(fallback.isFromCache)
        assertEquals("Taron", fallback.waitTimes.single().name)
        // The banner must report when the data was actually fetched, not when the cache was read.
        assertEquals(fresh.fetchedAt, fallback.fetchedAt)
    }

    @Test
    fun `a network failure without any cache surfaces the error`() = runTest {
        api.failure = IOException("offline")

        val result = repository().getWaitTimes()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `duplicate codes are collapsed so list keys stay unique`() = runTest {
        api.response = listOf(
            attraction("1", name = "First"),
            attraction("1", name = "Duplicate"),
            attraction("2")
        )

        val snapshot = repository().getWaitTimes().getOrThrow()

        assertEquals(listOf("1", "2"), snapshot.waitTimes.map { it.code })
        assertEquals("First", snapshot.waitTimes.first().name)
    }

    @Test
    fun `entries without a usable code or name are dropped`() = runTest {
        api.response = listOf(
            attraction("1"),
            AttractionWaitTime(code = "", name = "No code", status = "opened"),
            AttractionWaitTime(code = "9", name = "", status = "opened")
        )

        val snapshot = repository().getWaitTimes().getOrThrow()

        assertEquals(listOf("1"), snapshot.waitTimes.map { it.code })
    }

    @Test
    fun `cancellation propagates instead of being swallowed as a failure`() = runTest {
        api.failure = CancellationException("cancelled")

        try {
            repository().getWaitTimes()
            fail("Expected the CancellationException to propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun `an unreadable cache is discarded rather than served`() = runTest {
        api.response = listOf(attraction("1"))
        val repository = repository()
        repository.getWaitTimes().getOrThrow()

        // Corrupt the stored payload the way a partial write or a schema change would.
        prefs.edit().putString("cached_wait_times", "{not json").commit()

        api.failure = IOException("offline")
        val result = repository.getWaitTimes(forceRefresh = true)

        assertTrue(result.isFailure)
        assertNull(prefs.getString("cached_wait_times", null))
    }

    @Test
    fun `a German device asks the API for German data`() = runTest {
        Locale.setDefault(Locale.GERMANY)
        api.response = listOf(attraction("1"))

        repository().getWaitTimes().getOrThrow()

        assertEquals(ApiLanguage.GERMAN.header, api.lastLanguage)
    }

    @Test
    fun `any other locale falls back to the English data the API supports`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("pl-PL"))
        api.response = listOf(attraction("1"))

        repository().getWaitTimes().getOrThrow()

        assertEquals(ApiLanguage.ENGLISH.header, api.lastLanguage)
    }

    @Test
    fun `switching language invalidates an otherwise fresh cache`() = runTest {
        Locale.setDefault(Locale.GERMANY)
        api.response = listOf(attraction("1"))
        val repository = repository()
        repository.getWaitTimes().getOrThrow()

        Locale.setDefault(Locale.US)
        val afterSwitch = repository.getWaitTimes().getOrThrow()

        assertEquals(2, api.callCount)
        assertFalse(afterSwitch.isFromCache)
        assertEquals(ApiLanguage.ENGLISH.header, api.lastLanguage)
    }

    @Test
    fun `the offline fallback still serves a cache written in another language`() = runTest {
        Locale.setDefault(Locale.GERMANY)
        api.response = listOf(attraction("1", name = "Taron"))
        val repository = repository()
        repository.getWaitTimes().getOrThrow()

        Locale.setDefault(Locale.US)
        api.failure = IOException("offline")
        val fallback = repository.getWaitTimes().getOrThrow()

        // Better a German label than an empty screen.
        assertTrue(fallback.isFromCache)
        assertEquals("Taron", fallback.waitTimes.single().name)
    }

    @Test
    fun `clearCache removes the stored payload`() = runTest {
        api.response = listOf(attraction("1"))
        val repository = repository()
        repository.getWaitTimes().getOrThrow()
        assertNotNull(prefs.getString("cached_wait_times", null))

        repository.clearCache()

        assertNull(prefs.getString("cached_wait_times", null))
    }
}
