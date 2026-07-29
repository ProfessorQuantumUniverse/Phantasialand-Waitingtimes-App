package com.quantum_prof.phantalandwaittimes.ui.theme.main

/** What the attraction list is ordered by (favourites always float to the top regardless). */
enum class SortType {
    NAME,
    WAIT_TIME;

    companion object {
        fun fromName(value: String?): SortType =
            entries.firstOrNull { it.name == value } ?: NAME
    }
}

enum class SortDirection {
    /** A–Z, shortest wait first. */
    ASCENDING,

    /** Z–A, longest wait first. */
    DESCENDING;

    val opposite: SortDirection
        get() = if (this == ASCENDING) DESCENDING else ASCENDING

    companion object {
        fun fromName(value: String?): SortDirection =
            entries.firstOrNull { it.name == value } ?: ASCENDING
    }
}
