package com.quantum_prof.phantalandwaittimes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward // NEU: Import für Sortierrichtung
import androidx.compose.material.icons.filled.ArrowUpward   // NEU: Import für Sortierrichtung
import androidx.compose.material.icons.filled.MoreVert     // NEU: Import für 3-Punkte-Menü
import androidx.compose.material.icons.filled.Star         // NEU: Import für gefüllten Stern
import androidx.compose.material.icons.outlined.StarBorder // NEU: Import für leeren Stern
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // NEU: Import für Bildskalierung
import androidx.compose.ui.res.painterResource // NEU: Import für Drawable-Ressourcen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import com.quantum_prof.phantalandwaittimes.ui.theme.PhantasialandWaitTimesTheme
// Importiere ViewModel und UiState (Pfad ggf. anpassen)
import com.quantum_prof.phantalandwaittimes.ui.theme.main.MainViewModel
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortDirection
import com.quantum_prof.phantalandwaittimes.ui.theme.main.SortType
import com.quantum_prof.phantalandwaittimes.ui.theme.main.WaitTimeUiState
// Importiere die Farbdefinitionen
import com.quantum_prof.phantalandwaittimes.ui.theme.WaitTimeLong
import com.quantum_prof.phantalandwaittimes.ui.theme.WaitTimeMedium
import com.quantum_prof.phantalandwaittimes.ui.theme.WaitTimeShort
import com.quantum_prof.phantalandwaittimes.ui.theme.WaitTimeVeryLong
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow // Für DummyViewModel in Preview
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.quantum_prof.phantalandwaittimes.R // <-- ERSETZE mit deinem echten Paketnamen!
import androidx.compose.material.icons.filled.ArrowDownward // Pfeil nach unten
import androidx.compose.material.icons.filled.ArrowUpward   // Pfeil nach oben
import androidx.compose.material.icons.filled.Sort

@AndroidEntryPoint // Hilt Einstiegspunkt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Kann aktiviert werden für Vollbild-Layout
        setContent {
            PhantasialandWaitTimesTheme { // Dein Material You Theme
                WaitTimeApp()
            }
        }
    }
}

// --- NEU: Hilfsfunktion für Icon Mapping ---
@DrawableRes // Zeigt an, dass die Funktion eine Drawable Resource ID zurückgibt
fun getAttractionIconResId(code: String): Int {
    // Hier Logik hinzufügen, um Code oder Namen zu mappen
    // TODO: Diese Zuordnung MUSS überprüft und an die ECHTEN Phantasialand-Codes angepasst werden!
    return when (code) {
        // Hauptachterbahnen (Beispielcodes - ANPASSEN!)
        "3136", "3137", "3532", "3235", "3630", "3539", "3733" -> R.drawable.ic_coaster // Taron, Raik, Mamba, Colorado, Winjas, Crazy Bats, F.L.Y.
        // Wasserbahnen (Beispielcodes - ANPASSEN!)
        "3238", "3139", "3735" -> R.drawable.ic_waterride // Chiapas, River Quest, Wakobato
        // Shows / Indoor Dark Rides (Beispielcodes - ANPASSEN!)
        "34", "3431", "3432" -> R.drawable.ic_show // Maus, Feng Ju, Geister Rikscha
        // Kinderfahrten / Ruhigere Fahrten (Beispiele - ANPASSEN!)
        "31", "32", "33", "35", "3632", "3633", "3634", "3635", "3638", "3730", "3731", "3732" -> R.drawable.ic_kid_ride // Viele kleine Attraktionen
        // Default für alles andere
        else -> R.drawable.ic_default_ride // Fallback-Icon
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitTimeApp(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // Zustand für die Sichtbarkeit des Sortiermenüs
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phantasialand waiting times") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // --- NEU: Button zum Umschalten der Richtung ---
                    IconButton(onClick = { viewModel.toggleSortDirection() }) { // Ruft neue VM-Funktion auf
                        Icon(
                            // Wählt Icon basierend auf aktueller Richtung
                            imageVector = if (uiState.currentSortDirection == SortDirection.ASCENDING)
                                Icons.Filled.ArrowUpward // Pfeil hoch bei ASC
                            else
                                Icons.Filled.ArrowDownward, // Pfeil runter bei DESC
                            contentDescription = "Sortierrichtung wechseln"
                        )
                    }
                    // --- ENDE Neuer Button ---

                    // --- Bestehender Button für SortierTYP-Auswahl ---
                    Box { // Box als Anker für das DropdownMenu
                        IconButton(onClick = { showSortMenu = true }) { // Öffnet das Menü
                            Icon(
                                imageVector = Icons.Filled.Sort, // Icon bleibt gleich
                                contentDescription = "Sortierkriterium wählen" // Beschreibung angepasst
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            // Menüpunkte ändern NUR den Typ, Richtung wird vom anderen Button gesteuert
                            // (Die changeSortOrder Funktion behält die aktuelle Richtung bei)
                            DropdownMenuItem(
                                text = { Text("Name") }, // Nur noch Typ auswählen
                                onClick = {
                                    // Setzt Typ auf NAME, behält aktuelle Richtung bei
                                    viewModel.changeSortOrder(SortType.NAME, uiState.currentSortDirection)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Wartezeit") }, // Nur noch Typ auswählen
                                onClick = {
                                    // Setzt Typ auf WAIT_TIME, behält aktuelle Richtung bei
                                    viewModel.changeSortOrder(SortType.WAIT_TIME, uiState.currentSortDirection)
                                    showSortMenu = false
                                }
                            )
                            // Die alten Menüpunkte (Name Z-A, Wartezeit längste) werden NICHT mehr benötigt,
                            // da die Richtung separat umgeschaltet wird.
                        }
                    }
                    // --- ENDE Bestehender Button ---
                }
            )
        }
    ) { paddingValues ->
        // Der Aufruf von WaitTimeControl bleibt unverändert
        WaitTimeControl(
            uiState = uiState,
            onRefresh = { viewModel.fetchWaitTimes(isRefresh = true) },
            onFavoriteToggle = { code -> viewModel.toggleFavorite(code) },
            onFilterOnlyOpenChanged = { enabled -> viewModel.setFilterOnlyOpen(enabled) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun WaitTimeControl(
    uiState: WaitTimeUiState,
    onRefresh: () -> Unit,
    // --- HINZUGEFÜGT: Callbacks ---
    onFavoriteToggle: (String) -> Unit,
    onFilterOnlyOpenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Zeige Refresh-Indikator nur, wenn aktiv geladen wird UND schon Daten da sind (Pull-to-Refresh)
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading && uiState.waitTimes.isNotEmpty())

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        // --- HINZUGEFÜGT: Box für Hintergrund und Inhalt ---
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Hintergrundbild
            Image(
                painter = painterResource(id = R.drawable.background_park), // DEIN BILDNAME HIER
                contentDescription = "Park Hintergrund",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Bild zuschneiden, um den Bereich zu füllen
            )

            // 2. Semi-transparenter Overlay für Lesbarkeit
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Nutze Hintergrundfarbe des Themes mit reduzierter Deckkraft
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
            )

            // 3. Inhalt (Filter, Liste, Fehler, Laden) kommt jetzt hier rein
            Box(modifier = Modifier.fillMaxSize()) { // Innere Box für Inhalt

                // --- GEÄNDERT: Column für Filter + Rest ---
                Column(modifier = Modifier.fillMaxSize()) {

                    // --- HINZUGEFÜGT: Filter Controls ---
                    // Nur anzeigen, wenn es Daten gibt oder potenziell geben könnte
                    if (uiState.waitTimes.isNotEmpty() || (uiState.error == null && !uiState.isLoading)) {
                        FilterControls(
                            filterOnlyOpen = uiState.filterOnlyOpen,
                            onFilterOnlyOpenChanged = onFilterOnlyOpenChanged
                        )
                    }
                    // --- ENDE Filter Controls ---

                    // Fehler anzeigen (nur wenn keine Daten da sind und nicht initial geladen wird)
                    if (uiState.error != null && uiState.waitTimes.isEmpty() && !uiState.isLoading) {
                        ErrorView(
                            errorMessage = uiState.error,
                            onRetry = onRefresh,
                            modifier = Modifier.weight(1f).padding(16.dp) // Füllt restlichen Platz
                        )
                    }
                    // Hauptinhalt (Liste oder Leere-Ansicht)
                    else {
                        // Leere Ansicht (Keine Daten, kein Laden, kein Fehler)
                        if (uiState.waitTimes.isEmpty() && !uiState.isLoading && uiState.error == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f).padding(16.dp) // Füllt restlichen Platz
                            ) {
                                Text(
                                    // Text anpassen je nach Filterstatus
                                    text = if (uiState.filterOnlyOpen) "No open attractions found." else "No waitingtimes available",
                                    textAlign = TextAlign.Center,
                                    // Textfarbe für Kontrast auf Overlay
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // Liste anzeigen (wenn Daten vorhanden)
                        else if (uiState.waitTimes.isNotEmpty()) {
                            WaitTimeList(
                                waitTimes = uiState.waitTimes,
                                lastUpdated = uiState.lastUpdated,
                                isOffline = uiState.isOfflineData,
                                showErrorSnackbar = uiState.error != null && !uiState.isOfflineData, // Snackbar nur bei Online-Fehler nach erfolgreichem Laden
                                // --- HINZUGEFÜGT: Favoriten & Callback übergeben ---
                                favoriteCodes = uiState.favoriteCodes,
                                onFavoriteToggle = onFavoriteToggle,
                                modifier = Modifier.weight(1f) // Füllt restlichen Platz
                            )
                        }
                        // Initialer Ladeindikator (im leeren Zustand)
                        else if (uiState.isLoading && uiState.waitTimes.isEmpty() && uiState.error == null) {
                            Box(
                                modifier = Modifier.weight(1f), // Füllt restlichen Platz
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        // Fallback für leeren Raum (sollte selten auftreten)
                        else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } // Ende Column
            } // Ende innerer Content-Box
        } // Ende äußerer Hintergrund-Box
    } // Ende SwipeRefresh
}

// --- NEU: Composable für Filter-Steuerelemente ---
@Composable
fun FilterControls(
    filterOnlyOpen: Boolean,
    onFilterOnlyOpenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End // Schalter nach rechts
    ) {
        Text(
            text = "Show only opened",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f), // Nimmt Platz links
            color = MaterialTheme.colorScheme.onSurface // Kontrast zum Overlay
        )
        Switch(
            checked = filterOnlyOpen,
            onCheckedChange = onFilterOnlyOpenChanged,
            colors = SwitchDefaults.colors( // Optionale Farbgebung
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                checkedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun WaitTimeList(
    waitTimes: List<AttractionWaitTime>,
    lastUpdated: Long,
    isOffline: Boolean,
    showErrorSnackbar: Boolean,
    // --- HINZUGEFÜGT: Parameter für Favoriten ---
    favoriteCodes: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Zeige Snackbar bei Fehler im Hintergrund (unverändert)
    LaunchedEffect(showErrorSnackbar) {
        if (showErrorSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Error loading data",
                duration = SnackbarDuration.Short
            )
        }
    }

    // --- GEÄNDERT: Innerer Scaffold mit transparenter Farbe ---
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent // Wichtig, damit der Hintergrund durchscheint
    ) { innerPadding ->
        LazyColumn(
            // Padding vom inneren Scaffold übernehmen + zusätzliches Padding
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp, // Platz für Header etc.
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Abstand zwischen Elementen
        ) {
            // Header für "Zuletzt aktualisiert" / Offline-Hinweis
            if (lastUpdated > 0) {
                item {
                    LastUpdatedHeader(timestamp = lastUpdated, isOffline = isOffline)
                    // Kein Spacer hier, da Arrangement.spacedBy verwendet wird
                }
            }

            // --- GEÄNDERT: Wartezeit-Items mit Favoriten-Info ---
            items(waitTimes, key = { it.code }) { attraction ->
                WaitTimeItem(
                    attraction = attraction,
                    isFavorite = attraction.code in favoriteCodes, // Prüfe, ob Favorit
                    onFavoriteToggle = onFavoriteToggle // Callback weitergeben
                )
            }
        }
    }
}

// Behalte deine detaillierte LastUpdatedHeader-Logik bei, nur der Offline-Teil wird genutzt
@Composable
fun LastUpdatedHeader(timestamp: Long, isOffline: Boolean) {
    val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timestamp)
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(timestamp))
    val dateFormatted = SimpleDateFormat("dd.MM.", Locale.GERMANY).format(Date(timestamp))

    val ageText = when {
        minutesAgo < 1 -> "Just right now"
        minutesAgo == 1L -> "1 minute ago"
        minutesAgo < 60 -> " $minutesAgo minutes ago"
        minutesAgo < 120 -> "1 hour ago"
        minutesAgo < (24 * 60) -> "Ca. ${TimeUnit.MINUTES.toHours(minutesAgo)} hours ago"
        else -> "on $dateFormatted"
    }

    val prefix = if (isOffline) "Offline-Data" else "Refreshed"
    val fullText = "$prefix $ageText ($timeFormatted )"

    Text(
        text = fullText,
        style = MaterialTheme.typography.labelSmall,
        color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        textAlign = TextAlign.Center
    )
}


@Composable
fun WaitTimeItem(
    attraction: AttractionWaitTime,
    // --- HINZUGEFÜGT: Parameter für Favoriten ---
    isFavorite: Boolean,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Etwas mehr Schatten
        // Leicht transparente Karte, um den Hintergrund durchscheinen zu lassen
        // Unterschiedliche Transparenz für geschlossene Elemente (optional)
        colors = CardDefaults.cardColors(
            containerColor = (if (attraction.status.lowercase() == "closed") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                .copy(alpha = 0.85f) // Anpassen nach Geschmack
        )
    ) {
        // --- GEÄNDERT: Row Layout für Icon, Name, Zeit, Favorit ---
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp) // Horizontal etwas weniger Padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Von links beginnen
        ) {
            // --- HINZUGEFÜGT: Icon für Attraktionstyp ---
            Icon(
                painter = painterResource(id = getAttractionIconResId(attraction.code)),
                contentDescription = "Attraction Type",
                // Farbe nach Geschmack anpassen, z.B. Primary oder OnSurfaceVariant
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp) // Größe des Icons
            )
            Spacer(modifier = Modifier.width(10.dp)) // Abstand Icon <-> Name

            // Name (nimmt flexiblen Platz ein)
            Text(
                text = attraction.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f) // Nimmt verfügbaren Platz
            )
            Spacer(modifier = Modifier.width(8.dp)) // Abstand Name <-> Zeit

            // Wartezeit-Status (deine bestehende Logik)
            WaitTimeStatusText(attraction = attraction)

            // --- HINZUGEFÜGT: Favoriten-Button ---
            IconButton(
                onClick = { onFavoriteToggle(attraction.code) },
                modifier = Modifier
                    .size(36.dp) // Kleinere Klickfläche als Standard
                    .padding(start = 4.dp) // Abstand Zeit <-> Stern
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Remove as Favorit" else "Add as Favorit",
                    // Farbe für den Stern anpassen
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
            }
            // --- ENDE Favoriten-Button ---
        }
    }
}

// Deine bestehende Logik für die Wartezeit-Anzeige und Farben
@Composable
fun WaitTimeStatusText(attraction: AttractionWaitTime, modifier: Modifier = Modifier) {
    val statusText: String
    val fontWeight: FontWeight
    val textColor: Color

    when (attraction.status.lowercase()) {
        "opened" -> {
            statusText = "${attraction.waitTimeMinutes} Min."
            fontWeight = FontWeight.Bold
            textColor = when {
                attraction.waitTimeMinutes >= 75 -> WaitTimeVeryLong
                attraction.waitTimeMinutes >= 50 -> WaitTimeLong
                attraction.waitTimeMinutes >= 25 -> WaitTimeMedium
                else -> WaitTimeShort
            }
        }
        "closed" -> {
            statusText = "Closed" // Kürzer für mehr Platz
            fontWeight = FontWeight.Normal
            textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Etwas ausgegraut
        }
        // Optional: Bessere Darstellung für andere Status (z.B. Wartung)
        "refurbishment" -> {
            statusText = "Maintenance"
            fontWeight = FontWeight.Normal
            textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }
        else -> { // Fallback
            statusText = attraction.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMANY) else it.toString() }
            fontWeight = FontWeight.Normal
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = fontWeight,
        color = textColor,
        textAlign = TextAlign.End,
        modifier = modifier.widthIn(min = 55.dp) // Mindestbreite für bessere Ausrichtung
    )
}

// Deine bestehende ErrorView
@Composable
fun ErrorView(errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(), // Nimmt normalerweise Platz über .weight(1f)
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Oops!", // Angepasster Titel
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer // Besserer Kontrast bei Error-Container
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

// --- Hilfsfunktionen ---

// Helper zum Umschalten der Sortierrichtung (aus Anleitung übernommen)
fun SortDirection.toggle(): SortDirection {
    return if (this == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
}

// --- PREVIEWS (angepasst für neue Features) ---

// Beispieldaten für Previews
val previewSampleData = listOf(
    AttractionWaitTime(code = "3136", name = "Taron", waitTimeMinutes = 80, status = "opened"),
    AttractionWaitTime(code = "3532", name = "Black Mamba", waitTimeMinutes = 35, status = "opened"),
    AttractionWaitTime(code = "3238", name = "Chiapas", waitTimeMinutes = 0, status = "closed"),
    AttractionWaitTime(code = "34", name = "Maus au Chocolat", waitTimeMinutes = 45, status = "opened"),
    AttractionWaitTime(code = "3139", name = "River Quest", waitTimeMinutes = 999, status = "refurbishment"), // Beispiel Wartung
)
val previewSampleFavorites = setOf("3136", "34") // Taron & Maus sind Favoriten

@Preview(showBackground = true, name = "Item - Favorit")
@Composable
fun WaitTimeItemPreviewFavorite() {
    PhantasialandWaitTimesTheme {
        WaitTimeItem(
            attraction = previewSampleData[0], // Taron
            isFavorite = true,
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, name = "Item - Nicht Favorit")
@Composable
fun WaitTimeItemPreviewNotFavorite() {
    PhantasialandWaitTimesTheme {
        WaitTimeItem(
            attraction = previewSampleData[1], // Black Mamba
            isFavorite = false,
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, name = "Item - Geschlossen")
@Composable
fun WaitTimeItemPreviewClosed() {
    PhantasialandWaitTimesTheme {
        WaitTimeItem(
            attraction = previewSampleData[2], // Chiapas
            isFavorite = false,
            onFavoriteToggle = {}
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Liste Online")
@Composable
fun WaitTimeListOnlinePreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeList(
            waitTimes = previewSampleData,
            lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5),
            isOffline = false,
            showErrorSnackbar = false,
            favoriteCodes = previewSampleFavorites,
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Liste Offline")
@Composable
fun WaitTimeListOfflinePreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeList(
            waitTimes = previewSampleData.dropLast(1), // Weniger Daten simulieren
            lastUpdated = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
            isOffline = true,
            showErrorSnackbar = true, // Simulieren, dass Update fehlgeschlagen ist
            favoriteCodes = previewSampleFavorites,
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Control - Filter An")
@Composable
fun WaitTimeControlWithFilterPreview() {
    PhantasialandWaitTimesTheme {
        // Filtere die Beispieldaten für die Vorschau
        val filteredData = previewSampleData.filter { it.status.lowercase() == "opened" }
        WaitTimeControl(
            uiState = WaitTimeUiState(
                waitTimes = filteredData, // Zeige nur geöffnete
                lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3),
                filterOnlyOpen = true, // Filter ist an
                favoriteCodes = previewSampleFavorites,
                isOfflineData = false,
                isLoading = false,
                error = null
            ),
            onRefresh = {},
            onFavoriteToggle = {},
            onFilterOnlyOpenChanged = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Control - Filter Aus")
@Composable
fun WaitTimeControlWithoutFilterPreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeControl(
            uiState = WaitTimeUiState(
                waitTimes = previewSampleData, // Alle anzeigen
                lastUpdated = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3),
                filterOnlyOpen = false, // Filter ist aus
                favoriteCodes = previewSampleFavorites,
                isOfflineData = false,
                isLoading = false,
                error = null
            ),
            onRefresh = {},
            onFavoriteToggle = {},
            onFilterOnlyOpenChanged = {}
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Control - Loading")
@Composable
fun WaitTimeControlLoadingPreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeControl(
            uiState = WaitTimeUiState(isLoading = true),
            onRefresh = {},
            onFavoriteToggle = {},
            onFilterOnlyOpenChanged = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Control - Error")
@Composable
fun WaitTimeControlErrorPreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeControl(
            uiState = WaitTimeUiState(error = "Netzwerkfehler.", waitTimes = emptyList()),
            onRefresh = {},
            onFavoriteToggle = {},
            onFilterOnlyOpenChanged = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "Control - Empty")
@Composable
fun WaitTimeControlEmptyPreview() {
    PhantasialandWaitTimesTheme {
        WaitTimeControl(
            uiState = WaitTimeUiState(isLoading = false, waitTimes = emptyList(), error = null),
            onRefresh = {},
            onFavoriteToggle = {},
            onFilterOnlyOpenChanged = {}
        )
    }
}

// Dummy-Datenklassen und ViewModel für den Fall, dass die echten nicht verfügbar sind
// oder Hilt in Previews Probleme macht. Kommentiere sie aus, wenn du Hilt verwendest und es funktioniert.
/*
data class AttractionWaitTime(val code: String, val name: String, val waitTimeMinutes: Int, val status: String)
data class WaitTimeUiState(
    val isLoading: Boolean = false,
    val waitTimes: List<AttractionWaitTime> = emptyList(),
    val error: String? = null,
    val lastUpdated: Long = 0L,
    val isOfflineData: Boolean = false,
    val currentSortType: SortType = SortType.NAME,
    val currentSortDirection: SortDirection = SortDirection.ASCENDING,
    val favoriteCodes: Set<String> = emptySet(),
    val filterOnlyOpen: Boolean = false
)
enum class SortType { NAME, WAIT_TIME }
enum class SortDirection { ASCENDING, DESCENDING }

// Erstelle eine Dummy-ViewModel-Klasse, wenn kein Hilt verwendet wird oder für Previews
class DummyViewModel {
     val uiState = MutableStateFlow(WaitTimeUiState(waitTimes = previewSampleData, favoriteCodes = previewSampleFavorites))
     fun fetchWaitTimes(isRefresh: Boolean = false) {}
     fun changeSortOrder(newType: SortType, newDirection: SortDirection) {}
     fun toggleFavorite(code: String) {}
     fun setFilterOnlyOpen(enabled: Boolean) {}
}
// Passe die Preview-Annotation an, falls du DummyViewModel brauchst:
// @Composable fun WaitTimeApp(viewModel: DummyViewModel = DummyViewModel()) { ... }
*/