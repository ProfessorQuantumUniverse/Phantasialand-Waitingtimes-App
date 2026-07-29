package com.quantum_prof.phantalandwaittimes.data.network

import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    /**
     * Returns the current wait times for [parkId].
     *
     * The API selects the park and the language via request headers rather than the path.
     * [language] must be one of [ApiLanguage]'s header values.
     */
    @GET("v1/waitingtimes")
    suspend fun getWaitTimes(
        @Header("park") parkId: String = DEFAULT_PARK,
        @Header("language") language: String = ApiLanguage.ENGLISH.header
    ): List<AttractionWaitTime>

    companion object {
        const val DEFAULT_PARK = "phantasialand"
    }
}
