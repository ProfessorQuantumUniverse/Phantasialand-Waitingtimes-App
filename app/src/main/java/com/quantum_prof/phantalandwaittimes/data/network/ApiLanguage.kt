package com.quantum_prof.phantalandwaittimes.data.network

import java.util.Locale

/**
 * The languages the wait-time API accepts.
 *
 * The endpoint only understands `de` and `en`; any other value comes back as
 * `{"error":"Invalid parameters"}`, which does not deserialise into a list and would surface as a
 * load failure. The app therefore maps whatever locale the user runs onto one of these two, while
 * the rest of the interface is translated locally.
 */
enum class ApiLanguage(val header: String) {
    GERMAN("de"),
    ENGLISH("en");

    companion object {
        /** German-speaking users get German data; everyone else gets English. */
        fun forLocale(locale: Locale): ApiLanguage =
            if (locale.language == Locale.GERMAN.language) GERMAN else ENGLISH

        fun forCurrentLocale(): ApiLanguage = forLocale(Locale.getDefault())
    }
}
