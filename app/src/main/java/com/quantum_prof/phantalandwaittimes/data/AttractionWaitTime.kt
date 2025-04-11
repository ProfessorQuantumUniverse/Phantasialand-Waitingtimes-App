package com.quantum_prof.phantalandwaittimes.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttractionWaitTime(
    @SerialName("code")
    val code: String,

    @SerialName("name")
    val name: String,

    @SerialName("waitingtime")
    val waitTimeMinutes: Int,

    @SerialName("status")
    val status: String // z.B. "opened", "closed"
)