package com.quantum_prof.phantalandwaittimes.ui.theme.main

import com.quantum_prof.phantalandwaittimes.data.AttractionWaitTime

/**
 * Ordering rules for the attraction list, kept out of the ViewModel so they can be unit tested.
 *
 * Rules, in order:
 *  1. Favourites always come first, in both sort directions. Reversing the order should not bury
 *     the entries the user pinned.
 *  2. Within each group the chosen criterion applies, and only that part respects [direction].
 *  3. For a wait-time sort, attractions that are not operating are treated as having the longest
 *     possible wait — their reported number is meaningless — and are broken out by name.
 */
fun sortAttractions(
    attractions: List<AttractionWaitTime>,
    sortType: SortType,
    direction: SortDirection,
    favoriteCodes: Set<String>
): List<AttractionWaitTime> {
    val byFavorite = compareByDescending<AttractionWaitTime> { it.code in favoriteCodes }

    val byCriterion: Comparator<AttractionWaitTime> = when (sortType) {
        SortType.NAME ->
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }

        SortType.WAIT_TIME ->
            compareBy<AttractionWaitTime> { if (it.isOpen) it.displayWaitTime else Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }

    val directed = if (direction == SortDirection.ASCENDING) byCriterion else byCriterion.reversed()
    return attractions.sortedWith(byFavorite.then(directed))
}

/** Applies the "only open attractions" filter. */
fun filterAttractions(
    attractions: List<AttractionWaitTime>,
    onlyOpen: Boolean
): List<AttractionWaitTime> = if (onlyOpen) attractions.filter { it.isOpen } else attractions
