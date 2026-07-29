package com.quantum_prof.phantalandwaittimes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Operational state of an attraction as reported by the API.
 *
 * The raw value is kept so unexpected states survive a cache round-trip, but every
 * consumer should branch on [AttractionStatus] instead of comparing strings.
 */
enum class AttractionStatus {
    OPENED,
    CLOSED,
    MAINTENANCE,
    VIRTUAL_QUEUE,
    UNKNOWN;

    /** Only [OPENED] and [VIRTUAL_QUEUE] have a meaningful wait time. */
    val isOperating: Boolean
        get() = this == OPENED || this == VIRTUAL_QUEUE

    companion object {
        fun fromRaw(raw: String): AttractionStatus =
            when (raw.trim().lowercase(Locale.ROOT)) {
                "opened", "open" -> OPENED
                "closed", "close" -> CLOSED
                "maintenance" -> MAINTENANCE
                "virtualqueue", "virtual_queue" -> VIRTUAL_QUEUE
                else -> UNKNOWN
            }
    }
}

/**
 * A single attraction with its current wait time.
 *
 * Every field has a default so that a malformed or partial API response degrades to a
 * usable object instead of throwing. Combined with `coerceInputValues = true` on the
 * [kotlinx.serialization.json.Json] instance, explicit `null`s also fall back to the defaults.
 */
@Serializable
data class AttractionWaitTime(
    @SerialName("code")
    val code: String = "",

    @SerialName("name")
    val name: String = "",

    @SerialName("waitingtime")
    val waitTimeMinutes: Int = 0,

    @SerialName("status")
    val status: String = ""
) {
    val attractionStatus: AttractionStatus
        get() = AttractionStatus.fromRaw(status)

    val isOpen: Boolean
        get() = attractionStatus.isOperating

    /**
     * Wait time clamped to a sane range. The API occasionally reports negative values for
     * attractions that are between states.
     */
    val displayWaitTime: Int
        get() = waitTimeMinutes.coerceIn(0, MAX_PLAUSIBLE_WAIT_MINUTES)

    /** True when the entry carries enough information to be shown at all. */
    val isUsable: Boolean
        get() = code.isNotBlank() && name.isNotBlank()

    companion object {
        /** Anything beyond this is treated as a data glitch rather than a real queue. */
        const val MAX_PLAUSIBLE_WAIT_MINUTES = 600
    }
}
