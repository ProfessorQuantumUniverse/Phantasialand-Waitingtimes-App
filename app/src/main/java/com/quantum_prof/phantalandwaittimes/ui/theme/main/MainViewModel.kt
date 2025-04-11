package com.quantum_prof.phantalandwaittimes.ui.theme.main // Korrigierter Paketname (ohne .theme)

import android.content.SharedPreferences // NEU: Import für SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.data.WaitTimeRepository
import com.quantum_prof.phantalandwaittimes.data.WaitTimeResult
// NEU: Import für den SharedPreferences Key (Pfad ggf. anpassen)
import com.quantum_prof.phantalandwaittimes.di.StorageModule.KEY_FAVORITE_CODES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale // Import für CASE_INSENSITIVE_ORDER (bereits vorhanden)

// Enums für Sortierung (bereits vorhanden)
// enum class SortType { NAME, WAIT_TIME } // Optional: FAVORITE hinzufügen
// enum class SortDirection { ASCENDING, DESCENDING }

// --- GEÄNDERT: WaitTimeUiState erweitert ---
data class WaitTimeUiState(
    val isLoading: Boolean = false,
    val waitTimes: List<AttractionWaitTime> = emptyList(),
    val error: String? = null,
    val lastUpdated: Long = 0L, // Zeitpunkt der letzten erfolgreichen Aktualisierung
    val currentSortType: SortType = SortType.NAME, // Standard-Sortierung
    val currentSortDirection: SortDirection = SortDirection.ASCENDING, // Standard-Richtung
    val isOfflineData: Boolean = false, // Flag für Offline-Daten
    // --- NEU: Status für Favoriten und Filter ---
    val favoriteCodes: Set<String> = emptySet(), // Set der Favoriten-Attraktionscodes
    val filterOnlyOpen: Boolean = false // Flag, ob nur geöffnete Attraktionen angezeigt werden
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WaitTimeRepository,
    // --- NEU: SharedPreferences injecten ---
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaitTimeUiState())
    val uiState: StateFlow<WaitTimeUiState> = _uiState.asStateFlow()

    // Optional: Zwischenspeicher für die *originalen*, ungefilterten/unsortierten Daten
    // private var originalFetchedWaitTimes: List<AttractionWaitTime> = emptyList()

    init {
        loadFavorites() // Lade Favoriten ZUERST
        fetchWaitTimes() // Dann lade die Wartezeiten (wendet initiale Filter/Sortierung an)
    }

    // --- NEU: Funktion zum Laden der Favoriten aus SharedPreferences ---
    private fun loadFavorites() {
        val favorites = sharedPreferences.getStringSet(KEY_FAVORITE_CODES, emptySet()) ?: emptySet()
        _uiState.update { it.copy(favoriteCodes = favorites) }
    }

    // --- NEU: Funktion zum Umschalten eines Favoriten ---
    fun toggleFavorite(code: String) {
        val currentFavorites = _uiState.value.favoriteCodes
        val newFavorites = if (currentFavorites.contains(code)) {
            currentFavorites - code // Entfernen
        } else {
            currentFavorites + code // Hinzufügen
        }

        // Speichere in SharedPreferences
        sharedPreferences.edit().putStringSet(KEY_FAVORITE_CODES, newFavorites).apply()

        // Aktualisiere den UI State mit den neuen Favoriten
        _uiState.update { it.copy(favoriteCodes = newFavorites) }

        // Optional: Wenn Favoriten die Sortierung beeinflussen sollen, hier neu filtern/sortieren.
        // Die aktuelle applySorting berücksichtigt Favoriten noch nicht explizit.
        // Falls applySorting erweitert wird, hier aufrufen:
        // applyFiltersAndSorting(_uiState.value.waitTimes) // Einfacher Ansatz
        // applyFiltersAndSorting(originalFetchedWaitTimes) // Besserer Ansatz
    }

    // --- NEU: Funktion zum Setzen des Filters für offene Attraktionen ---
    fun setFilterOnlyOpen(enabled: Boolean) {
        // Nur fortfahren, wenn sich der Wert ändert
        if (enabled == _uiState.value.filterOnlyOpen) return

        // Update den Filter-Status im State
        _uiState.update { it.copy(filterOnlyOpen = enabled) }

        // Wende Filter (und Sortierung) auf die aktuell geladenen Daten neu an
        // Besser wäre es, die Originaldaten zu verwenden, falls zwischengespeichert.
        applyFiltersAndSorting(_uiState.value.waitTimes) // Einfacher Ansatz: Aktuelle Liste neu verarbeiten
        // applyFiltersAndSorting(originalFetchedWaitTimes) // Besserer Ansatz
    }


    fun fetchWaitTimes(isRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !isRefresh) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = if (isRefresh) null else it.error) } // Fehler nur bei explizitem Refresh löschen

            val result: Result<WaitTimeResult> = repository.getPhantasialandWaitTimes()

            result.onSuccess { (times, isFromCache) ->
                // Speichere die Originaldaten (optional, aber gut für reines Filtern/Sortieren)
                // originalFetchedWaitTimes = times

                // Wende aktuelle Filter UND Sortierung auf die NEUEN Daten an
                // Diese Funktion aktualisiert auch den waitTimes-Teil des States
                applyFiltersAndSorting(times)

                // Update restlichen State (isLoading, Offline-Status, Fehler, Zeitstempel)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOfflineData = isFromCache,
                        // Fehler nur löschen, wenn Daten frisch von API kamen
                        error = if (!isFromCache) null else it.error,
                        // Aktualisiere Timestamp nur bei frischen Daten oder wenn noch keiner gesetzt
                        lastUpdated = if (!isFromCache || it.lastUpdated == 0L) System.currentTimeMillis() else it.lastUpdated
                        // waitTimes wird bereits durch applyFiltersAndSorting gesetzt
                        // favoriteCodes bleiben unverändert (werden separat verwaltet)
                        // filterOnlyOpen bleibt unverändert (wird separat verwaltet)
                    )
                }

            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Laden fehlgeschlagen: ${throwable.localizedMessage ?: "Unbekannter Fehler"}",
                        // Behalte alte Daten bei, markiere sie aber als offline, falls vorhanden
                        isOfflineData = it.waitTimes.isNotEmpty()
                    )
                }
            }
        }
    }

    // --- GEÄNDERT: changeSortOrder wendet Filterung & Sortierung neu an ---
    fun changeSortOrder(newType: SortType, newDirection: SortDirection) {
        if (newType == _uiState.value.currentSortType && newDirection == _uiState.value.currentSortDirection) {
            return
        }

        // Update die Sortierparameter im State *zuerst*
        _uiState.update { it.copy(currentSortType = newType, currentSortDirection = newDirection) }

        // Wende Filter und die *neue* Sortierung auf die *aktuell angezeigte* Liste an
        // (Oder besser: auf die originalen Daten, falls vorhanden)
        applyFiltersAndSorting(_uiState.value.waitTimes) // Einfacher Ansatz
        // applyFiltersAndSorting(originalFetchedWaitTimes) // Besserer Ansatz
    }

    // --- NEU: Zentrale Hilfsfunktion zum Anwenden von Filter UND Sortierung ---
    private fun applyFiltersAndSorting(sourceList: List<AttractionWaitTime>) {
        // 1. Filtern (basierend auf dem aktuellen State)
        val filteredList = if (_uiState.value.filterOnlyOpen) {
            sourceList.filter { it.status.lowercase() == "opened" }
        } else {
            sourceList // Kein Filter angewendet
        }

        // 2. Sortieren (basierend auf dem aktuellen State)
        val sortedAndFilteredList = applySortingInternal( // Umbenannt, um Verwechslung zu vermeiden
            filteredList,
            _uiState.value.currentSortType,
            _uiState.value.currentSortDirection
            // Optional: Favoriten hier übergeben, wenn applySortingInternal sie berücksichtigt
            // _uiState.value.favoriteCodes
        )

        // 3. Aktualisiere nur die Liste im UI State
        // Die anderen Parameter (isLoading, error, etc.) werden in fetchWaitTimes oder anderen Funktionen gesetzt
        _uiState.update { it.copy(waitTimes = sortedAndFilteredList) }
    }


    // --- GEÄNDERT: Private Hilfsfunktion zum Anwenden der Sortierung (umbenannt) ---
    // Wird jetzt von applyFiltersAndSorting aufgerufen
    private fun applySortingInternal(
        list: List<AttractionWaitTime>,
        sortType: SortType,
        sortDirection: SortDirection
        // Optional: Füge hier `favorites: Set<String>` hinzu, falls benötigt
    ): List<AttractionWaitTime> {

        // Aktuelle Sortierlogik (ohne Favoriten-Priorisierung)
        val comparator = when (sortType) {
            SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortType.WAIT_TIME -> compareBy<AttractionWaitTime> {
                // Behandle geschlossene/Wartungs-Attraktionen (z.B. ans Ende sortieren bei ASC)
                when (it.status.lowercase()) {
                    "opened" -> it.waitTimeMinutes
                    else -> Int.MAX_VALUE // Setze geschlossene/andere ans Ende bei ASC
                }
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name } // Bei gleicher Zeit nach Name
            // TODO: Ggf. SortType.FAVORITE hinzufügen
        }

        // Beispiel: Favoriten zuerst (nur bei aufsteigender Sortierung nach Name oder Zeit)
        // val combinedComparator = if (sortDirection == SortDirection.ASCENDING && favorites.isNotEmpty()) {
        //     compareByDescending<AttractionWaitTime> { it.code in favorites } // Favoriten zuerst (true > false)
        //         .thenComparing(comparator) // Dann nach dem eigentlichen Kriterium
        // } else {
        //      comparator // Standard-Sortierung oder wenn absteigend
        // }
        // val finalComparator = combinedComparator // Oder nur comparator, wenn keine Favoriten-Sortierung

        return if (sortDirection == SortDirection.ASCENDING) {
            list.sortedWith(comparator) // oder finalComparator
        } else {
            // Bei DESC-Sortierung für Wartezeit: Geschlossene sollen *zuletzt* kommen
            if (sortType == SortType.WAIT_TIME) {
                val descComparator = compareBy<AttractionWaitTime> {
                    when (it.status.lowercase()) {
                        "opened" -> it.waitTimeMinutes
                        else -> -1 // Setze geschlossene/andere an den Anfang bei DESC, damit reversed() sie ans Ende stellt
                    }
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                list.sortedWith(descComparator.reversed())
            } else {
                list.sortedWith(comparator.reversed()) // oder finalComparator.reversed()
            }
        }
    }
}

