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

    fun toggleSortDirection() {
        // Bestimme die neue Richtung
        val newDirection = if (_uiState.value.currentSortDirection == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }

        // Update die Richtung im State *zuerst*
        _uiState.update { it.copy(currentSortDirection = newDirection) }

        // Wende Filter und die *neue* Sortierung (mit dem *alten* Typ)
        // auf die *aktuell angezeigte* Liste an
        applyFiltersAndSorting(_uiState.value.waitTimes) // Einfacher Ansatz
        // applyFiltersAndSorting(originalFetchedWaitTimes) // Besserer Ansatz
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
        applyFiltersAndSorting(_uiState.value.waitTimes) // Einfacher Ansatz
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




    // --- GEÄNDERT: Private Hilfsfunktion zum Anwenden der Sortierung (umbenannt) ---
    // Wird jetzt von applyFiltersAndSorting aufgerufen
    private fun applySortingInternal(
        list: List<AttractionWaitTime>,
        sortType: SortType,
        sortDirection: SortDirection,
        favorites: Set<String> // Set der Favoriten-Codes
    ): List<AttractionWaitTime> {

        // 1. Primärer Comparator: Favoriten immer zuerst
        //    compareByDescending: true (ist Favorit) wird als "größer" betrachtet und kommt daher bei DESC zuerst.
        val favoritesComparator = compareByDescending<AttractionWaitTime> { it.code in favorites }

        // 2. Sekundärer Comparator: Basierend auf der Benutzerauswahl (Name oder Wartezeit)
        val secondaryComparator: Comparator<AttractionWaitTime> = when (sortType) {
            SortType.NAME -> {
                // Sortiere nach Name:
                // - nullsLast(): Namenlose Einträge ans Ende.
                // - String.CASE_INSENSITIVE_ORDER: Ignoriere Groß/Kleinschreibung.
                compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { it.name }
            }
            SortType.WAIT_TIME -> {
                // Sortiere nach Wartezeit:
                compareBy<AttractionWaitTime, Int?>(nullsLast()) { attraction ->
                    // Behandle nicht-geöffnete oder null-Zeit-Attraktionen für die Sortierung:
                    // Setze ihre "effektive" Wartezeit auf einen sehr hohen Wert, damit sie bei ASC ans Ende kommen.
                    if (attraction.status?.lowercase(Locale.GERMANY) == "opened" && attraction.waitTimeMinutes != null) {
                        attraction.waitTimeMinutes
                    } else {
                        Int.MAX_VALUE // Geschlossene/unbekannte/null-Zeit Attraktionen gelten als "längste" Wartezeit
                    }
                }.thenBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
                    // Bei gleicher effektiver Wartezeit (z.B. alle geschlossenen), sortiere nach Name
                    it.name
                }
            }
            // Zukünftige Sortieroptionen könnten hier hinzugefügt werden
            // z.B. SortType.FAVORITE (obwohl Favoriten bereits primär behandelt werden)
        }

        // 3. Kombiniere die Comparators: Erst nach Favorit, dann nach dem sekundären Kriterium
        val finalComparator = favoritesComparator.thenComparing(secondaryComparator)

        // 4. Wende die Sortierung an
        return if (sortDirection == SortDirection.ASCENDING) {
            // Aufsteigend: Favoriten zuerst, dann nach sekundärem Kriterium aufsteigend
            list.sortedWith(finalComparator)
        } else {
            // Absteigend: Favoriten bleiben zuerst, aber die sekundäre Sortierung wird umgedreht.
            // Wir erstellen einen neuen Comparator, der Favoriten priorisiert und dann absteigend sortiert.
            val descendingComparator = favoritesComparator.thenComparing(secondaryComparator.reversed())
            list.sortedWith(descendingComparator)

            // Einfachere Alternative, falls das Verhalten "alles umdrehen" (auch Favoriten nach unten) akzeptabel wäre:
            // list.sortedWith(finalComparator.reversed())
            // Die obere Lösung (mit thenComparing(secondaryComparator.reversed())) ist aber meistens das, was man will.
        }
    }

    // Stelle sicher, dass applyFiltersAndSorting diese Funktion korrekt aufruft:
    private fun applyFiltersAndSorting(sourceList: List<AttractionWaitTime>) {
        val filteredList = if (_uiState.value.filterOnlyOpen) {
            sourceList.filter { it.status?.lowercase(Locale.GERMANY) == "opened" }
        } else {
            sourceList
        }

        val sortedAndFilteredList = applySortingInternal( // Rufe die überarbeitete Funktion auf
            filteredList,
            _uiState.value.currentSortType,
            _uiState.value.currentSortDirection,
            _uiState.value.favoriteCodes // Übergabe der Favoriten ist entscheidend!
        )

        _uiState.update { it.copy(waitTimes = sortedAndFilteredList) }
    }
}

