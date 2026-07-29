package com.quantum_prof.phantalandwaittimes.ui.theme.main

import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.FakeSharedPreferences
import com.quantum_prof.phantalandwaittimes.data.UserPreferencesRepository
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.network.ApiService
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private class FakeApiService : ApiService {
        var response: List<AttractionWaitTime> = emptyList()
        var failure: Throwable? = null
        var callCount = 0

        /** When set, the call parks here until completed, so in-flight state can be asserted. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun getWaitTimes(parkId: String, language: String): List<AttractionWaitTime> {
            callCount++
            gate?.await()
            failure?.let { throw it }
            return response
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApiService()
    private val prefs = FakeSharedPreferences()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        // viewModelScope dispatches on Dispatchers.Main.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(): MainViewModel {
        val appScope = CoroutineScope(dispatcher)
        return MainViewModel(
            repository = WaitTimeRepository(api, prefs, json, dispatcher),
            preferencesRepository = UserPreferencesRepository(prefs, dispatcher, appScope),
            alertRepository = AlertRepository(prefs, json, dispatcher, appScope)
        )
    }

    /** `uiState` is a `WhileSubscribed` flow, so it only runs while something collects it. */
    private fun TestScope.collectUiState(viewModel: MainViewModel) {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }
    }

    private fun attraction(code: String, name: String = "Ride $code", wait: Int = 10) =
        AttractionWaitTime(code = code, name = name, waitTimeMinutes = wait, status = "opened")

    /**
     * Regression test: the in-flight guard used to read the same flag the UI used for "first
     * load", which defaulted to true. The request fired from `init` was therefore skipped and the
     * screen sat on the loading spinner forever.
     */
    @Test
    fun `the load started in init completes and clears the loading state`() = runTest(dispatcher) {
        api.response = listOf(attraction("1"), attraction("2"))
        val viewModel = createViewModel()
        collectUiState(viewModel)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, api.callCount)
        assertFalse("The spinner must not be stuck after the initial load", state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(2, state.waitTimes.size)
        assertNull(state.error)
    }

    @Test
    fun `the loading state is shown while the first request is running`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        api.response = listOf(attraction("1"))

        val viewModel = createViewModel()
        collectUiState(viewModel)
        // Everything runs up to the parked request, so the derived state is settled.
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.waitTimes.size)
    }

    @Test
    fun `a later refresh is not blocked by the initial load`() = runTest(dispatcher) {
        api.response = listOf(attraction("1"))
        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, api.callCount)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a refresh on top of existing data drives the refresh indicator, not the spinner`() =
        runTest(dispatcher) {
            api.response = listOf(attraction("1"))
            val viewModel = createViewModel()
            collectUiState(viewModel)
            advanceUntilIdle()

            val gate = CompletableDeferred<Unit>()
            api.gate = gate
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Existing data must stay visible during a refresh", state.isRefreshing)
            assertFalse(state.isLoading)
            assertEquals(1, state.waitTimes.size)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `overlapping refreshes collapse into a single request`() = runTest(dispatcher) {
        api.response = listOf(attraction("1"))
        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        // One for init, one for the first of the three overlapping calls.
        assertEquals(2, api.callCount)
    }

    @Test
    fun `a failed first load surfaces an error and stops loading`() = runTest(dispatcher) {
        api.failure = IOException("offline")
        val viewModel = createViewModel()
        collectUiState(viewModel)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(WaitTimeError.NO_CONNECTION, state.error)
        assertTrue(state.waitTimes.isEmpty())
    }

    @Test
    fun `a retry after a failure clears the error`() = runTest(dispatcher) {
        api.failure = IOException("offline")
        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()
        assertEquals(WaitTimeError.NO_CONNECTION, viewModel.uiState.value.error)

        api.failure = null
        api.response = listOf(attraction("1"))
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(1, state.waitTimes.size)
    }

    @Test
    fun `changing the filter re-derives the list without another request`() = runTest(dispatcher) {
        api.response = listOf(
            attraction("1", name = "Open Ride"),
            AttractionWaitTime(code = "2", name = "Closed Ride", status = "closed")
        )
        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.waitTimes.size)

        viewModel.setFilterOnlyOpen(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Open Ride"), state.waitTimes.map { it.name })
        assertEquals(2, state.totalCount)
        assertEquals(1, state.openCount)
        assertEquals(1, api.callCount)
    }

    @Test
    fun `toggling a favourite moves the attraction to the top`() = runTest(dispatcher) {
        api.response = listOf(attraction("1", name = "Alpha"), attraction("2", name = "Zulu"))
        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()
        assertEquals("Alpha", viewModel.uiState.value.waitTimes.first().name)

        viewModel.toggleFavorite("2")
        advanceUntilIdle()

        assertEquals("Zulu", viewModel.uiState.value.waitTimes.first().name)
    }
}
