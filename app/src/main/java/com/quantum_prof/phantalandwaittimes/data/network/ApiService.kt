package com.quantum_prof.phantalandwaittimes.data.network


import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    @GET("v1/waitingtimes")
    suspend fun getWaitTimes(
        @Header("park") parkId: String = "phantasialand",
        @Header("language") language: String = "de"
    ): List<AttractionWaitTime>
}