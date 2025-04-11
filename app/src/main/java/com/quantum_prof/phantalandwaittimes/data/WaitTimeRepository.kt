package com.quantum_prof.phantalandwaittimes.data

import android.content.SharedPreferences // Import für SharedPreferences
import android.util.Log
import com.quantum_prof.phantalandwaittimes.data.network.ApiService
// Importiere die Keys aus dem StorageModule
import com.quantum_prof.phantalandwaittimes.di.StorageModule.KEY_LAST_FETCH_TIMESTAMP
import com.quantum_prof.phantalandwaittimes.di.StorageModule.KEY_WAIT_TIMES_JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString // Import für Serialization
import kotlinx.serialization.json.Json     // Import für Json
import javax.inject.Inject
import kotlin.Result // Expliziter Import um Kollisionen zu vermeiden

// Typalias für Klarheit (Daten + Boolean, ob aus Cache)
typealias WaitTimeResult = Pair<List<AttractionWaitTime>, Boolean>

// --- GEÄNDERT: SharedPreferences und Json im Konstruktor injecten ---
class WaitTimeRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences, // <- Hinzugefügt
    private val json: Json                            // <- Hinzugefügt (kommt von Hilt aus NetworkModule)
) {

    // --- GEÄNDERT: Rückgabetyp ist Result<WaitTimeResult> ---
    suspend fun getPhantasialandWaitTimes(): Result<WaitTimeResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Versuche, Daten von der API zu holen
                Log.d("Repository", "--> Versuche API-Aufruf...")
                val freshWaitTimes = apiService.getWaitTimes()
                Log.d("Repository", "<-- API-Aufruf erfolgreich (${freshWaitTimes.size} Elemente).")

                // 2. Speichere erfolgreiche Daten im Cache
                saveToCache(freshWaitTimes) // Ruft die unten definierte Funktion auf

                // 3. Gib frische Daten zurück (isFromCache = false)
                // Die Sortierung hier ist eine Standard-Sortierung, die immer angewendet wird.
                // Das ViewModel kann sie dann ggf. ändern.
                Result.success(freshWaitTimes.sortedBy { it.name } to false)

            } catch (apiException: Exception) {
                // API-Aufruf fehlgeschlagen
                Log.w("Repository", "<-- API-Aufruf fehlgeschlagen!", apiException)

                // 4. Versuche, Daten aus dem Cache zu laden
                Log.d("Repository", "--> Versuche aus Cache zu laden...")
                val cachedData = loadFromCache() // Ruft die unten definierte Funktion auf

                if (cachedData != null) {
                    Log.d("Repository", "<-- Daten aus Cache geladen (${cachedData.first.size} Elemente).")
                    // 5. Gib Cache-Daten zurück (isFromCache = true)
                    // cachedData.first ist die Liste, cachedData.second der Timestamp (hier nicht direkt benötigt)
                    Result.success(cachedData.first to true)
                } else {
                    Log.e("Repository", "<-- Kein Cache verfügbar oder Fehler beim Laden. Gebe API-Fehler zurück.")
                    // 6. Kein Cache vorhanden oder Fehler beim Parsen, gib den ursprünglichen API-Fehler zurück
                    Result.failure(apiException)
                }
            }
        }
    }

    // --- HINZUGEFÜGT: Funktion zum Speichern im Cache ---
    private fun saveToCache(waitTimes: List<AttractionWaitTime>) {
        try {
            // Wandle die Liste in einen JSON-String um
            // 'json' kommt jetzt aus dem Konstruktor
            val jsonString = json.encodeToString(waitTimes)
            val timestamp = System.currentTimeMillis()

            // Speichere String und Timestamp in SharedPreferences
            // 'sharedPreferences' kommt jetzt aus dem Konstruktor
            sharedPreferences.edit()
                .putString(KEY_WAIT_TIMES_JSON, jsonString)
                .putLong(KEY_LAST_FETCH_TIMESTAMP, timestamp)
                .apply() // apply() ist asynchron und blockiert nicht

            Log.d("Repository", "Daten erfolgreich im Cache gespeichert.")
        } catch (e: Exception) {
            // Fange mögliche Fehler bei der Serialisierung oder beim Speichern ab
            Log.e("Repository", "Fehler beim Speichern im Cache", e)
        }
    }

    // --- HINZUGEFÜGT: Funktion zum Laden aus dem Cache ---
    // Gibt ein Pair aus der Liste und dem Speicherzeitpunkt zurück, oder null bei Fehler/keinen Daten
    private fun loadFromCache(): Pair<List<AttractionWaitTime>, Long>? {
        // Lese JSON-String und Timestamp aus SharedPreferences
        val jsonString = sharedPreferences.getString(KEY_WAIT_TIMES_JSON, null)
        val timestamp = sharedPreferences.getLong(KEY_LAST_FETCH_TIMESTAMP, 0L)

        // Prüfe, ob beides vorhanden ist
        return if (jsonString != null && timestamp > 0) {
            try {
                // Wandle den JSON-String zurück in eine Liste
                // 'json' kommt jetzt aus dem Konstruktor
                val waitTimes = json.decodeFromString<List<AttractionWaitTime>>(jsonString)

                // Gib die Liste (standardmäßig sortiert) und den Timestamp zurück
                waitTimes.sortedBy { it.name } to timestamp

            } catch (e: Exception) {
                // Fehler beim Parsen des JSON -> Cache ist ungültig oder beschädigt
                Log.e("Repository", "Fehler beim Laden/Deserialisieren aus Cache", e)
                // Lösche ungültigen Cache (optional aber empfohlen)
                sharedPreferences.edit()
                    .remove(KEY_WAIT_TIMES_JSON)
                    .remove(KEY_LAST_FETCH_TIMESTAMP)
                    .apply()
                null
            }
        } else {
            // Keine Daten im Cache gefunden
            Log.d("Repository", "Kein gültiger Cache gefunden (jsonString=$jsonString, timestamp=$timestamp)")
            null
        }
    }
}