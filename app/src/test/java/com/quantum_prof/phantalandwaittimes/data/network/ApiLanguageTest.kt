package com.quantum_prof.phantalandwaittimes.data.network

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ApiLanguageTest {

    @Test
    fun `German-speaking locales request German data`() {
        listOf(Locale.GERMANY, Locale.GERMAN, Locale.forLanguageTag("de-AT"), Locale.forLanguageTag("de-CH"))
            .forEach { locale ->
                assertEquals(
                    "Expected German for $locale",
                    ApiLanguage.GERMAN,
                    ApiLanguage.forLocale(locale)
                )
            }
    }

    /**
     * The API rejects anything other than `de` and `en`, so every locale the app is translated
     * into but the API does not support has to fall back to English rather than send its own tag.
     */
    @Test
    fun `every other supported app locale falls back to English`() {
        listOf("nl-NL", "fr-FR", "fr-BE", "pl-PL", "cs-CZ", "da-DK", "it-IT", "en-GB")
            .map(Locale::forLanguageTag)
            .forEach { locale ->
                assertEquals(
                    "Expected English for $locale",
                    ApiLanguage.ENGLISH,
                    ApiLanguage.forLocale(locale)
                )
            }
    }

    @Test
    fun `header values match what the API accepts`() {
        assertEquals("de", ApiLanguage.GERMAN.header)
        assertEquals("en", ApiLanguage.ENGLISH.header)
    }
}
