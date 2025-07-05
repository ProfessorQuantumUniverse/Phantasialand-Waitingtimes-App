package com.quantum_prof.phantalandwaittimes.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class WaitTimeAlert(
    val attractionCode: String,
    val attractionName: String,
    val targetTime: Int // Benachrichtigen, wenn Wartezeit UNTER diesen Wert fällt
)

