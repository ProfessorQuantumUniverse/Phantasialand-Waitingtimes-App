# 🎢 Phantasialand Wartezeiten App

Eine einfache Android-App zur Anzeige der aktuellen Wartezeiten für Attraktionen im Phantasialand Themenpark. Sie hilft Besuchern, ihren Tag im Park besser zu planen, indem sie Live-Daten abruft (sofern verfügbar) und verschiedene Ansichts- und Organisationsoptionen bietet.

*(Screenshots folgen)*
[Hier könnten Screenshots der App eingefügt werden, z.B. von der Hauptliste, dem Filter, den Favoriten.]

## ✨ Features

*   **Aktuelle Wartezeiten:** Anzeige der von der Datenquelle gemeldeten Wartezeiten.
*   **Attraktionsstatus:** Zeigt an, ob eine Attraktion geöffnet, geschlossen oder in Wartung ist.
*   **Offline-Caching:** Speichert die zuletzt erfolgreich abgerufenen Daten, um auch ohne Internetverbindung die letzten bekannten Wartezeiten anzuzeigen.
*   **Pull-to-Refresh:** Manuelles Aktualisieren der Daten durch Herunterziehen der Liste.
*   **Sortierung:** Möglichkeit, die Attraktionen nach Name (A-Z, Z-A) oder nach Wartezeit (kürzeste/längste zuerst) zu sortieren.
*   **Filterung:** Option, nur die aktuell geöffneten Attraktionen anzuzeigen.
*   **Favoriten:** Markieren von Lieblingsattraktionen mit einem Stern. Die Favoriten werden gespeichert und bleiben über App-Neustarts hinweg erhalten.
*   **Attraktions-Icons:** Visuelle Kennzeichnung des Attraktionstyps (z.B. Achterbahn, Wasserbahn, Show) durch passende Icons.
*   **Thematisches Design:** Ein Hintergrundbild sorgt für Park-Atmosphäre.
*   **Letzte Aktualisierung:** Anzeige, wann die Daten zuletzt erfolgreich abgerufen wurden.
*   **Fehlerbehandlung:** Informiert den Benutzer bei Problemen mit dem Datenabruf.

## 🛠️ Technologie-Stack

*   **Sprache:** Kotlin
*   **UI Toolkit:** Jetpack Compose
*   **Architektur:** MVVM (Model-View-ViewModel)
*   **Asynchronität:** Kotlin Coroutines & StateFlow
*   **Dependency Injection:** Hilt
*   **Netzwerk:** (Wahrscheinlich Retrofit oder Ktor - bitte ergänzen, was du verwendest)
*   **Datenspeicherung (Cache/Favoriten):** SharedPreferences
*   **UI-Komponenten:**
    *   Material 3 Design Components
    *   Accompanist SwipeRefresh für Pull-to-Refresh

## 🚀 Zukünftige Ideen / Mögliche Erweiterungen

*   Integration einer Parkkarte mit Anzeige der Wartezeiten direkt an den Attraktionsstandorten.
*   Push-Benachrichtigungen für Favoriten, wenn deren Wartezeit unter einen bestimmten Wert fällt.
*   Anzeige von Show-Zeiten und anderen Parkinformationen.
*   Detailliertere Informationen zu Attraktionen (z.B. Mindestgröße, Typ, Beschreibung).
*   Umstellung des Caches auf eine robustere Lösung wie Room Database.
*   Widgets für den Android Homescreen.
*   Verbesserte Barrierefreiheit.

## 🙏 Danksagung

*   Ein Dank geht an **[Quelle der Wartezeit-Daten einfügen, z.B. "die inoffizielle Phantasialand API" oder der spezifische Anbieter]** für die Bereitstellung der Daten.
*   Danke an die Entwickler der verwendeten Open-Source-Bibliotheken (Coil, Hilt, Accompanist etc.).
*   Icons basieren ggf. auf Material Design Icons / [Andere Icon-Quelle nennen, falls zutreffend].
