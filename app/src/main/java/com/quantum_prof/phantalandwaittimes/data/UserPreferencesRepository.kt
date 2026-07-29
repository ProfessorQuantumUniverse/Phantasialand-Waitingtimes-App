package com.quantum_prof.phantalandwaittimes.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.quantum_prof.phantalandwaittimes.di.ApplicationScope
import com.quantum_prof.phantalandwaittimes.di.IoDispatcher
import com.quantum_prof.phantalandwaittimes.di.StorageModule
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortDirection
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the user has configured on the list screen, persisted across restarts. */
data class UserPreferences(
    val favoriteCodes: Set<String> = emptySet(),
    val sortType: SortType = SortType.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val filterOnlyOpen: Boolean = false
)

/**
 * Reads and writes [UserPreferences].
 *
 * Note on favourites: the set returned by [SharedPreferences.getStringSet] must never be mutated
 * in place — the platform hands back its own instance and the change would not be persisted, so
 * every update below builds a new set.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope appScope: CoroutineScope
) {

    private val mutex = Mutex()

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    init {
        // Loaded off the main thread; the flow starts at the defaults and fills in shortly after.
        appScope.launch { refresh() }
    }

    suspend fun refresh(): UserPreferences = withContext(ioDispatcher) {
        mutex.withLock {
            readFromDisk().also { _preferences.value = it }
        }
    }

    /** Adds [code] to the favourites if absent, removes it otherwise. */
    suspend fun toggleFavorite(code: String) {
        if (code.isBlank()) return
        update { current ->
            val favorites = current.favoriteCodes
            current.copy(
                favoriteCodes = if (code in favorites) favorites - code else favorites + code
            )
        }
    }

    suspend fun setSort(type: SortType, direction: SortDirection) {
        update { it.copy(sortType = type, sortDirection = direction) }
    }

    suspend fun setFilterOnlyOpen(enabled: Boolean) {
        update { it.copy(filterOnlyOpen = enabled) }
    }

    private suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        withContext(ioDispatcher) {
            mutex.withLock {
                val updated = transform(readFromDisk())
                sharedPreferences.edit {
                    putStringSet(StorageModule.KEY_FAVORITE_CODES, updated.favoriteCodes)
                    putString(StorageModule.KEY_SORT_TYPE, updated.sortType.name)
                    putString(StorageModule.KEY_SORT_DIRECTION, updated.sortDirection.name)
                    putBoolean(StorageModule.KEY_FILTER_ONLY_OPEN, updated.filterOnlyOpen)
                }
                _preferences.value = updated
            }
        }
    }

    private fun readFromDisk(): UserPreferences = UserPreferences(
        favoriteCodes = sharedPreferences
            .getStringSet(StorageModule.KEY_FAVORITE_CODES, null)
            ?.filterTo(mutableSetOf()) { it.isNotBlank() }
            ?: emptySet(),
        sortType = SortType.fromName(
            sharedPreferences.getString(StorageModule.KEY_SORT_TYPE, null)
        ),
        sortDirection = SortDirection.fromName(
            sharedPreferences.getString(StorageModule.KEY_SORT_DIRECTION, null)
        ),
        filterOnlyOpen = sharedPreferences.getBoolean(StorageModule.KEY_FILTER_ONLY_OPEN, false)
    )
}
