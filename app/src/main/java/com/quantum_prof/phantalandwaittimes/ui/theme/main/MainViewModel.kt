package com.quantum_prof.phantalandwaittimes.ui.theme.main

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.UserPreferences
import com.quantum_prof.phantalandwaittimes.data.UserPreferencesRepository
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.WaitTimeSnapshot
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import com.quantum_prof.phantalandwaittimes.data.notification.WaitTimeAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/** Why a refresh failed. The UI maps this onto a localised message. */
enum class WaitTimeError {
    NO_CONNECTION,
    TIMEOUT,
    SERVER,
    UNKNOWN
}

/**
 * Marked [Immutable] because it is only ever rebuilt, never mutated in place. Without this the
 * `List`/`Set` members make the whole class unstable to the Compose compiler, which forces the
 * entire screen to recompose on every emission — including once a minute for the freshness label.
 */
@Immutable
data class WaitTimeUiState(
    /** True only for the very first load, when there is nothing to show yet. */
    val isLoading: Boolean = false,
    /** True while a refresh runs on top of data that is already on screen. */
    val isRefreshing: Boolean = false,
    val waitTimes: List<AttractionWaitTime> = emptyList(),
    val error: WaitTimeError? = null,
    /** When the displayed data was fetched from the API (0 if never). */
    val lastUpdated: Long = 0L,
    val isOfflineData: Boolean = false,
    val sortType: SortType = SortType.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val favoriteCodes: Set<String> = emptySet(),
    val filterOnlyOpen: Boolean = false,
    val activeAlerts: List<WaitTimeAlert> = emptyList(),
    /**
     * Codes of [activeAlerts], precomputed so the list can do an O(1) membership test per row
     * instead of rebuilding the set on every read.
     */
    val activeAlertCodes: Set<String> = emptySet(),
    /** Counts over the unfiltered data, so the filter row can show what it is hiding. */
    val totalCount: Int = 0,
    val openCount: Int = 0
) {
    val hasContent: Boolean get() = waitTimes.isNotEmpty()
}

/**
 * The parts of the state this ViewModel owns directly; everything else is derived.
 *
 * Note that this tracks only whether a request is running, not how it should be presented.
 * "First load" versus "refresh on top of existing data" follows from [snapshot] and is computed
 * in [MainViewModel.buildUiState].
 */
private data class FetchState(
    val snapshot: WaitTimeSnapshot? = null,
    val isRequestInFlight: Boolean = false,
    val error: WaitTimeError? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WaitTimeRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val fetchState = MutableStateFlow(FetchState())

    private var refreshJob: Job? = null

    /**
     * The screen state is fully derived: sorting and filtering are recomputed from the last
     * fetched snapshot whenever the data or the user's preferences change. There is no second
     * copy of the list to keep in sync.
     */
    val uiState: StateFlow<WaitTimeUiState> = combine(
        fetchState,
        preferencesRepository.preferences,
        alertRepository.alerts
    ) { fetch, preferences, alerts ->
        buildUiState(fetch, preferences, alerts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WaitTimeUiState(isLoading = true)
    )

    init {
        refresh(force = false)
    }

    /**
     * Loads the wait times. [force] bypasses the repository cache and is what pull-to-refresh uses;
     * the cheap non-forced variant is used on start-up and when returning to the screen.
     */
    fun refresh(force: Boolean = true) {
        // Guard on the job rather than on a flag: if a previous request ever died without
        // clearing its flag, a stale flag would lock the screen on the loading state forever.
        if (refreshJob?.isActive == true) return

        fetchState.update { it.copy(isRequestInFlight = true) }

        refreshJob = viewModelScope.launch {
            try {
                repository.getWaitTimes(forceRefresh = force)
                    .onSuccess { snapshot ->
                        fetchState.update {
                            it.copy(
                                snapshot = snapshot,
                                // Cached data means the network call did not succeed this time,
                                // so a previous error stays visible.
                                error = if (snapshot.isFromCache) it.error else null
                            )
                        }
                    }
                    .onFailure { throwable ->
                        fetchState.update { it.copy(error = throwable.toWaitTimeError()) }
                    }
            } finally {
                // Runs on cancellation too, so the spinner always clears.
                fetchState.update { it.copy(isRequestInFlight = false) }
            }
        }
    }

    /** Called when the screen becomes visible again; a no-op if the cached data is still fresh. */
    fun refreshIfStale() = refresh(force = false)

    fun toggleSortDirection() {
        val preferences = preferencesRepository.preferences.value
        setSort(preferences.sortType, preferences.sortDirection.opposite)
    }

    fun changeSortType(type: SortType) {
        setSort(type, preferencesRepository.preferences.value.sortDirection)
    }

    private fun setSort(type: SortType, direction: SortDirection) {
        viewModelScope.launch { preferencesRepository.setSort(type, direction) }
    }

    fun toggleFavorite(code: String) {
        viewModelScope.launch { preferencesRepository.toggleFavorite(code) }
    }

    fun setFilterOnlyOpen(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFilterOnlyOpen(enabled) }
    }

    fun addAlert(attraction: AttractionWaitTime, targetMinutes: Int) {
        viewModelScope.launch {
            alertRepository.addAlert(
                WaitTimeAlert(
                    attractionCode = attraction.code,
                    attractionName = attraction.name,
                    targetMinutes = targetMinutes,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeAlert(attractionCode: String) {
        viewModelScope.launch { alertRepository.removeAlert(attractionCode) }
    }

    private fun buildUiState(
        fetch: FetchState,
        preferences: UserPreferences,
        alerts: List<WaitTimeAlert>
    ): WaitTimeUiState {
        val hasData = fetch.snapshot != null
        val all = fetch.snapshot?.waitTimes.orEmpty()
        val visible = sortAttractions(
            attractions = filterAttractions(all, preferences.filterOnlyOpen),
            sortType = preferences.sortType,
            direction = preferences.sortDirection,
            favoriteCodes = preferences.favoriteCodes
        )

        return WaitTimeUiState(
            // A request with nothing on screen yet is a first load; one on top of existing data
            // is a refresh and only drives the pull-to-refresh indicator.
            isLoading = fetch.isRequestInFlight && !hasData,
            isRefreshing = fetch.isRequestInFlight && hasData,
            waitTimes = visible,
            error = fetch.error,
            lastUpdated = fetch.snapshot?.fetchedAt ?: 0L,
            isOfflineData = fetch.snapshot?.isFromCache == true,
            sortType = preferences.sortType,
            sortDirection = preferences.sortDirection,
            favoriteCodes = preferences.favoriteCodes,
            filterOnlyOpen = preferences.filterOnlyOpen,
            activeAlerts = alerts,
            activeAlertCodes = alerts.mapTo(mutableSetOf()) { it.attractionCode },
            totalCount = all.size,
            openCount = all.count { it.isOpen }
        )
    }

    private fun Throwable.toWaitTimeError(): WaitTimeError = when (this) {
        is UnknownHostException -> WaitTimeError.NO_CONNECTION
        is SocketTimeoutException -> WaitTimeError.TIMEOUT
        is HttpException -> WaitTimeError.SERVER
        is IOException -> WaitTimeError.NO_CONNECTION
        else -> WaitTimeError.UNKNOWN
    }

    private companion object {
        /** Keeps the derived state alive across configuration changes without leaking. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
