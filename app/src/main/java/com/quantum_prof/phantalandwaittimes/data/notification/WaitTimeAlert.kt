package com.quantum_prof.phantalandwaittimes.data.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A reminder to notify the user once [attractionCode]'s wait drops to [targetMinutes] or below.
 *
 * Every field has a default so older persisted payloads keep deserialising after schema changes.
 */
@Serializable
data class WaitTimeAlert(
    val attractionCode: String = "",
    val attractionName: String = "",
    // Serialised name kept as `targetTime` so alerts saved by older app versions still load.
    @SerialName("targetTime")
    val targetMinutes: Int = DEFAULT_TARGET_MINUTES,
    /** Wall-clock time the alert was created; used to keep the list order stable. */
    val createdAt: Long = 0L
) {
    val isValid: Boolean
        get() = attractionCode.isNotBlank() && targetMinutes in MIN_TARGET_MINUTES..MAX_TARGET_MINUTES

    companion object {
        const val DEFAULT_TARGET_MINUTES = 30
        const val MIN_TARGET_MINUTES = 0
        const val MAX_TARGET_MINUTES = 600
    }
}
